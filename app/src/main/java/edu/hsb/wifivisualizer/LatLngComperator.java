package edu.hsb.wifivisualizer;

import com.google.android.gms.maps.model.LatLng;

import java.util.Comparator;

public class LatLngComperator implements Comparator<LatLng> {

    private LatLng upper;

    public LatLngComperator(LatLng upper) {
        this.upper = upper;
    }

    @Override
    public int compare(LatLng o1, LatLng o2) {
        // Exclude the 'upper' point from the sort (which should come first).
        if(o1 == upper) return -1;
        if(o2 == upper) return 1;

        // Find the slopes of 'p1' and 'p2' when a line is
        // drawn from those points through the 'upper' point.
        double m1 = slope(o1);
        double m2 = slope(o2);

        // 'p1' and 'p2' are on the same line towards 'upper'.
        if(m1 == m2) {
            // The point closest to 'upper' will come first.
            return distance(o1) < distance(o2) ? -1 : 1;
        }

        // If 'p1' is to the right of 'upper' and 'p2' is the the left.
        if(m1 <= 0 && m2 > 0) return -1;

        // If 'p1' is to the left of 'upper' and 'p2' is the the right.
        if(m1 > 0 && m2 <= 0) return 1;

        // It seems that both slopes are either positive, or negative.
        return m1 > m2 ? -1 : 1;
    }

    private double distance(LatLng that) {
        double dX = that.latitude - this.upper.latitude;
        double dY = that.longitude - this.upper.longitude;
        return Math.sqrt((dX*dX) + (dY*dY));
    }

    private double slope(LatLng that) {
        double dX = that.latitude - this.upper.latitude;
        double dY = that.longitude - this.upper.longitude;
        return dY / dX;
    }
}