package edu.hsb.wifivisualizer.map;

import android.location.Location;
import android.support.annotation.NonNull;
import android.view.View;

import org.greenrobot.greendao.annotation.NotNull;

import java.util.List;

import bolts.Task;
import edu.hsb.wifivisualizer.model.Isoline;
import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.Triangle;

public interface IMapService {
    void initMap(View wrapper);
    void centerOnLocation(@NonNull Location location);
    void recalculate();
    void drawMarker(@NonNull Point point);
    void drawTriangle(@NonNull Triangle triangle);
    void drawIsoline(@NotNull final Isoline isoline, final Integer color);
    Task<Void> drawHeatmap(@NotNull List<Isoline> isolineList, List<Integer> colorList);
}
