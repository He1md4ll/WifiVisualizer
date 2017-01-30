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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import org.greenrobot.greendao.annotation.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;

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

/**
 * Displays wifi data on google map
 */
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
        // Create database task controller to get access to local database
        final DaoSession daoSession = ((WifiVisualizerApp) fragment.getActivity().getApplication()).getDaoSession();
        dbController = new DatabaseTaskController(daoSession);
    }

    /**
     * Create google map and replace current view on main screen with the map
     * @param wrapper view in which the map will be placed
     */
    @Override
    public void initMap(View wrapper) {
        final SupportMapFragment supportMapFragment = SupportMapFragment.newInstance();
        supportMapFragment.getMapAsync(this);
        fragment.getFragmentManager().beginTransaction().replace(wrapper.getId(),supportMapFragment).commitAllowingStateLoss();
        markerMap = Collections.synchronizedMap(Maps.<Marker, Point>newHashMap());
    }


    /**
     * Callback method when google map is ready
     * Configures map ui elements and sets map behaviour (click, drag, marker click)
     * Starts first calculation to display captured wifi data on the map
     * @param map Google map instance
     */
    @Override
    public void onMapReady(GoogleMap map) {
        try {
            this.map = map;
            map.getUiSettings().setMyLocationButtonEnabled(false);
            map.getUiSettings().setZoomControlsEnabled(true);
            map.getUiSettings().setAllGesturesEnabled(true);
            map.getUiSettings().setMapToolbarEnabled(false);
            map.getUiSettings().setCompassEnabled(true);
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

    /**
     * Display wifi data for marker on defined location in popup window when user clicks on marker
     * Wifi data displayed in radar view (functionality imported form artifact 'com.github.PhilJay:MPAndroidChart')
     */
    private void setMarkerClickListener() {
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(fragment.getContext());
                // Get location and wifi data of current marker
                final Point point = markerMap.get(marker);
                final List<WifiInfo> wifiInfoList = point.getSignalStrength();
                if (wifiInfoList.isEmpty()) return false;

                // Find and create views in popup window
                final View v = fragment.getLayoutInflater(null).inflate(R.layout.marker_info_window, null);
                final TextView caption = (TextView) v.findViewById(R.id.marker_caption);
                final RadarChart barChart = (RadarChart) v.findViewById(R.id.marker_chart);

                // Build radar (for more information please refer to artifact 'com.github.PhilJay:MPAndroidChart')
                caption.setText("Found information for " + wifiInfoList.size() + " wifi networks");
                List<RadarEntry> barEntryList = Lists.newArrayList();
                for (WifiInfo wifiInfo : wifiInfoList) {
                    barEntryList.add(new RadarEntry(wifiInfo.getStrength(), wifiInfo.getSsid()));
                }
                final RadarDataSet barDataSet = new RadarDataSet(barEntryList, "Wifi data");
                barDataSet.setDrawValues(false);
                barDataSet.setLineWidth(2f);
                barDataSet.setColor(ContextCompat.getColor(fragment.getContext(), R.color.colorPrimaryDark));
                barDataSet.setDrawFilled(true);
                barDataSet.setFillColor(ContextCompat.getColor(fragment.getContext(), R.color.colorPrimary));
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

    /**
     * Center map on passed location
     * Current behaviour: Only center on inital location --> do not follow user
     * @param location current location
     */
    @Override
    public void centerOnLocation(@NonNull Location location) {
        if (map != null) {
            final LatLng myPosition = new LatLng(location.getLatitude(), location.getLongitude());
            if (zoomIn) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(myPosition, INITAL_ZOOM_LEVEL));
                zoomIn = Boolean.FALSE;
            } else {
                // TODO: Follow user?
                //map.animateCamera(CameraUpdateFactory.newLatLng(myPosition));
            }
        }
    }

    /**
     * Create map marker from point
     * @param point Point with location and wifi information
     * @return MarkerOptions, ready to be added to google map
     */
    private MarkerOptions createMarkerOptions(final Point point) {
        final MarkerOptions marker = new MarkerOptions();
        marker.draggable(Boolean.TRUE);
        marker.title("Messpunkt");
        marker.snippet("Signalstärke: " + point.getSignalStrength());
        marker.position(point.getPosition());
        return marker;
    }

    /**
     * Define behaviour for long click on map
     * --> Add new marker (and related Point) with random data to the map
     */
    private void setClickListener() {
        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                // Random wifi data
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
                        // After point is saved in database, recalculate
                        recalculate();
                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR);
            }
        });
    }

    /**
     * Creates random wifi data
     * @return Random wifi data
     */
    private List<WifiInfo> randomWifiInfoList() {
        List<WifiInfo> result = Lists.newArrayList();
        result.add(new WifiInfo(null, null, UUID.randomUUID().toString(), new Random().nextInt(MAX_SIGNAL_STRENGTH) * -1));
        result.add(new WifiInfo(null, null, UUID.randomUUID().toString(), new Random().nextInt(MAX_SIGNAL_STRENGTH) * -1));
        result.add(new WifiInfo(null, null, UUID.randomUUID().toString(), new Random().nextInt(MAX_SIGNAL_STRENGTH) * -1));
        return result;
    }

    /**
     * Deletes marker and related point in database on marker drag
     */
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

    /**
     * Clears google map (deletes marker, line, ...) and triggers recalculation to draw everything again
     */
    @Override
    public void recalculate() {
        map.clear();
        fragment.calculate();
    }

    /**
     * Draws point as marker on the map
     * @param point Point to draw
     */
    @Override
    public void drawMarker(@NonNull Point point) {
        if (map != null) {
            final Marker marker = map.addMarker(createMarkerOptions(point));
            markerMap.put(marker, point);
        }
    }

    /**
     * Draws triangle on the map as polyline
     * Triangle color is defined my the app (accent color)
     * @param triangle Trinagle to draw
     */
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

    /**
     * Draws isoline on the map as polyline
     * Isoline is defined by triangle intersections --> all intersection points added to polyline
     * Method only used in 'single isoline mode' where the iso value is defined by slider on the map
     * @param isoline
     * @param color
     */
    @Override
    public void drawIsoline(@NotNull final Isoline isoline, final Integer color) {
        if (map != null) {
            List<PolylineOptions> polylineOptionsList = Lists.newArrayList();
            // Iterate over all triangle intersections to get whole iso line
            // --> one intersection result in one polyline
            for (Isoline.Intersection intersection : isoline.getIntersectionList()) {
                List<LatLng> pointList = Lists.newArrayList();
                // Add intersection points to local point list
                if (!intersection.getCorrespondingPointList().isEmpty() && intersection.getCorrespondingPointList().size() < 3) {
                    pointList.add(intersection.getIntersectionPoint1());
                    pointList.add(intersection.getIntersectionPoint2());
                }
                // Add polyline to polyline list
                polylineOptionsList.add(new PolylineOptions().addAll(pointList).color(color));
            }
            // Clear map and draw all polylines to display the isoline
            map.clear();
            for (PolylineOptions polylineOptions : polylineOptionsList) {
                map.addPolyline(polylineOptions);
            }
        }
    }

    /**
     * Draws heatmap on the map as multiple polygons using passed isolines
     * Every isoline can result in multiple polygons
     * Polygons realted to one isoline are merged (union) for better map performance (drawing every single polygon on the map was heavy for the map)
     * @param isolineList List if isolines to draw
     * @param colorList Isoline related colors for the heatmap
     * @return Task for calculationg and drawing the heatmap
     */
    @Override
    public Task<Void> drawHeatmap(@NotNull final List<Isoline> isolineList, final List<Integer> colorList) {
        if (map != null && isolineList.size() <= colorList.size()) {
            // Every isoline is computed in a separate task --> list holds tasks
            final List<Task<List<PolygonOptions>>> taskList = Lists.newArrayList();
            for (int i = 0; i <isolineList.size(); i++) {
                final Isoline isoline = isolineList.get(i);
                final int color = colorList.get(i);
                // Line must have triangle intersections to be valid
                if (!isoline.getIntersectionList().isEmpty()) {
                    // Add task to list and execute asynchronously
                    taskList.add(Task.callInBackground(new Callable<List<PolygonOptions>>() {
                        @Override
                        public List<PolygonOptions> call() throws Exception {
                            List<List<LatLng>> polygonList = Lists.newArrayList();
                            // Iterate over all triangle intersections of isoline
                            // Every intersection results in one polygon for now
                            for (Isoline.Intersection intersection : isoline.getIntersectionList()) {
                                List<LatLng> pointList = Lists.newArrayList();
                                // Add intersection points to polygon
                                if (!intersection.getCorrespondingPointList().isEmpty() && intersection.getCorrespondingPointList().size() < 3) {
                                    pointList.add(intersection.getIntersectionPoint1());
                                    pointList.add(intersection.getIntersectionPoint2());
                                }
                                // Add corresponding points to polygon
                                // Corresponding point = triangle point inside the iso line, thus point inside of the heatmap
                                pointList.addAll(intersection.getCorrespondingPointList());
                                // Sort polygon points in clockwise-order
                                final LatLng upper = PointUtils.findUpperLeftPoint(pointList);
                                Collections.sort(pointList, new LatLngComperator(upper));
                                // Add polygon point to polygon list
                                polygonList.add(pointList);
                            }
                            // Union all polygons of isoline to save map performance
                            return unionPolygons(polygonList, color);
                        }
                    }));
                }
            }

            // Wait for all tasks to finish --> then draw polygons
            return Task.whenAllResult(taskList).onSuccess(new Continuation<List<List<PolygonOptions>>, Void>() {
                @Override
                public Void then(Task<List<List<PolygonOptions>>> task) throws Exception {
                    for (List<PolygonOptions> polygonList : task.getResult()) {
                        for (PolygonOptions options: polygonList) {
                            map.addPolygon(options);
                        }
                    }
                    return null;
                }
            }, Task.UI_THREAD_EXECUTOR);
        }
        return Task.forError(new IllegalStateException("Map not ready"));
    }

    /**
     * Merge multiple polygons
     * Union mechanism provided by 'com.vividsolutions:jts:1.13' library (implementation of Java Topology Suite)
     * @param polygoneOptions Polygons to merge
     * @param color color for resulting polygons
     * @return Merged polygons
     */
    private List<PolygonOptions> unionPolygons(List<List<LatLng>> polygoneOptions, @ColorInt final int color) {
        Geometry union = null;
        final List<PolygonOptions> result = Lists.newArrayList();

        try {
            // Iterate over all polygons to merge them
            for (List<LatLng> pointList : polygoneOptions) {
                // Add first point as last to close polygon (required by JTS lib)
                pointList.add(pointList.get(0));
                // Transform LatLng to JTS data structure 'Coordinate'
                final List<Coordinate> coordinateList = Lists.transform(pointList, new Function<LatLng, Coordinate>() {
                    @Override
                    public Coordinate apply(LatLng input) {
                        return new Coordinate(input.latitude, input.longitude);
                    }
                });
                // Create JTS polygon from transformed polygon points
                final Polygon polygon = new GeometryFactory().createPolygon(Iterables.toArray(coordinateList, Coordinate.class));
                // Union with previous polygon
                if (union == null) {
                    union = polygon;
                } else {
                    union = union.union(polygon);
                }
            }

            // Extract all polygons from merged geometry structure (JTS data structure)
            final List<Polygon> linearRingList = extractPolygons(union);
            // Transform JTS polygon to google map polygon and add to result list
            for (Polygon polygon : linearRingList) {
                result.add(buildPolygonOptions(polygon, color));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Transforms JTS polygon to google map polygon with color
     * @param polygon JTS polygon structure
     * @param color polygon color on heatmap
     * @return Google map polygon structure
     */
    private PolygonOptions buildPolygonOptions(Polygon polygon, int color) {
        // Build polygon and set color
        final PolygonOptions options = new PolygonOptions().fillColor(color).strokeColor(Color.TRANSPARENT);
        // JTS polygon can have one exterior ring (border) and multiple interior rings (hols)
        // Border is added as polygon points
        options.addAll(convertLinearRing((LinearRing) polygon.getExteriorRing()));
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            // Hols are added as polygon hols
            options.addHole(convertLinearRing((LinearRing) polygon.getInteriorRingN(i)));
        }
        return options;
    }

    /**
     * Converts linear ring JTS data structure to LatLng point list
     * @param linearRing linear ring JTS data structure
     * @return Point list
     */
    private List<LatLng> convertLinearRing (LinearRing linearRing) {
        final List<LatLng> result = Lists.newArrayList();
        for (int i = 1; i < linearRing.getNumPoints(); i++) {
            final com.vividsolutions.jts.geom.Point point = linearRing.getPointN(i);
            result.add(new LatLng(point.getX(), point.getY()));
        }
        return result;
    }

    /**
     * Extracts all polygons from JTS data structure
     * @param polygonStructure Merged polygon geometry
     * @return Polygon list
     */
    private List<Polygon> extractPolygons(Geometry polygonStructure) {
        final List<Polygon> result = Lists.newArrayList();
        // Geometry can either be one single polygon
        if (polygonStructure instanceof Polygon) {
            result.add((Polygon) polygonStructure);
        // Or multiple polygons
        } else if (polygonStructure instanceof MultiPolygon) {
            for (int i = 0; i < polygonStructure.getNumGeometries(); i++) {
                result.add((Polygon) polygonStructure.getGeometryN(i));
            }
        }
        return result;
    }
}