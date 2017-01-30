package edu.hsb.wifivisualizer.map;

import android.content.DialogInterface;
import android.content.IntentSender;
import android.graphics.Color;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.api.Status;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultiset;
import com.google.common.primitives.Ints;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import butterknife.BindView;
import butterknife.ButterKnife;
import edu.hsb.wifivisualizer.DatabaseTaskController;
import edu.hsb.wifivisualizer.PreferenceController;
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

/**
 * View to display map together with captured points and resulting calculations (trinagles, isoline, heatmap)
 * User can configure witch information is displayed
 * User can filter captured information by a specific SSID and iso values
 *
 * This view provides to main modes:
 * 1. Draw heatmap from captured data using SSID filter und defined iso values in filter. Markers and triangles are optional.
 * 2. Draw a single iso line from captured data using SSID filter and iso value from displayed seek bar. Only the iso line is displayed.
 */
public class MapFragment extends Fragment implements ILocationListener {

    public static final String TAG = MapFragment.class.getSimpleName();
    public static final int PERMISSIONS_ACCESS_FINE_LOCATION = 10;

    @BindView(R.id.map_wrapper)
    View wrapper;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.map_seek)
    SeekBar seekBar;

    private IMapService mapService;
    private GoogleLocationProvider googleLocationProvider;
    private Set<String> ssidSet = Sets.newLinkedHashSet();

    private DatabaseTaskController dbController;
    private IDelaunayService delaunayService;
    private IIsoService isoService;
    private PreferenceController preferenceController;

    private Snackbar locationSnackbar;
    private Snackbar requiredSnackbar;

    private boolean renderMarker = Boolean.TRUE;
    private boolean renderTriangle = Boolean.TRUE;
    private boolean renderIsoline = Boolean.TRUE;
    private boolean renderHeatmap = Boolean.FALSE;
    private boolean colorSwitch = Boolean.FALSE;

    private List<Triangle> triangleCache;

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
        // Inject bound views from above
        ButterKnife.bind(this, view);
        // Create service to draw data on map
        mapService = new GoogleMapService(this);
        // Create location provider to receive location updates (zoom in on user location)
        googleLocationProvider = new GoogleLocationProvider(this.getContext());

        // Create database controller to access local database
        final DaoSession daoSession = ((WifiVisualizerApp) getActivity().getApplication()).getDaoSession();
        dbController = new DatabaseTaskController(daoSession);
        // Create service to compute delaunay triangulation
        delaunayService = new IncrementalDelaunayService();
        // Create service to compute iso lines from triangles (triangle intersections)
        isoService = new SimpleIsoService();
        // Create preference controller to access app properties
        preferenceController = new PreferenceController(getContext());
        buildSnackbars();
        // Initialize map to trigger data drawing (map will call 'calculate' method when ready)
        mapService.initMap(wrapper);
        // Set on value change behaviour for seek bar for mode 2 (only draw one iso line from seek bar value)
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Calculation of isoline needs previously computed triangles
                if (triangleCache != null) {
                    drawIsolines(triangleCache);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    /**
     * Starts listening for location updates from location provider
     */
    @Override
    public void onStart() {
        googleLocationProvider.startListening(this);
        super.onStart();
    }

    /**
     * Stops listening for location updates
     */
    @Override
    public void onStop() {
        super.onStop();
        googleLocationProvider.stopListening();
    }

    /**
     * Sets menu in toolbar of app (FILTER and RENDER options)
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar, menu);
    }

    /**
     * Called by android when user selects menu item in toolbar
     * @param item selected menu item
     * @return handled?
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // FILTER pressed
        // Create dialog with fields for iso values and ssid list (singel choise list --> user can select one item in list)
        if (id == R.id.action_filter) {
            final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
            final View dialogRootView = getLayoutInflater(null).inflate(R.layout.filter_window, null);
            final TextView filterTextView = (TextView) dialogRootView.findViewById(R.id.filter_text);
            final Button filterButtonView = (Button) dialogRootView.findViewById(R.id.filter_button);
            final ListView listView = (ListView) dialogRootView.findViewById(R.id.list);

            final AlertDialog dialog = dialogBuilder.setView(dialogRootView).create();
            // Fill SSID list with data from ssidSet
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_list_item_single_choice,
                    android.R.id.text1,
                    Lists.newArrayList(ssidSet));
            listView.setAdapter(adapter);
            // Select previously selected value
            listView.setItemChecked(preferenceController.getFilter(),  Boolean.TRUE);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // Save selection in app preferences to get access to choice later
                    preferenceController.setFilter(position);
                    // Trigger recalculation for map
                    mapService.recalculate();
                    dialog.dismiss();
                }
            });

            filterTextView.setText(preferenceController.getIsoValues());

            filterButtonView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Save specified is values in app preferences
                    preferenceController.setIsoValues(filterTextView.getText().toString());
                    // Trigger recalculation for map
                    mapService.recalculate();
                    dialog.dismiss();
                }
            });
            dialog.show();
            return true;
        // RENDER pressed
        // Create dialog with choice boxes to configure what is displayed on map
        } else if (id == R.id.action_render) {
            final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
            // Find and create choice boxes
            final View dialogRootView = getLayoutInflater(null).inflate(R.layout.render_window, null);
            final CheckBox markerCheckBoxView = (CheckBox) dialogRootView.findViewById(R.id.marker_checkbox);
            final CheckBox triangleCheckBoxView = (CheckBox) dialogRootView.findViewById(R.id.triangle_checkbox);
            final CheckBox isolineCheckBoxView = (CheckBox) dialogRootView.findViewById(R.id.isoline_checkbox);
            final CheckBox heatmapCheckBoxView = (CheckBox) dialogRootView.findViewById(R.id.heatmap_checkbox);
            final CheckBox colorSwitchView = (CheckBox) dialogRootView.findViewById(R.id.color_switch);

            // Each choice box state is reflected by one boolean value
            markerCheckBoxView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    renderMarker = isChecked;
                }
            });
            triangleCheckBoxView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    renderTriangle = isChecked;
                }
            });
            // Either iso line or heat map is active (2 modes) --> hide or show seek bar
            isolineCheckBoxView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    renderIsoline = isChecked;
                    heatmapCheckBoxView.setEnabled(!isChecked);
                    heatmapCheckBoxView.setChecked(!isChecked);
                    if (renderIsoline) {
                        seekBar.setVisibility(View.VISIBLE);
                    } else {
                        seekBar.setVisibility(View.GONE);
                    }
                }
            });
            // Either iso line or heat map is active (2 modes)
            heatmapCheckBoxView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    renderHeatmap = isChecked;
                    isolineCheckBoxView.setEnabled(!isChecked);
                    isolineCheckBoxView.setChecked(!isChecked);
                }
            });
            colorSwitchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    colorSwitch = isChecked;
                }
            });

            // Set in fragment reflected state to choice boxes on dialog open
            markerCheckBoxView.setChecked(renderMarker);
            triangleCheckBoxView.setChecked(renderTriangle);
            isolineCheckBoxView.setChecked(renderIsoline);
            heatmapCheckBoxView.setChecked(renderHeatmap);
            colorSwitchView.setChecked(colorSwitch);

            dialogBuilder.setNeutralButton("OK", null);
            final AlertDialog dialog = dialogBuilder.setView(dialogRootView).create();
            // Trigger recalculation on dialog close
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mapService.recalculate();
                }
            });
            dialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called on location change from location provider
     * Centers map on current location
     * @param location current location
     */
    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            locationSnackbar.dismiss();
            mapService.centerOnLocation(location);
        }
    }

    /**
     * User needs to activate location services
     * @param status
     */
    @Override
    public void onResolutionNeeded(Status status) {
        try {
            status.startResolutionForResult(this.getActivity(),
                    MapFragment.PERMISSIONS_ACCESS_FINE_LOCATION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Unexpected error while resolving location intent", e);
        }
    }

    /**
     * Called when location provider can not get location
     */
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

    /**
     * Display location and permission information to user in snackbar
     */
    private void buildSnackbars() {
        final int color = ContextCompat.getColor(this.getActivity(), R.color.colorPrimary);
        locationSnackbar = Snackbar.make(wrapper, "Searching for location", Snackbar.LENGTH_INDEFINITE);
        locationSnackbar.getView().setBackgroundColor(color);
        locationSnackbar.show();

        requiredSnackbar = Snackbar.make(wrapper, "Permission required", Snackbar.LENGTH_INDEFINITE);
        requiredSnackbar.getView().setBackgroundColor(color);
    }

    /**
     * Main method to compute triangulation, iso line extraction and heatmap generation
     * Method is always called from map service
     * Method used map service to draw computed data on map
     * Method uses task chaining (facebook bolts library 'com.parse.bolts:bolts-tasks:1.4.0')
     */
    public void calculate() {
        // Indicate computing process
        progressBar.setVisibility(View.VISIBLE);
        // Draw points on map
        drawPoints().onSuccessTask(new Continuation<List<Point>, Task<List<Triangle>>>() {
            @Override
            public Task<List<Triangle>> then(Task<List<Point>> task) throws Exception {
                // On success return drawTrinagles task
                return drawTrinagles(task.getResult());
            }
        }).onSuccessTask(new Continuation<List<Triangle>, Task<Void>>() {
            @Override
            public Task<Void> then(Task<List<Triangle>> task) throws Exception {
                // On success return drawIsolines task
                return drawIsolines(task.getResult());
            }
        }).onSuccessTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Void> task) throws Exception {
                // On success return drawHeatmap task
                return drawHeatmap(triangleCache);
            }
        }).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                // In any case hide process indicator after calculation --> process chain complete
                progressBar.setVisibility(View.GONE);
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    /**
     * Method defines task to draw points as markers on map using map service
     * Points are initially loaded from database and returned with the task
     * Points are only drawn if related render option is active
     * @return Task that stores the loaded point list
     */
    private Task<List<Point>> drawPoints() {
        return dbController.getPointList().onSuccess(new Continuation<List<Point>, List<Point>>() {
            @Override
            public List<Point> then(final Task<List<Point>> task) throws Exception {
                final List<Point> result = task.getResult();
                if (renderMarker) {
                    for (Point point : result) {
                        mapService.drawMarker(point);
                    }
                }
                return result;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    /**
     * Method defines task to draw triangles on map using map service
     * Triangles are computed from previously loaded points using delaunay service
     * Triangles are onyl drawn if related render option is active
     * @param pointList Point list
     * @return Task that sores the computed triangles
     */
    private Task<List<Triangle>> drawTrinagles(final List<Point> pointList) {
        return Task.callInBackground(new Callable<List<Triangle>>() {
            @Override
            public List<Triangle> call() throws Exception {
                extractSsids(pointList);
                return delaunayService.calculate(pointList);
            }
        }).onSuccess(new Continuation<List<Triangle>, List<Triangle>>() {
            @Override
            public List<Triangle> then(final Task<List<Triangle>> task) throws Exception {
                final List<Triangle> result = task.getResult();
                triangleCache = result;
                if (renderTriangle) {
                    for (Triangle triangle : result) {
                        mapService.drawTriangle(triangle);
                    }
                }
                return result;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    /**
     * Method defines task to draw Isoline on map using map service
     * Isoline is computed from previously computed triangles using iso service
     * User can select SSID in filter --> selected filter option is loaded from app preferences
     * Isoline is only drawn in mode 2 (iso line render option active)
     * @param triangleList Triangle list
     * @return Task that sores the computed Isoline
     */
    private Task<Void> drawIsolines(final List<Triangle> triangleList) {
        if (!renderIsoline) {
            return Task.forResult(null);
        } else {
            return Task.callInBackground(new Callable<List<Isoline>>() {
                @Override
                public List<Isoline> call() throws Exception {
                    String ssid = null;
                    final int selectedSSIDPosition = preferenceController.getFilter();
                    if (selectedSSIDPosition != 0) {
                        ssid = Iterables.get(ssidSet, selectedSSIDPosition, null);
                    }
                    final List<Integer> integerList = Lists.newArrayList(seekBar.getProgress() - 100);
                    return isoService.extractIsolines(triangleList, integerList, ssid);
                }
            }).onSuccess(new Continuation<List<Isoline>, Void>() {
                @Override
                public Void then(Task<List<Isoline>> task) throws Exception {
                    final List<Isoline> lines = task.getResult();
                    mapService.drawIsoline(lines.get(0), colorMap(lines, 255).get(0));
                    return null;
                }
            }, Task.UI_THREAD_EXECUTOR);
        }
    }

    /**
     * Method defines task to draw Heatmap on map using map service
     * Heatmap is computed from isolines (triangle intersections) which are computed from previously computed triangles
     * User can select SSID in filter --> selected filter option is loaded from app preferences
     * Heatmap is only drawn in mode 1 (heatmap render option active)
     * @param triangleList Triangle list
     * @return Task without value (user can wait for task to finish)
     */
    private Task<Void> drawHeatmap(final List<Triangle> triangleList) {
        return Task.callInBackground(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (renderHeatmap) {
                    String ssid = null;
                    final int selectedSSIDPosition = preferenceController.getFilter();
                    if (selectedSSIDPosition != 0) {
                        ssid = Iterables.get(ssidSet, selectedSSIDPosition, null);
                    }
                    // Get selected iso values and save them to a list
                    final String selectedIsoValues = preferenceController.getIsoValues();
                    final List<Integer> integerList = Lists.transform(Lists.newArrayList(selectedIsoValues.split(";")), new Function<String, Integer>() {
                        @Override
                        public Integer apply(String input) {
                            Integer result = null;
                            try {
                                result = Integer.valueOf(input);
                            } catch (Exception ignored){}
                            return result;
                        }
                    });
                    final List<Isoline> lines = isoService.extractIsolines(triangleList, integerList, ssid);

                    //Sort Isolines to draw weak signals first
                    Collections.sort(lines, new Comparator<Isoline>() {
                        @Override
                        public int compare(Isoline o1, Isoline o2) {
                            return Ints.compare(o1.getIsovalue(), o2.getIsovalue());
                        }
                    });
                    // Wait for drawHeatmap task to finish then continue
                    mapService.drawHeatmap(lines, colorMap(lines)).waitForCompletion();
                }
                return null;
            }
        });
    }

    /**
     * Extracts all SSIDs from the provided wifi information of all points
     * Method is used to create filter ssid filter options
     * Method uses TreeMultiset (Google Guava Collection type) to sort values by number of occurrences
     * method uses Set to have distinct SSID values
     * @param pointList
     */
    private void extractSsids(List<Point> pointList) {
        ssidSet.clear();
        ssidSet.add("All");
        final Multiset<String> multiset = TreeMultiset.create();
        for (Point point : pointList) {
            for (WifiInfo info : point.getSignalStrength()) {
                ssidSet.add(info.getSsid());
                multiset.add(info.getSsid());
            }
        }
        ssidSet.addAll(Multisets.copyHighestCountFirst(multiset).elementSet());
    }

    private List<Integer> colorMap(List<Isoline> lines){
        return colorMap(lines, 125);
    }

    /**
     * Provides colors for isolines
     * Method supports 2 modes:
     * 1. Color is related to iso value of Isoline (low value = red, high value = green)
     * 2. Color is related to count of Isolines (first Isoline red, last isoline green, in between values based on distance to other iso values)
     * Also see http://stackoverflow.com/a/13249391 for mor information regarding HSVToColor
     * @param lines list of Isolines
     * @param alpha Alpha value of color
     * @return Color list (same size as lines)
     */
    private List<Integer> colorMap(List<Isoline> lines, final int alpha){
        if (colorSwitch) {
            return Lists.transform(lines, new Function<Isoline, Integer>() {
                @Override
                public Integer apply(Isoline input) {
                    return Color.HSVToColor(alpha, new float[]{(float)((double)(input.getIsovalue()+100)/(100))*120f,1f,1f});
                }
            });
        } else {
            final int colorIntecrement = 120 / lines.size();
            final List<Integer> result = Lists.newArrayList();
            for (int i = 0; i < lines.size(); i++) {
                int colorInt = Color.HSVToColor(alpha, new float[]{
                        i * colorIntecrement , 1f, 1f
                });
                result.add(colorInt);
            }
            return result;
        }
    }
}