package edu.hsb.wifivisualizer.map;

import android.graphics.Color;
import android.location.Location;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.greenrobot.greendao.annotation.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import bolts.Continuation;
import bolts.Task;
import edu.hsb.wifivisualizer.DatabaseTaskController;
import edu.hsb.wifivisualizer.LatLngComperator;
import edu.hsb.wifivisualizer.PointUtils;
import edu.hsb.wifivisualizer.R;
import edu.hsb.wifivisualizer.WifiVisualizerApp;
import edu.hsb.wifivisualizer.database.DaoSession;
import edu.hsb.wifivisualizer.model.Isoline;
import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.Triangle;
import edu.hsb.wifivisualizer.model.WifiInfo;

public class GoogleMapService implements IMapService, OnMapReadyCallback {

    private static final int MAX_SIGNAL_STRENGTH = 100;
    private static final Float INITAL_ZOOM_LEVEL = 15.0f;
    private GoogleMap map;
    private MapFragment fragment;
    private DatabaseTaskController dbController;
    private Map<Marker, Point> markerMap;
    private boolean zoomIn = Boolean.TRUE;


    public GoogleMapService(MapFragment fragment) {
        this.fragment = fragment;
        final DaoSession daoSession = ((WifiVisualizerApp) fragment.getActivity().getApplication()).getDaoSession();
        dbController = new DatabaseTaskController(daoSession);
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
            setMarkerClickListener();
            recalculate();
        } catch (SecurityException e) {
            Log.e(this.getClass().getSimpleName(), "Could not acquire phone location.");
        }
    }

    private void setMarkerClickListener() {
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(fragment.getContext());
                final Point point = markerMap.get(marker);
                final List<WifiInfo> wifiInfoList = point.getSignalStrength();
                if (wifiInfoList.isEmpty()) return false;
                final View v = fragment.getLayoutInflater(null).inflate(R.layout.marker_info_window, null);


                final TextView caption = (TextView) v.findViewById(R.id.marker_caption);
                final RadarChart barChart = (RadarChart) v.findViewById(R.id.marker_chart);

                caption.setText("Found information for " + wifiInfoList.size() + " wifi networks");
                List<RadarEntry> barEntryList = Lists.newArrayList();
                for (WifiInfo wifiInfo : wifiInfoList) {
                    barEntryList.add(new RadarEntry(wifiInfo.getStrength(), wifiInfo.getSsid()));
                }
                final RadarDataSet barDataSet = new RadarDataSet(barEntryList, "Wifi data");
                barDataSet.setDrawValues(false);
                //barDataSet.setValueTextSize(10f);
                //barDataSet.setValueTextColor(ContextCompat.getColor(fragment.getContext(), R.color.colorAccent));
                barDataSet.setLineWidth(3f);
                barDataSet.setFillColor(ContextCompat.getColor(fragment.getContext(), R.color.colorPrimary));
                barDataSet.setDrawFilled(true);
                barDataSet.setColor(ContextCompat.getColor(fragment.getContext(), R.color.colorPrimaryDark));
                final RadarData barData = new RadarData(barDataSet);
                barChart.setData(barData);
                barChart.getDescription().setEnabled(false);
                final XAxis xAxis = barChart.getXAxis();
                xAxis.setTextSize(7f);
                xAxis.setValueFormatter(new IAxisValueFormatter() {

                    private List<String> ssidList = Lists.transform(wifiInfoList, new Function<WifiInfo, String>() {
                        @Override
                        public String apply(WifiInfo input) {
                            return input.getSsid();
                        }
                    });

                    @Override
                    public String getFormattedValue(float value, AxisBase axis) {
                        if (value < ssidList.size()) {
                            final String label = ssidList.get((int) value);
                            if (label.length() > 10) {
                                return label.substring(0, 10) + "...";
                            } else {
                                return label;
                            }
                        } else {
                            return "UNKNOWN";
                        }
                    }
                });

                //Disable all interaction with the chart
                barChart.setHighlightPerTapEnabled(false);
                barChart.animateXY(
                        1400, 1400,
                        Easing.EasingOption.EaseInOutQuad,
                        Easing.EasingOption.EaseInOutQuad);
                barChart.setWebLineWidth(1f);
                barChart.setWebColor(Color.LTGRAY);
                barChart.setWebLineWidthInner(1f);
                barChart.setWebColorInner(Color.LTGRAY);
                barChart.setWebAlpha(100);
                barChart.invalidate();

                dialogBuilder.setNeutralButton(android.R.string.ok, null);
                dialogBuilder.setView(v).create().show();
                return true;
            }
        });
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
                final List<WifiInfo> infoList = randomWifiInfoList();
                final Point point = new Point(null, latLng, PointUtils.calculateAverageStrength(infoList));
                // Save Point in db
                dbController.savePoint(point).onSuccessTask(new Continuation<Point, Task<List<WifiInfo>>>() {
                    @Override
                    public Task<List<WifiInfo>> then(Task<Point> task) throws Exception {
                        final Long pointId = task.getResult().getId();
                        for (WifiInfo info : infoList) {
                            info.setPointId(pointId);
                        }
                        return dbController.saveWifiInfoList(infoList);
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

    private List<WifiInfo> randomWifiInfoList() {
        List<WifiInfo> result = Lists.newArrayList();
        result.add(new WifiInfo(null, null, UUID.randomUUID().toString(), new Random().nextInt(MAX_SIGNAL_STRENGTH) * -1));
        result.add(new WifiInfo(null, null, UUID.randomUUID().toString(), new Random().nextInt(MAX_SIGNAL_STRENGTH) * -1));
        result.add(new WifiInfo(null, null, UUID.randomUUID().toString(), new Random().nextInt(MAX_SIGNAL_STRENGTH) * -1));
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

    @Override
    public void recalculate() {
        map.clear();
        fragment.calculateTriangulation();
        loadPoints();
    }

    @Override
    public void drawTriangle(@NonNull Triangle triangle) {
        if (map != null) {
            final int color = ContextCompat.getColor(fragment.getContext(), R.color.colorAccent);
            final PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.color(color);
            for (Point point : triangle.getDefiningPointList()) {
                polylineOptions.add(point.getPosition());
            }
            polylineOptions.add(triangle.getDefiningPointList().get(0).getPosition());
            map.addPolyline(polylineOptions);
        }
    }

    @Override
    public void drawIsoline(@NotNull Isoline isoline, @ColorInt int color) {
        if (map != null && !isoline.getIntersectionList().isEmpty()) {
            for (Isoline.Intersection intersection : isoline.getIntersectionList()) {
                boolean draw = Boolean.FALSE;
                final PolygonOptions polygoneOptions = new PolygonOptions();
                polygoneOptions.strokeColor(Color.TRANSPARENT);
                polygoneOptions.fillColor(color);
                if (!intersection.getCorrespondingPointList().isEmpty() && intersection.getCorrespondingPointList().size() < 3) {
                    polygoneOptions.add(intersection.getIntersectionPoint1());
                    polygoneOptions.add(intersection.getIntersectionPoint2());
                    draw = Boolean.TRUE;
                }
                for (final LatLng point : intersection.getCorrespondingPointList()) {
                    polygoneOptions.add(point);
                    draw = Boolean.TRUE;
                }
                if (draw) {
                    final LatLng upper = PointUtils.findUpperLeftPoint(polygoneOptions.getPoints());
                    Collections.sort(polygoneOptions.getPoints(), new LatLngComperator(upper));
                    map.addPolygon(polygoneOptions);
                }
            }
        }
    }
}