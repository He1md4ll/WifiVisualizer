package edu.hsb.wifivisualizer.calculation;

import java.util.List;

import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.Triangle;

/**
 * Interface to provide calculation of Delaunay triangles from a given list of points
 */
public interface IDelaunayService {
    List<Triangle> calculate(List<Point> triangleList);
}
