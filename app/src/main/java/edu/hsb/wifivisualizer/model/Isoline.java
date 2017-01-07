package edu.hsb.wifivisualizer.model;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class Isoline {
    private List<Intersection> intersectionList;

    public Isoline(List<Intersection> intersectionList) {
        this.intersectionList = intersectionList;
    }

    public List<Intersection> getIntersectionList() {
        return intersectionList;
    }

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
