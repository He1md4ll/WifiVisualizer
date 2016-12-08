package edu.hsb.wifivisualizer.map;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import bolts.Continuation;
import bolts.Task;
import edu.hsb.wifivisualizer.DatabaseTaskController;
import edu.hsb.wifivisualizer.R;
import edu.hsb.wifivisualizer.WifiVisualizerApp;
import edu.hsb.wifivisualizer.calculation.IDelaunayService;
import edu.hsb.wifivisualizer.calculation.impl.SimpleDelauneyService;
import edu.hsb.wifivisualizer.database.DaoSession;
import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.Triangle;

public class GoogleMapService implements IMapService, OnMapReadyCallback {

    private static final int MAX_SIGNAL_STRENGTH = 50;
    private static final Float INITAL_ZOOM_LEVEL = 15.0f;
    private GoogleMap map;
    private Fragment fragment;
    private DatabaseTaskController dbController;
    private Map<Marker, Point> markerMap;
    private boolean zoomIn = Boolean.TRUE;

    private IDelaunayService delaunayService;

    public GoogleMapService(Fragment fragment) {
        this.fragment = fragment;
        final DaoSession daoSession = ((WifiVisualizerApp) fragment.getActivity().getApplication()).getDaoSession();
        dbController = new DatabaseTaskController(daoSession);
        delaunayService = new SimpleDelauneyService();
    }

    @Override
    public void initMap(View wrapper) {
        final SupportMapFragment supportMapFragment = SupportMapFragment.newInstance();
        supportMapFragment.getMapAsync(this);
        fragment.getFragmentManager().beginTransaction().replace(wrapper.getId(),supportMapFragment).commitAllowingStateLoss();
        markerMap = Collections.synchronizedMap(Maps.<Marker, Point>newHashMap());
    }


    @Override
    public void onMapReady(GoogleMap map) {
        try {
            this.map = map;
            map.getUiSettings().setMyLocationButtonEnabled(false);
            map.getUiSettings().setZoomControlsEnabled(true);
            map.getUiSettings().setScrollGesturesEnabled(true);
            map.getUiSettings().setRotateGesturesEnabled(true);
            map.getUiSettings().setZoomGesturesEnabled(true);
            map.setMyLocationEnabled(true);
            MapsInitializer.initialize(fragment.getContext());
            setClickListener();
            setDragListener();
            recalculate();
        } catch (SecurityException e) {
            Log.e(this.getClass().getSimpleName(), "Could not acquire phone location.");
        }
    }

    @Override
    public void centerOnLocation(@NonNull Location location) {
        if (map != null) {
            final LatLng myPosition = new LatLng(location.getLatitude(), location.getLongitude());
            if (zoomIn) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(myPosition, INITAL_ZOOM_LEVEL));
                if (INITAL_ZOOM_LEVEL.equals(map.getCameraPosition().zoom)) {
                    zoomIn = Boolean.FALSE;
                }
            } else {
                map.animateCamera(CameraUpdateFactory.newLatLng(myPosition));
            }
        }
    }

    private MarkerOptions createMarkerOptions(final Point point) {
        final MarkerOptions marker = new MarkerOptions();
        marker.draggable(Boolean.TRUE);
        marker.title("Messpunkt");
        marker.snippet("Signalstärke: " + point.getSignalStrength());
        marker.position(point.getPosition());
        return marker;
    }

    private void setClickListener() {
        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                final int randomStrength = new Random().nextInt(MAX_SIGNAL_STRENGTH) + 1;
                final Point point = new Point(null, latLng, randomStrength);

                // Save Point in db
                dbController.savePoint(point).onSuccess(new Continuation<Point, Void>() {
                    @Override
                    public Void then(Task<Point> task) throws Exception {
                        recalculate();
                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR);
            }
        });
    }

    private void setDragListener() {
        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {}
            @Override
            public void onMarkerDrag(Marker marker) {}
            @Override
            public void onMarkerDragEnd(Marker marker) {
                final Point point = markerMap.get(marker);
                if (point != null) {
                    dbController.removePoint(point).onSuccess(new Continuation<Void, Void>() {
                        @Override
                        public Void then(Task<Void> task) throws Exception {
                            recalculate();
                            return null;
                        }
                    }, Task.UI_THREAD_EXECUTOR);
                }
            }
        });
    }

    private void recalculate() {
        map.clear();
        calculateTriangulation();
        loadPoints();
    }

    private void loadPoints() {
        dbController.getPointList().onSuccess(new Continuation<List<Point>, Void>() {
            @Override
            public Void then(Task<List<Point>> task) throws Exception {
                for (Point point : task.getResult()) {
                    final Marker marker = map.addMarker(createMarkerOptions(point));
                    markerMap.put(marker, point);
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    private void calculateTriangulation() {
        dbController.getPointList().onSuccess(new Continuation<List<Point>, Void>() {
            @Override
            public Void then(Task<List<Point>> task) throws Exception {
                final List<Triangle> calculate = delaunayService.calculate(task.getResult());
                for (Triangle triangle : calculate) {
                    drawTriangle(triangle);
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    private void drawTriangle(@NonNull Triangle triangle) {
        if (map != null) {
            final int color = ContextCompat.getColor(fragment.getContext(), R.color.colorAccent);
            final PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.visible(Boolean.TRUE);
            polylineOptions.color(color);
            for (Point point : triangle.getDefiningPointList()) {
                polylineOptions.add(point.getPosition());
            }
            map.addPolyline(polylineOptions);
        }
    }
}