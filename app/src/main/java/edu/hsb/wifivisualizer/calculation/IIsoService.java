package edu.hsb.wifivisualizer.calculation;

import java.util.List;

import edu.hsb.wifivisualizer.model.Isoline;
import edu.hsb.wifivisualizer.model.Triangle;

/**
 * Interface to Provide isoline extraction of a given list of triangles
 */
public interface IIsoService {
    List<Isoline> extractIsolines(List<Triangle> triangleList, List<Integer> isoValues, String ssid);
}
