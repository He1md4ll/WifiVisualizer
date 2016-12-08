package edu.hsb.wifivisualizer.calculation.impl;

import com.google.common.collect.Lists;

import java.util.List;

import edu.hsb.wifivisualizer.calculation.IDelaunayService;
import edu.hsb.wifivisualizer.model.Isoline;
import edu.hsb.wifivisualizer.model.Triangle;

// TODO: Add implementation for delaunay triangulation
public class SimpleDelauneyService implements IDelaunayService {

    @Override
    public List<Isoline> calculate(List<Triangle> triangleList) {
        return Lists.newArrayList();
    }
}