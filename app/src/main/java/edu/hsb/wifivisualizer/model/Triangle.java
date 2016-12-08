package edu.hsb.wifivisualizer.model;

import java.util.List;

public class Triangle {
    private List<Point> definingPointList;

    public Triangle(List<Point> definingPointList) {
        this.definingPointList = definingPointList;
    }

    public List<Point> getDefiningPointList() {
        return definingPointList;
    }
}