package edu.hsb.wifivisualizer.map;

import android.location.Location;

import com.google.android.gms.common.api.Status;

public interface ILocationListener {
    void onLocationChanged(Location location);
    void onResolutionNeeded(Status status);
    void onProviderUnavailable();
    void onLostConnection();
}
