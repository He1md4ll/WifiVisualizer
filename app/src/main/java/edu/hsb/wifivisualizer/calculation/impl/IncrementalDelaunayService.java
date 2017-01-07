package edu.hsb.wifivisualizer.calculation.impl;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

import edu.hsb.wifivisualizer.calculation.IDelaunayService;
import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.Triangle;
import io.github.jdiemke.triangulation.DelaunayTriangulator;
import io.github.jdiemke.triangulation.NotEnoughPointsException;
import io.github.jdiemke.triangulation.Triangle2D;
import io.github.jdiemke.triangulation.Vector2D;

public class IncrementalDelaunayService implements IDelaunayService {

    @Override
    public List<Triangle> calculate(List<Point> triangleList) {
        final Map<Vector2D, Point> vector2DPointMap = Maps.newLinkedHashMap();
        final List<Triangle> result = Lists.newArrayList();
        try {
            final List<Vector2D> vector2DList = Lists.newArrayList(Lists.transform(triangleList, new Function<Point, Vector2D>() {
                @Override
                public Vector2D apply(Point input) {
                    final Vector2D vector2D = new Vector2D(input.getPosition().latitude, input.getPosition().longitude);
                    vector2DPointMap.put(vector2D, input);
                    return vector2D;
                }
            }));
            final DelaunayTriangulator triangulator = new DelaunayTriangulator(vector2DList);
            triangulator.shuffle();
            triangulator.triangulate();
            result.addAll(Lists.transform(triangulator.getTriangles(), new Function<Triangle2D, Triangle>() {
                @Override
                public Triangle apply(Triangle2D input) {
                    final Point a = vector2DPointMap.get(input.a);
                    final Point b = vector2DPointMap.get(input.b);
                    final Point c = vector2DPointMap.get(input.c);
                    return new Triangle(Lists.newArrayList(a, b, c));
                }
            }));
        } catch (NotEnoughPointsException ignored) {}
        return result;
    }
}
