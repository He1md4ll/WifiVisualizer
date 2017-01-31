package edu.hsb.wifivisualizer.calculation.impl;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;

import java.util.List;
import java.util.Map;

import edu.hsb.wifivisualizer.calculation.IDelaunayService;
import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.Triangle;

/**
 * Service to calculate Delauney triangles from a given list of points.
 * This implementation uses the incemental version of the algorithm provided by external library
 * 'com.vividsolutions:jts:1.13' library (implementation of Java Topology Suite)
 */
public class IncrementalDelaunayService implements IDelaunayService {

    /**
     * Methode to calculate Delaunay triangles form list of points
     * @param triangleList list of points which need to be triangulated
     * @return list of Delaunay triangles
     */
    @Override
    public List<Triangle> calculate(List<Point> triangleList) {
        final Map<Coordinate, Point> coordinatePointMap = Maps.newLinkedHashMap();
        final List<Triangle> result = Lists.newArrayList();

        final List<Coordinate> coordinateList = Lists.transform(triangleList, new Function<Point, Coordinate>() {
            @Override
            public Coordinate apply(Point input) {
                final Coordinate coordinate = new Coordinate(input.getPosition().latitude, input.getPosition().longitude);
                coordinatePointMap.put(coordinate, input);
                return coordinate;
            }
        });

        //create DelaunayTriangleBuilder
        final DelaunayTriangulationBuilder delaunayBuilder = new DelaunayTriangulationBuilder();
        delaunayBuilder.setSites(coordinateList);

        //use builder to calculate triangles
        final Geometry triangles = delaunayBuilder.getTriangles(new GeometryFactory());

        //convert Geometry data structure to list of triangles
        for (int i = 0; i < triangles.getNumGeometries(); i++) {
            final List<Point> trianglePointList = Lists.newArrayList();
            final Triangle triangle = new Triangle(trianglePointList);

            LinearRing triangleRing = (LinearRing) triangles.getGeometryN(i).getBoundary();
            for (int j = 0; j < 3; j++) {
                trianglePointList.add(coordinatePointMap.get(triangleRing.getPointN(j).getCoordinate()));
            }
            result.add(triangle);
        }
        return result;
    }
}