package edu.hsb.wifivisualizer.calculation;

import java.util.List;

import edu.hsb.wifivisualizer.model.Isoline;
import edu.hsb.wifivisualizer.model.Triangle;

public interface IIsoService {
    List<Isoline> extractIsolines(List<Triangle> triangleList, List<Integer> isoValues);
}
