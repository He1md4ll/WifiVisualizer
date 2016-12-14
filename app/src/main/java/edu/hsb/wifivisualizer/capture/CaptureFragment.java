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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import bolts.Continuation;
import bolts.Task;
import butterknife.BindView;
import butterknife.ButterKnife;
import edu.hsb.wifivisualizer.DatabaseTaskController;
import edu.hsb.wifivisualizer.R;
import edu.hsb.wifivisualizer.WifiVisualizerApp;
import edu.hsb.wifivisualizer.map.GoogleLocationProvider;
import edu.hsb.wifivisualizer.map.ILocationListener;
import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.WifiInfo;

public class CaptureFragment extends Fragment implements ILocationListener, IWifiListener {

    @BindView(R.id.button_start)
    Button start;
    @BindView(R.id.button_stop)
    Button stop;
    @BindView(R.id.text_captured)
    TextView captured;

    private static final float MIN_DISTANCE = 5;
    private WifiService wifiService;
    private GoogleLocationProvider googleLocationProvider;
    private boolean serviceBound = Boolean.FALSE;
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
        ButterKnife.bind(this, view);
        googleLocationProvider = new GoogleLocationProvider(this.getContext());
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                WifiService.LocalBinder binder = (WifiService.LocalBinder) service;
                wifiService = binder.getService();
                wifiService.registerListener(CaptureFragment.this);
                serviceBound = Boolean.TRUE;
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                wifiService.unregisterListener(CaptureFragment.this);
                serviceBound = Boolean.FALSE;
            }
        };
        dbController = new DatabaseTaskController(((WifiVisualizerApp) getActivity().getApplication()).getDaoSession());
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleLocationProvider.startListening(CaptureFragment.this);
                Intent intent = new Intent(CaptureFragment.this.getContext(), WifiService.class);
                getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
                start.setEnabled(Boolean.FALSE);
                stop.setEnabled(Boolean.TRUE);
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
            }
        });
        captured.setText("Captured locations: " + locationMap.size());
    }

    @Override
    public void onDestroy() {
        stop();
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            if (currentLocation == null) {
                currentLocation = location;
            } else if (currentLocation.distanceTo(location) > MIN_DISTANCE) {
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

    @Override
    public void onWifiUpdate(List<ScanResult> resultList) {
        if (currentLocation != null) {
            final List<WifiInfo> newWifiInfoList = Lists.transform(resultList, new Function<ScanResult, WifiInfo>() {
                @Override
                public WifiInfo apply(ScanResult input) {
                    return WifiInfo.fromScanResult(input);
                }
            });
            final List<WifiInfo> currentWifiInfoList = locationMap.get(currentLocation);
            if (currentWifiInfoList == null) {
                locationMap.put(currentLocation, newWifiInfoList);
            } else {
                locationMap.put(currentLocation, updateWifiInfoList(currentWifiInfoList, newWifiInfoList));
            }

            new Handler(getContext().getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), "Updated location map", Toast.LENGTH_SHORT).show();
                    captured.setText("Captured locations: " + locationMap.size());
                }
            });
        }
    }

    private void stop() {
        googleLocationProvider.stopListening();
        if (serviceBound) {
            getActivity().unbindService(serviceConnection);
            wifiService.unregisterListener(CaptureFragment.this);
            serviceBound = Boolean.FALSE;
        }
        start.setEnabled(Boolean.TRUE);
        stop.setEnabled(Boolean.FALSE);
        for (final Map.Entry<Location, List<WifiInfo>> entry : locationMap.entrySet()) {
            final Location entryLocation = entry.getKey();
            final Point point = new Point(null, new LatLng(entryLocation.getLatitude(), entryLocation.getLongitude()));
            dbController.savePoint(point).onSuccessTask(new Continuation<Point, Task<List<WifiInfo>>>() {
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
                    Toast.makeText(CaptureFragment.this.getContext(), "Saved location map to local database", Toast.LENGTH_LONG).show();
                    locationMap.clear();
                    captured.setText("Captured locations: " + locationMap.size());
                    return null;
                }
            }, Task.UI_THREAD_EXECUTOR);
        }
    }

    private List<WifiInfo> updateWifiInfoList(List<WifiInfo> currentList, List<WifiInfo> newList) {
        List<WifiInfo> result = Lists.newArrayList(currentList);
        for(final WifiInfo wifiInfo : newList) {
            final Optional<WifiInfo> wifiInfoOptional = Iterables.tryFind(result, new Predicate<WifiInfo>() {
                @Override
                public boolean apply(WifiInfo input) {
                    return wifiInfo.getSsid().equals(input.getSsid());
                }
            });
            if (wifiInfoOptional.isPresent()) {
                wifiInfo.setStrength((wifiInfo.getStrength() + wifiInfoOptional.get().getStrength()) / 2);
            } else {
                result.add(wifiInfo);
            }
        }
        return result;
    }
}