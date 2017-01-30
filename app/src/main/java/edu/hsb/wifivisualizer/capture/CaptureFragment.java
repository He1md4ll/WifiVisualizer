package edu.hsb.wifivisualizer.capture;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import bolts.Continuation;
import bolts.Task;
import butterknife.BindView;
import butterknife.ButterKnife;
import edu.hsb.wifivisualizer.DatabaseTaskController;
import edu.hsb.wifivisualizer.PointUtils;
import edu.hsb.wifivisualizer.R;
import edu.hsb.wifivisualizer.WifiVisualizerApp;
import edu.hsb.wifivisualizer.map.GoogleLocationProvider;
import edu.hsb.wifivisualizer.map.ILocationListener;
import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.WifiInfo;

/**
 * View to display information regarding capturing wifi data
 * User can start and stop capture process
 */
public class CaptureFragment extends Fragment implements ILocationListener, IWifiListener {

    @BindView(R.id.seekbar_distance)
    SeekBar seekBar;
    @BindView(R.id.text_seek)
    TextView seekText;
    @BindView(R.id.button_start)
    Button start;
    @BindView(R.id.button_stop)
    Button stop;
    @BindView(R.id.text_captured)
    TextView captured;

    // Default distance value in meter
    private int minDistance = 5;
    private WifiService wifiService;
    private GoogleLocationProvider googleLocationProvider;
    private boolean serviceBound = Boolean.FALSE;
    private boolean started = Boolean.FALSE;
    private ServiceConnection serviceConnection;
    private Location currentLocation;
    private DatabaseTaskController dbController;
    private Map<Location, List<WifiInfo>> locationMap = Collections.synchronizedMap(Maps.<Location, List<WifiInfo>>newLinkedHashMap());

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_capture, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        // Inject bound views --> see declaration above
        ButterKnife.bind(this, view);
        // Instantiate location provider to monitor device location
        googleLocationProvider = new GoogleLocationProvider(this.getContext());
        // Create connection object to bind to WifiService
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                WifiService.LocalBinder binder = (WifiService.LocalBinder) service;
                wifiService = binder.getService();
                // Register callback for captured data
                wifiService.registerListener(CaptureFragment.this);
                serviceBound = Boolean.TRUE;
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                // Unregister callback
                wifiService.unregisterListener(CaptureFragment.this);
                serviceBound = Boolean.FALSE;
            }
        };
        // Instantiate database task controller to access local database over asynchronous tasks
        dbController = new DatabaseTaskController(((WifiVisualizerApp) getActivity().getApplication()).getDaoSession());
        // Register click behaviour for start button
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                started = Boolean.TRUE;
                // Start listening for location updates
                googleLocationProvider.startListening(CaptureFragment.this);
                // Start and bind WifiService over intent
                Intent intent = new Intent(CaptureFragment.this.getContext(), WifiService.class);
                getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
                start.setEnabled(Boolean.FALSE);
                stop.setEnabled(Boolean.TRUE);
            }
        });
        // Register click behaviour for stop button
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
            }
        });
        // Display information to user
        captured.setText("Captured locations: " + locationMap.size());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekText.setText(progress + "/50 meter");
                minDistance = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stop();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    public void onDestroy() {
        stop();
        super.onDestroy();
    }

    /**
     * Called on location change from location provider
     * Sets new location if min distance is exceeded
     * @param location current location
     */
    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            if (currentLocation == null) {
                currentLocation = location;
            } else if (currentLocation.distanceTo(location) > minDistance) {
                currentLocation = location;
            }
        }
    }

    @Override
    public void onResolutionNeeded(Status status) {
    }

    @Override
    public void onProviderUnavailable() {
    }

    @Override
    public void onLostConnection() {
    }

    /**
     * Called from bound WifiService if new wifi data is available
     * @param resultList captured wifi data from android
     */
    @Override
    public void onWifiUpdate(List<ScanResult> resultList) {
        if (currentLocation != null) {
            // Transform android wifi data to our data model --> Limit information to needed properties
            final List<WifiInfo> newWifiInfoList = Lists.transform(resultList, new Function<ScanResult, WifiInfo>() {
                @Override
                public WifiInfo apply(ScanResult input) {
                    return WifiInfo.fromScanResult(input);
                }
            });
            final List<WifiInfo> currentWifiInfoList = locationMap.get(currentLocation);

            if (currentWifiInfoList == null) {
                // If there is no stored information for current location --> add them to the map
                locationMap.put(currentLocation, distinctWifiInfoList(newWifiInfoList));
            } else {
                // If we already have information for current location --> update information in map
                locationMap.put(currentLocation, updateWifiInfoList(currentWifiInfoList, newWifiInfoList));
            }

            // Display information to user (Handler necessary to get access to android main thread)
            new Handler(getContext().getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), "Updated location map", Toast.LENGTH_SHORT).show();
                    captured.setText("Captured locations: " + locationMap.size());
                }
            });
        }
    }

    /**
     * Stop capturing wifi data
     */
    private void stop() {
        if (!started) return;
        // Stop listening for location updates
        googleLocationProvider.stopListening();
        // Unbind service
        if (serviceBound) {
            getActivity().unbindService(serviceConnection);
            wifiService.unregisterListener(CaptureFragment.this);
            serviceBound = Boolean.FALSE;
        }
        start.setEnabled(Boolean.TRUE);
        stop.setEnabled(Boolean.FALSE);

        // Save captured information in database
        List<Task<Void>> taskList = Lists.newArrayList();
        for (final Map.Entry<Location, List<WifiInfo>> entry : locationMap.entrySet()) {
            // Create Point location - wifi data combination
            final Location entryLocation = entry.getKey();
            final Point point = new Point(null, new LatLng(entryLocation.getLatitude(),
                    entryLocation.getLongitude()),
                    PointUtils.calculateAverageStrength(entry.getValue()));
            // Save Point in local database as task (managed by database task controller)
            taskList.add(dbController.savePoint(point).onSuccessTask(new Continuation<Point, Task<List<WifiInfo>>>() {
                @Override
                public Task<List<WifiInfo>> then(Task<Point> task) throws Exception {
                    final Point result = task.getResult();
                    final List<WifiInfo> entryWifiInfoList = Lists.newArrayList(entry.getValue());
                    for (int i = 0; i < entryWifiInfoList.size(); i++) {
                        WifiInfo entryWifiInfo = entryWifiInfoList.get(i);
                        entryWifiInfo.setPointId(result.getId());
                    }
                    Log.d(this.getClass().getSimpleName(), "Saved point " + point.getId());
                    return dbController.saveWifiInfoList(entryWifiInfoList);
                }
            }).onSuccess(new Continuation<List<WifiInfo>, Void>() {
                @Override
                public Void then(Task<List<WifiInfo>> task) throws Exception {
                    return null;
                }
            }));
        }

        // Display information to user if all points successfully saved in local database
        Task.whenAll(taskList).onSuccess(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                Toast.makeText(CaptureFragment.this.getContext(), "Saved location map to local database", Toast.LENGTH_LONG).show();
                locationMap.clear();
                captured.setText("Captured locations: " + locationMap.size());
                started = Boolean.FALSE;
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    /**
     * Update current wifi data using new wifi data (merge)
     * @param currentList current wifi data
     * @param newList new wifi data
     * @return merged wifi data
     */
    private List<WifiInfo> updateWifiInfoList(List<WifiInfo> currentList, List<WifiInfo> newList) {
        List<WifiInfo> result = Lists.newArrayList(currentList);
        for(final WifiInfo wifiInfo : newList) {
            // Find Wifi SSID in current list
            final Optional<WifiInfo> wifiInfoOptional = Iterables.tryFind(result, new Predicate<WifiInfo>() {
                @Override
                public boolean apply(WifiInfo input) {
                    return wifiInfo.getSsid().equals(input.getSsid());
                }
            });
            // Set average wifi strength
            if (wifiInfoOptional.isPresent()) {
                wifiInfo.setStrength((wifiInfo.getStrength() + wifiInfoOptional.get().getStrength()) / 2);
            } else {
                result.add(wifiInfo);
            }
        }
        return result;
    }

    /**
     * Makes wifi SSID distinct (Combines duplicated SSIDs into one list entry)
     * @param infoList wifi data with (possibly) duplicated SSIDs
     * @return distinct wifi data
     */
    private List<WifiInfo> distinctWifiInfoList(List<WifiInfo> infoList) {
        final List<WifiInfo> result = Lists.newArrayList();
        // Index wifi data using SSID string
        ImmutableListMultimap<String, WifiInfo> indexedWifiMultimap = Multimaps.index(infoList, new Function<WifiInfo, String>() {
            @Override
            public String apply(WifiInfo input) {
                return input.getSsid();
            }
        });
        for (Map.Entry<String, Collection<WifiInfo>> entry: indexedWifiMultimap.asMap().entrySet()) {
            // Get collection if wifi data entries for a single SSID
            final Collection<WifiInfo> entryValue = entry.getValue();
            if (!entryValue.isEmpty()) {
                // Calculate average signal strength and create new WifiInfo object for distinct result
                WifiInfo infoEntry = new WifiInfo();
                int averageStrength = 0;
                for (WifiInfo info : entryValue) {
                    averageStrength += info.getStrength();
                }
                averageStrength /= entryValue.size();
                infoEntry.setSsid(entry.getKey());
                infoEntry.setStrength(averageStrength);
                result.add(infoEntry);
            }

        }
        return result;
    }
}