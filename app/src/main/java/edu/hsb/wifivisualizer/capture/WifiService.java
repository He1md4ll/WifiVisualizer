package edu.hsb.wifivisualizer.capture;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Service to scan for wifi data using android WifiManger
 * Forwards scan results to registered listeners
 */
public class WifiService extends Service {

    private final IBinder binder = new LocalBinder();
    private WifiManager wifiManager;
    private WifiReceiver wifiReceiver;
    private ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    private List<IWifiListener> listenerList = Lists.newArrayList();
    private Runnable wifiScan = new Runnable() {
        @Override
        public void run() {
            // Tells android WifiManger to start wifi scan --> sends broadcast when finished
            wifiManager.startScan();
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Get android WifiManager to capture wifi data
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        // Create and register broadcast receiver to get captured data
        wifiReceiver = new WifiReceiver();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        // Start scan for wifi data
        executor.execute(wifiScan);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop scan and unregister broadcast receiver from android
        executor.shutdown();
        unregisterReceiver(wifiReceiver);
    }

    public void registerListener(IWifiListener listener) {
        listenerList.add(listener);
    }

    public void unregisterListener(IWifiListener listener) {
        listenerList.remove(listener);
    }

    public class LocalBinder extends Binder {
        WifiService getService() {
            return WifiService.this;
        }
    }

    public class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            for (IWifiListener listener : listenerList) {
                listener.onWifiUpdate(wifiManager.getScanResults());
            }
            executor.execute(wifiScan);
        }
    }
}