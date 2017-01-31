package edu.hsb.wifivisualizer.calculation.impl;

import com.google.android.gms.maps.model.LatLng;
import com.google.common.collect.Lists;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;

import java.util.List;

import edu.hsb.wifivisualizer.calculation.IDelaunayService;
import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.Triangle;

/**
 * Service to calculate Delauney triangles from a given list of points.
 * This implementation uses the brute force methode and is too slow. See
 * implementation of Advanced/IncrementalDelaunayService.
 */
public class SimpleDelauneyService implements IDelaunayService {

    /**
     * Methode to calculate Delaunay triangles form list of points
     * @param pointList list of points which need to be triangulated
     * @return list of Delaunay triangles
     */
    @Override
    public List<Triangle> calculate(List<Point> pointList) {
        final List<Triangle> result = Lists.newArrayList();

        final int size = pointList.size();
        //iterate over every possible triangles combination
        for (int i = 0; i < size; i++) {
            for (int j = i+1; j < size; j++) {
                for (int k = j+1; k < size; k++) {
                    boolean isTriangle = true;
                    for (int a = 0; a < size; a++) {
                        //skip this combination where every point is the same
                        if (a == i || a == j || a == k) continue;

                        //check if the fourth point is inside the delaunay circle
                        final boolean isAInsideTriangleIJK = isInsideDelaunayCircle(Lists.newArrayList(
                                toVector2D(pointList.get(i)),
                                toVector2D(pointList.get(j)),
                                toVector2D(pointList.get(k)),
                                toVector2D(pointList.get(a))));

                        //if point is inside the circle, try next combination
                        if (isAInsideTriangleIJK) {
                            isTriangle = false;
                            break;
                        }
                    }
                    //if there is no point inside the circle, save triangle
                    if (isTriangle) {
                        result.add(new Triangle(Lists.newArrayList(pointList.get(i), pointList.get(j), pointList.get(k))));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Methode to check if a point is inside the Delaunay circle of three other points.
     * Calculation is done clockwise
     * @param vectorList
     * @return true if point is in circle, otherwise false
     */
    private boolean isInsideDelaunayCircle(List<Vector2D> vectorList) {
        if      (isClockWorkWise(vectorList) > 0) return (isInTriangle(vectorList) > 0);
        else if (isClockWorkWise(vectorList) < 0) return (isInTriangle(vectorList) < 0);
        return true;
    }

    /**
     * Methode to check if points are in clockwise or anticlockwise order
     * @param vectorList list of points
     * @return true if clockwise, false if anticlockwise
     */
    private int isClockWorkWise(List<Vector2D> vectorList) {
        double area = triangleArea(vectorList);
        if      (area > 0) return +1;
        else if (area < 0) return -1;
        else               return  0;
    }

    /**
     * methode to calculare area of triangle with cross product
     * @param vectorList triangle points
     * @return area of triangle
     */
    private double triangleArea(List<Vector2D> vectorList) {
        return 0.5 * vectorList.get(0).crossProduct(vectorList.get(1), vectorList.get(2));
    }

    /**
     * methode to check if a point is inside a triangle
     * @param vectorList first three points for triangle and fourth point to be tested
     * @return value above or below 0 decide if point is inside/outsie
     */
    private double isInTriangle(List<Vector2D> vectorList) {
        int rows = 4;
        final Array2DRowRealMatrix array2DRowRealMatrix = new Array2DRowRealMatrix(4, 4);
        for (int i = 0; i < rows; i++) {
            final Vector2D vec = vectorList.get(i);
            array2DRowRealMatrix.addToEntry(i, 0, vec.getX());
            array2DRowRealMatrix.addToEntry(i, 1, vec.getY());
            array2DRowRealMatrix.addToEntry(i, 2, vec.getX() * vec.getX() + vec.getY() * vec.getY());
            array2DRowRealMatrix.addToEntry(i, 3, 1);
        }
        return new LUDecomposition(array2DRowRealMatrix).getDeterminant();
    }

    private Vector2D toVector2D(Point point) {
        final LatLng position = point.getPosition();
        return new Vector2D(position.latitude, position.longitude);
    }
}