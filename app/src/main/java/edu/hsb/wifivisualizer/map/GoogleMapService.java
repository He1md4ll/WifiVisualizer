package edu.hsb.wifivisualizer.map;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

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
import edu.hsb.wifivisualizer.model.WifiInfo;

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
            map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    final Point point = markerMap.get(marker);
                    final List<WifiInfo> wifiInfoList = point.getSignalStrength();
                    final View v = fragment.getLayoutInflater(null).inflate(R.layout.marker_info_window, null);
                    final TextView caption = (TextView) v.findViewById(R.id.marker_caption);
                    final HorizontalBarChart barChart = (HorizontalBarChart) v.findViewById(R.id.marker_chart);

                    caption.setText("Found information for " + wifiInfoList.size() + " wifi networks");
                    List<BarEntry> barEntryList = Lists.newArrayList();
                    for (WifiInfo wifiInfo : wifiInfoList) {
                            barEntryList.add(new BarEntry(barEntryList.size() ,wifiInfo.getStrength() * -1, wifiInfo.getSsid()));
                    }
                    final BarDataSet barDataSet = new BarDataSet(barEntryList, "Wifi data");
                    barDataSet.setDrawValues(true);
                    barDataSet.setColor(ContextCompat.getColor(fragment.getContext(), R.color.colorPrimary));
                    barDataSet.setValueTextColor(ContextCompat.getColor(fragment.getContext(), R.color.colorAccent));
                    final BarData barData = new BarData(barDataSet);
                    barData.setDrawValues(true);
                    barChart.setData(barData);
                    barChart.getDescription().setEnabled(false);
                    barChart.getXAxis().setEnabled(false);
                    barChart.setMaxVisibleValueCount(wifiInfoList.size());

                    //Disable all interaction with the chart
                    barChart.setHighlightPerTapEnabled(false);
                    barChart.setHighlightPerDragEnabled(false);
                    barChart.setDoubleTapToZoomEnabled(false);
                    barChart.invalidate();
                    return v;
                }
            });
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
                final Point point = new Point(null, latLng);
                // Save Point in db
                dbController.savePoint(point).onSuccessTask(new Continuation<Point, Task<List<WifiInfo>>>() {
                    @Override
                    public Task<List<WifiInfo>> then(Task<Point> task) throws Exception {
                        final Point result = task.getResult();
                        return dbController.saveWifiInfoList(randomWifiInfoList(result.getId()));
                    }
                }).onSuccess(new Continuation<List<WifiInfo>, Void>() {
                    @Override
                    public Void then(Task<List<WifiInfo>> task) throws Exception {
                        recalculate();
                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR);
            }
        });
    }

    private List<WifiInfo> randomWifiInfoList(final Long pointId) {
        List<WifiInfo> result = Lists.newArrayList();
        result.add(new WifiInfo(null, pointId, UUID.randomUUID().toString(), new Random().nextInt(MAX_SIGNAL_STRENGTH)));
        result.add(new WifiInfo(null, pointId, UUID.randomUUID().toString(), new Random().nextInt(MAX_SIGNAL_STRENGTH)));
        result.add(new WifiInfo(null, pointId, UUID.randomUUID().toString(), new Random().nextInt(MAX_SIGNAL_STRENGTH)));
        return result;
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
            polylineOptions.add(triangle.getDefiningPointList().get(0).getPosition());
            map.addPolyline(polylineOptions);
        }
    }
}