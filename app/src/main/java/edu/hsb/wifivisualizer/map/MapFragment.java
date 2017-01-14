package edu.hsb.wifivisualizer.map;

import android.content.DialogInterface;
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
import com.google.common.collect.Sets;
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
        ButterKnife.bind(this, view);
        mapService = new GoogleMapService(this);
        googleLocationProvider = new GoogleLocationProvider(this.getContext());

        final DaoSession daoSession = ((WifiVisualizerApp) getActivity().getApplication()).getDaoSession();
        dbController = new DatabaseTaskController(daoSession);
        delaunayService = new IncrementalDelaunayService();
        isoService = new SimpleIsoService();
        preferenceController = new PreferenceController(getContext());
        buildSnackbars();
        mapService.initMap(wrapper);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
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
            final TextView filterTextView = (TextView) dialogRootView.findViewById(R.id.filter_text);
            final Button filterButtonView = (Button) dialogRootView.findViewById(R.id.filter_button);
            final ListView listView = (ListView) dialogRootView.findViewById(R.id.list);

            final AlertDialog dialog = dialogBuilder.setView(dialogRootView).create();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_list_item_single_choice,
                    android.R.id.text1,
                    Lists.newArrayList(ssidSet));
            listView.setAdapter(adapter);
            listView.setItemChecked(preferenceController.getFilter(),  Boolean.TRUE);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    preferenceController.setFilter(position);
                    mapService.recalculate();
                    dialog.dismiss();
                }
            });

            filterTextView.setText(preferenceController.getIsoValues());

            filterButtonView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    preferenceController.setIsoValues(filterTextView.getText().toString());
                    mapService.recalculate();
                    dialog.dismiss();
                }
            });
            dialog.show();
            return true;
        } else if (id == R.id.action_render) {
            final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
            final View dialogRootView = getLayoutInflater(null).inflate(R.layout.render_window, null);
            final CheckBox markerCheckBoxView = (CheckBox) dialogRootView.findViewById(R.id.marker_checkbox);
            final CheckBox triangleCheckBoxView = (CheckBox) dialogRootView.findViewById(R.id.triangle_checkbox);
            final CheckBox isolineCheckBoxView = (CheckBox) dialogRootView.findViewById(R.id.isoline_checkbox);
            final CheckBox heatmapCheckBoxView = (CheckBox) dialogRootView.findViewById(R.id.heatmap_checkbox);

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
            heatmapCheckBoxView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    renderHeatmap = isChecked;
                    isolineCheckBoxView.setEnabled(!isChecked);
                    isolineCheckBoxView.setChecked(!isChecked);
                }
            });

            markerCheckBoxView.setChecked(renderMarker);
            triangleCheckBoxView.setChecked(renderTriangle);
            isolineCheckBoxView.setChecked(renderIsoline);
            heatmapCheckBoxView.setChecked(renderHeatmap);

            dialogBuilder.setNeutralButton("OK", null);
            final AlertDialog dialog = dialogBuilder.setView(dialogRootView).create();
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
        // Indicate process
        progressBar.setVisibility(View.VISIBLE);
        drawPoints().onSuccessTask(new Continuation<List<Point>, Task<List<Triangle>>>() {
            @Override
            public Task<List<Triangle>> then(Task<List<Point>> task) throws Exception {
                return drawTrinagles(task.getResult());
            }
        }).onSuccessTask(new Continuation<List<Triangle>, Task<Void>>() {
            @Override
            public Task<Void> then(Task<List<Triangle>> task) throws Exception {
                return drawIsolines(task.getResult());
            }
        }).onSuccessTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Void> task) throws Exception {
                return drawHeatmap(triangleCache);
            }
        }).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                // Hide process indicator --> process chain complete
                progressBar.setVisibility(View.GONE);
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

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

    private Task<Void> drawIsolines(final List<Triangle> triangleList) {
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
                List<Isoline> lines = task.getResult();
                if(renderIsoline) {
                    mapService.drawIsoline(lines, colorMap(lines, 255));
                }
                return null;
            }
        },Task.UI_THREAD_EXECUTOR);
    }

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
                    // Wait for drawHeatmap task to finish
                    mapService.drawHeatmap(lines, colorMap(lines)).waitForCompletion();
                }
                return null;
            }
        });
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

    private List<Integer> colorMap(List<Isoline> lines){
        return colorMap(lines, 125);
    }

    /**
     * Siehe http://stackoverflow.com/a/13249391
     * @param lines
     * @return
     */
    private List<Integer> colorMap(List<Isoline> lines, final int alpha){
        return Lists.transform(lines, new Function<Isoline, Integer>() {
            @Override
            public Integer apply(Isoline input) {
                return android.graphics.Color.HSVToColor(alpha, new float[]{(float)((double)(input.getIsovalue()+100)/(100))*120f,1f,1f});
            }
        });
    }
}