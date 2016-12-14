package edu.hsb.wifivisualizer.map;

import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.api.Status;

import butterknife.BindView;
import butterknife.ButterKnife;
import edu.hsb.wifivisualizer.R;
import edu.hsb.wifivisualizer.calculation.IIsoService;
import edu.hsb.wifivisualizer.calculation.impl.SimpleIsoService;

public class MapFragment extends Fragment implements ILocationListener {

    public static final String TAG = MapFragment.class.getSimpleName();
    public static final int PERMISSIONS_ACCESS_FINE_LOCATION = 10;

    @BindView(R.id.map_wrapper)
    View wrapper;

    private IMapService mapService;
    private GoogleLocationProvider googleLocationProvider;
    private IIsoService isoService;

    private Location currentLocation;
    private Snackbar locationSnackbar;
    private Snackbar requiredSnackbar;

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ButterKnife.bind(this, view);
        mapService = new GoogleMapService(this);
        googleLocationProvider = new GoogleLocationProvider(this.getContext());
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
    public void onLocationChanged(Location location) {
        if (location != null) {
            locationSnackbar.dismiss();
            currentLocation = location;
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
}