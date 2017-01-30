package edu.hsb.wifivisualizer.model;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Data structure to hold isoline information
 * Isoline consists of (multiple) Intersections
 * Intersections and the related iso value define the Isoline
 */
public class Isoline {
    private List<Intersection> intersectionList;
    private int isovalue;

    public Isoline(List<Intersection> intersectionList) {
        this.intersectionList = intersectionList;
    }

    public Isoline(List<Intersection> intersectionList, int isovalue) {
        this.intersectionList = intersectionList;
        this.isovalue = isovalue;
    }

    public List<Intersection> getIntersectionList() {
        return intersectionList;
    }

    public int getIsovalue() {
        return isovalue;
    }

    public void setIsovalue(int isovalue) {
        this.isovalue = isovalue;
    }

    /**
     * Data structure to hold triangle intersection information
     * One Intersection can have 2intersection points and multiple corresponding points
     * Corresponding points are point inside the isoline border (value less than iso value)
     * Corresponding points are used to draw the heatmap
     */
    public static class Intersection {
        private LatLng intersectionPoint1;
        private LatLng intersectionPoint2;
        private List<LatLng> correspondingPointList;

        public void setIntersectionPoint(LatLng intersectionPoint) {
            if (intersectionPoint1 == null) {
                intersectionPoint1 = intersectionPoint;
            } else if (intersectionPoint2 == null) {
                intersectionPoint2 = intersectionPoint;
            }
        }

        public LatLng getIntersectionPoint1() {
            return intersectionPoint1;
        }

        public LatLng getIntersectionPoint2() {
            return intersectionPoint2;
        }

        public List<LatLng> getCorrespondingPointList() {
            return correspondingPointList;
        }

        public void setCorrespondingPointList(List<LatLng> correspondingPointList) {
            this.correspondingPointList = correspondingPointList;
        }
    }
}
