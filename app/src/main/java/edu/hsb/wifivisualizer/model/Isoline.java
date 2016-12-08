package edu.hsb.wifivisualizer.model;

import java.util.List;

public class Isoline {
    private List<Point> isoPointList;

    public Isoline(List<Point> isoPointList) {
        this.isoPointList = isoPointList;
    }

    public List<Point> getIsoPointList() {
        return isoPointList;
    }
}
