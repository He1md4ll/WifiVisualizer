package edu.hsb.wifivisualizer.map;

import android.location.Location;
import android.support.annotation.NonNull;
import android.view.View;

import org.greenrobot.greendao.annotation.NotNull;

import edu.hsb.wifivisualizer.model.Isoline;
import edu.hsb.wifivisualizer.model.Triangle;

public interface IMapService {
    void initMap(View wrapper);
    void centerOnLocation(@NonNull Location location);
    void recalculate();
    void drawTriangle(@NonNull Triangle triangle);
    void drawIsoline(@NotNull Isoline isoline);
}
