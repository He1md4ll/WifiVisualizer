package edu.hsb.wifivisualizer.calculation.impl;

import com.google.android.gms.maps.model.LatLng;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import java.util.List;

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
        List<Triangle> result = Lists.newArrayList();
        try {
            final List<Vector2D> vector2DList = Lists.newArrayList(Lists.transform(triangleList, new Function<Point, Vector2D>() {
                @Override
                public Vector2D apply(Point input) {
                    return new Vector2D(input.getPosition().latitude, input.getPosition().longitude);
                }
            }));
            final DelaunayTriangulator triangulator = new DelaunayTriangulator(vector2DList);
            triangulator.shuffle();
            triangulator.triangulate();
            result.addAll(Lists.transform(triangulator.getTriangles(), new Function<Triangle2D, Triangle>() {
                @Override
                public Triangle apply(Triangle2D input) {
                    final Point a = new Point(null, new LatLng(input.a.x, input.a.y));
                    final Point b = new Point(null, new LatLng(input.b.x, input.b.y));
                    final Point c = new Point(null, new LatLng(input.c.x, input.c.y));
                    return new Triangle(Lists.newArrayList(a, b, c));
                }
            }));
        } catch (NotEnoughPointsException ignored) {}
        return result;
    }
}
