package edu.hsb.wifivisualizer.model;

import java.util.List;

/**
 * Data structure to hold triangle points
 * One triangle consists of three defining points
 */
public class Triangle {
    private List<Point> definingPointList;

    public Triangle(List<Point> definingPointList) {
        this.definingPointList = definingPointList;
    }

    public List<Point> getDefiningPointList() {
        return definingPointList;
    }
}