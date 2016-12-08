package edu.hsb.wifivisualizer.calculation;

import java.util.List;

import edu.hsb.wifivisualizer.model.Triangle;

public interface IIsoService {
    List<Triangle> extractIsolines(List<Triangle> triangleList);
}
