package edu.hsb.wifivisualizer.map;

import android.location.Location;
import android.support.annotation.NonNull;
import android.view.View;

public interface IMapService {
    void initMap(View wrapper);
    void centerOnLocation(@NonNull Location location);
}
