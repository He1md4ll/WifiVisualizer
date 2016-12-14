package edu.hsb.wifivisualizer.capture;

import android.net.wifi.ScanResult;

import java.util.List;

public interface IWifiListener {
    void onWifiUpdate(List<ScanResult> resultList);
}