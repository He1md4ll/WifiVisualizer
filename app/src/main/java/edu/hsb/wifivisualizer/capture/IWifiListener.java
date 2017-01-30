package edu.hsb.wifivisualizer.capture;

import android.net.wifi.ScanResult;

import java.util.List;

/**
 * Interface to provide callback for wifi data update
 */
public interface IWifiListener {
    void onWifiUpdate(List<ScanResult> resultList);
}