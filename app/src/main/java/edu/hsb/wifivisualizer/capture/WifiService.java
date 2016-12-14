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

public class WifiService extends Service {

    private final IBinder binder = new LocalBinder();
    private WifiManager wifiManager;
    private WifiReceiver wifiReceiver;
    private ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    private List<IWifiListener> listenerList = Lists.newArrayList();
    private Runnable wifiScan = new Runnable() {
        @Override
        public void run() {
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
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        wifiReceiver = new WifiReceiver();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        executor.execute(wifiScan);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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