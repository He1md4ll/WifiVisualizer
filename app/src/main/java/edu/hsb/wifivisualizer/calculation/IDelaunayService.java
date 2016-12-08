package edu.hsb.wifivisualizer.calculation;

import java.util.List;

import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.Triangle;

public interface IDelaunayService {
    List<Triangle> calculate(List<Point> triangleList);
}
