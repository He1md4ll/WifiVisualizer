package edu.hsb.wifivisualizer.calculation.impl;

import com.google.common.collect.Lists;

import java.util.List;

import edu.hsb.wifivisualizer.calculation.IDelaunayService;
import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.Triangle;

// TODO: Add implementation for delaunay triangulation
public class SimpleDelauneyService implements IDelaunayService {

    @Override
    public List<Triangle> calculate(List<Point> triangleList) {
        return Lists.newArrayList();
    }
}