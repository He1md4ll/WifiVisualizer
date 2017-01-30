package edu.hsb.wifivisualizer;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nononsenseapps.filepicker.FilePickerActivity;

import java.io.File;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import butterknife.BindView;
import butterknife.ButterKnife;
import edu.hsb.wifivisualizer.capture.CaptureFragment;
import edu.hsb.wifivisualizer.map.MapFragment;
import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.WifiInfo;

/**
 * Main Activity of the app that is started on app launch
 * Manages user menu, user interactions and displayed fragments and views
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int EXIT_REACTION_TIME = 3 * 1000;
    private static final int PERMISSIONS_ACCESS_FINE_LOCATION = 1;
    private static final int PERMISSIONS_WRITE_EXTERNAL_STORAGE = 2;
    private static final int FILE_CODE = 3;
    private static final int GOOGLE_RESOLUTION = 4;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;
    @BindView(R.id.nav_view)
    NavigationView navigationView;

    @BindView(R.id.fragment_placeholder)
    View fragmentView;

    private boolean exit = Boolean.FALSE;
    private boolean hasPermission = Boolean.FALSE;
    private DatabaseTaskController dbController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Injects bound views defined above
        ButterKnife.bind(this);

        // Create toolbar and navigation menu
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // Create database controller to access local database
        dbController = new DatabaseTaskController(((WifiVisualizerApp) getApplication()).getDaoSession());
        // Check if app has all needed permissions
        checkLocationPermission();
    }

    /**
     * Called when user taps on the back button
     * Closes drawer if open, or closes app on double tap on back button
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (exit)
                // Minimize app
                moveTaskToBack(Boolean.TRUE);
            else {
                // Ask if user really wants to close the app
                Toast.makeText(this, "Drück erneut auf Zurück um die App zu beenden.",
                        Toast.LENGTH_SHORT).show();
                exit = true;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        exit = Boolean.FALSE;
                    }
                }, EXIT_REACTION_TIME);
            }
        }
    }

    /**
     * Handle clicks in and on the navigation menu
     * @param item selected item
     * @return handled?
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_capture:
                // Replace current fragment with capture fragment
                replaceFragment(new CaptureFragment());
                break;
            case R.id.nav_map:
                // Replace current fragment with map fragment
                replaceFragment(new MapFragment());
                break;
            case R.id.nav_import:
                // Check user permission and open dialog to import data into local database
                checkStoragePermission(false);
                break;
            case R.id.nav_export:
                // Check user permission and export data from local database
                checkStoragePermission(true);
                break;
            case R.id.nav_clear:
                // remove all data from local database and give feedback to user
                dbController.clearAll().continueWith(new Continuation<Void, Void>() {
                    @Override
                    public Void then(Task<Void> task) throws Exception {
                        Toast.makeText(MainActivity.this, task.isCompleted() && !task.isFaulted() ?
                                "Successfully deleted all data" :
                                "Could not delete data", Toast.LENGTH_LONG).show();
                        // Load map fragment after data clear to reset map view
                        replaceFragment(new MapFragment());
                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR);
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Called after permission resolution by user
     * User can confirm or decline permissions
     * @param requestCode Callback code
     * @param permissions Requested permissions
     * @param grantResults Granted permission results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_ACCESS_FINE_LOCATION:
                // App allowed to monitor phone location?
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasPermission = Boolean.TRUE;
                    replaceFragment(new MapFragment());
                } else {
                    showLocationRequiredAlert();
                }
                break;
            case PERMISSIONS_WRITE_EXTERNAL_STORAGE:
                // App allowed to access phone storage?
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Write permission granted");
                } else {
                    Log.w(TAG, "Write permission denied");
                }
                break;
        }
    }

    /**
     * Callback for results from other apps
     * @param requestCode Callback code
     * @param resultCode Successful?
     * @param data Optional data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        switch (requestCode) {
            case GOOGLE_RESOLUTION:
                // Could location problem be resolved?
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.w(TAG, "User chose not to make required location settings changes.");
                        break;
                }
                break;
            case FILE_CODE:
                // Could selected file be loaded?
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Task.callInBackground(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                // Uri of selected file
                                final Uri uri = data.getData();
                                // Load file from uri
                                final String importString = Files.toString(new File(uri.getPath()), Charsets.UTF_8);
                                // Import file into local database --> Parse json data and save points and wifi info
                                final List<Point> pointList = new Gson().fromJson(importString, new TypeToken<List<Point>>(){}.getType());
                                for (final Point point : pointList) {
                                    point.setId(null);
                                    point.setAverageStrength(PointUtils.calculateAverageStrength(point.getSignalStrength()));
                                    dbController.savePoint(point).onSuccessTask(new Continuation<Point, Task<List<WifiInfo>>>() {
                                        @Override
                                        public Task<List<WifiInfo>> then(Task<Point> task) throws Exception {
                                            final Long pointId = task.getResult().getId();
                                            for(WifiInfo info : point.getSignalStrength()) {
                                                info.setId(null);
                                                info.setPointId(pointId);
                                            }
                                            return dbController.saveWifiInfoList(point.getSignalStrength());
                                        }
                                    });
                                }
                                return null;
                            }
                        }).continueWith(new Continuation<Void, Void>() {
                            @Override
                            public Void then(Task<Void> task) throws Exception {
                                // Display import result to user
                                Toast.makeText(MainActivity.this, task.isCompleted() && !task.isFaulted() ?
                                        "Successfully inserted points from selected file" :
                                        "Could not insert points", Toast.LENGTH_LONG).show();
                                // Reset map view by replacing the fragment
                                replaceFragment(new MapFragment());
                                return null;
                            }
                        }, Task.UI_THREAD_EXECUTOR);
                }
                break;
        }
    }

    /**
     * Replace current fragment with new one using android fragment manager transaction system
     * @param fragment
     */
    private void replaceFragment(Fragment fragment) {
        // Replace if app has location permission or display error to user
        if (hasPermission) {
            fragmentView.setVisibility(View.VISIBLE);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_placeholder, fragment).commitAllowingStateLoss();
        } else {
            showLocationRequiredAlert();
        }
    }

    /**
     * Check if app has permission to access phone location
     * If permission not granted try to resolve this issue by asking the user for permission using 'requestPermissions'
     */
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_ACCESS_FINE_LOCATION);
        } else {
            hasPermission = Boolean.TRUE;
            replaceFragment(new MapFragment());
        }
    }

    /**
     * Displays location error to user --> app can not operate without phone location
     */
    private void showLocationRequiredAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Location required")
                .setMessage("This app requires location information.\n\nPlease grant access to your location.")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        checkLocationPermission();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Export data from local database to json file
     * File is saved to app related folder on external device storage
     * Data is converted to json by google Gson implementation (library 'com.google.code.gson:gson:2.8.0')
     */
    private void exportData() {
        // Load all points from database and save them to json file
        dbController.getPointList().onSuccess(new Continuation<List<Point>, Void>() {
            @Override
            public Void then(Task<List<Point>> task) throws Exception {
                final SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd");
                final String currentDateandTime = sdf.format(new Date());
                final String externalStoragePath = Environment.getExternalStorageDirectory() + "/WifiVisualizer";
                final File mkdirs = new File(externalStoragePath);
                mkdirs.mkdirs();
                final File file = new File(mkdirs, "data_" + currentDateandTime + ".txt");
                file.delete();
                Files.write(new Gson().toJson(task.getResult()), file, Charset.defaultCharset());
                return null;
            }
        }, Task.BACKGROUND_EXECUTOR).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                // Inform user about export reuslt
                Toast.makeText(MainActivity.this, task.isCompleted() && !task.isFaulted() ?
                        "Successfully saved data to file" :
                        "Could not save data to file", Toast.LENGTH_LONG).show();
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    /**
     * Import data to local database from selected file on external device storage
     * Implementation to pick a file from storage is provided by library 'com.nononsenseapps:filepicker:3.1.0'
     */
    private void importData() {
        final Intent intent = new Intent(this, FilePickerActivity.class);
        intent.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        intent.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        intent.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
        intent.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
        startActivityForResult(intent, FILE_CODE);
    }

    /**
     * Checks app permission to read and write external phone storage
     * If permission is not granted it is requested (same as location above)
     * Method supports read and write variant
     * @param write Write or read?
     */
    private void checkStoragePermission(boolean write) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_WRITE_EXTERNAL_STORAGE);
        } else {
            if (write) {
                exportData();
            } else {
                importData();
            }
        }
    }
}