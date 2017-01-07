package edu.hsb.wifivisualizer.map;

import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.common.api.Status;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import butterknife.BindView;
import butterknife.ButterKnife;
import edu.hsb.wifivisualizer.DatabaseTaskController;
import edu.hsb.wifivisualizer.R;
import edu.hsb.wifivisualizer.WifiVisualizerApp;
import edu.hsb.wifivisualizer.calculation.IDelaunayService;
import edu.hsb.wifivisualizer.calculation.IIsoService;
import edu.hsb.wifivisualizer.calculation.impl.IncrementalDelaunayService;
import edu.hsb.wifivisualizer.calculation.impl.SimpleIsoService;
import edu.hsb.wifivisualizer.database.DaoSession;
import edu.hsb.wifivisualizer.model.Isoline;
import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.Triangle;
import edu.hsb.wifivisualizer.model.WifiInfo;

public class MapFragment extends Fragment implements ILocationListener {

    public static final String TAG = MapFragment.class.getSimpleName();
    public static final int PERMISSIONS_ACCESS_FINE_LOCATION = 10;

    @BindView(R.id.map_wrapper)
    View wrapper;

    private IMapService mapService;
    private GoogleLocationProvider googleLocationProvider;

    private DatabaseTaskController dbController;
    private IDelaunayService delaunayService;
    private IIsoService isoService;

    private Snackbar locationSnackbar;
    private Snackbar requiredSnackbar;

    private Set<String> ssidSet = Sets.newLinkedHashSet();
    private int selectedSSIDPosition = 0;

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ButterKnife.bind(this, view);
        mapService = new GoogleMapService(this);
        googleLocationProvider = new GoogleLocationProvider(this.getContext());

        final DaoSession daoSession = ((WifiVisualizerApp) getActivity().getApplication()).getDaoSession();
        dbController = new DatabaseTaskController(daoSession);
        delaunayService = new IncrementalDelaunayService();
        isoService = new SimpleIsoService();
        buildSnackbars();
        mapService.initMap(wrapper);
    }

    @Override
    public void onStart() {
        googleLocationProvider.startListening(this);
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        googleLocationProvider.stopListening();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_filter) {
            final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
            final View dialogRootView = getLayoutInflater(null).inflate(R.layout.filter_window, null);
            final ListView listView = (ListView) dialogRootView.findViewById(R.id.list);
            final AlertDialog dialog = dialogBuilder.setView(dialogRootView).create();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_list_item_single_choice,
                    android.R.id.text1,
                    Lists.newArrayList(ssidSet));
            listView.setAdapter(adapter);
            listView.setItemChecked(selectedSSIDPosition,  Boolean.TRUE);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    selectedSSIDPosition = position;
                    mapService.recalculate();
                    dialog.dismiss();
                }
            });
            dialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            locationSnackbar.dismiss();
            mapService.centerOnLocation(location);
        }
    }

    @Override
    public void onResolutionNeeded(Status status) {
        try {
            status.startResolutionForResult(this.getActivity(),
                    MapFragment.PERMISSIONS_ACCESS_FINE_LOCATION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Unexpected error while resolving location intent", e);
        }
    }

    @Override
    public void onProviderUnavailable() {
        showLocationRequiredSnackbar();
    }

    @Override
    public void onLostConnection() {
        locationSnackbar.show();
    }

    private void showLocationRequiredSnackbar() {
        requiredSnackbar.show();
    }

    private void buildSnackbars() {
        final int color = ContextCompat.getColor(this.getActivity(), R.color.colorPrimary);
        locationSnackbar = Snackbar.make(wrapper, "Searching for location", Snackbar.LENGTH_INDEFINITE);
        locationSnackbar.getView().setBackgroundColor(color);
        locationSnackbar.show();

        requiredSnackbar = Snackbar.make(wrapper, "Permission required", Snackbar.LENGTH_INDEFINITE);
        requiredSnackbar.getView().setBackgroundColor(color);
    }

    public void calculateTriangulation() {
        dbController.getPointList().onSuccessTask(new Continuation<List<Point>, Task<List<Triangle>>>() {
            @Override
            public Task<List<Triangle>> then(final Task<List<Point>> task) throws Exception {
                return Task.callInBackground(new Callable<List<Triangle>>() {
                    @Override
                    public List<Triangle> call() throws Exception {
                        final List<Point> result = task.getResult();
                        extractSsids(result);
                        return delaunayService.calculate(result);
                    }
                });
            }
        }).onSuccess(new Continuation<List<Triangle>, List<Triangle>>() {
            @Override
            public List<Triangle> then(final Task<List<Triangle>> task) throws Exception {
                final List<Triangle> result = task.getResult();
                for (Triangle triangle : result) {
                    mapService.drawTriangle(triangle);
                }
                return result;
            }
        }, Task.UI_THREAD_EXECUTOR).onSuccess(new Continuation<List<Triangle>, List<Isoline>>() {
            @Override
            public List<Isoline> then(Task<List<Triangle>> task) throws Exception {
                String ssid = null;
                if (selectedSSIDPosition != 0) {
                    ssid = Iterables.get(ssidSet, selectedSSIDPosition, null);
                }
                return isoService.extractIsolines(task.getResult(), Lists.newArrayList(-50), ssid);
            }
        }, Task.BACKGROUND_EXECUTOR).onSuccess(new Continuation<List<Isoline>, Void>() {
            @Override
            public Void then(Task<List<Isoline>> task) throws Exception {
                for (Isoline isoline : task.getResult()) {
                    mapService.drawIsoline(isoline);
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    private void extractSsids(List<Point> pointList) {
        ssidSet.clear();
        ssidSet.add("All");
        for (Point point : pointList) {
            for (WifiInfo info : point.getSignalStrength()) {
                ssidSet.add(info.getSsid());
            }
        }
    }
}