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

public class SimpleDelauneyService implements IDelaunayService {

    @Override
    public List<Triangle> calculate(List<Point> pointList) {
        final List<Triangle> result = Lists.newArrayList();

        final int size = pointList.size();
        for (int i = 0; i < size; i++) {
            for (int j = i+1; j < size; j++) {
                for (int k = j+1; k < size; k++) {
                    boolean isTriangle = true;
                    for (int a = 0; a < size; a++) {
                        if (a == i || a == j || a == k) continue;
                        final boolean isAInsideTriangleIJK = isInsideDelaunayCircle(Lists.newArrayList(
                                toVector2D(pointList.get(i)),
                                toVector2D(pointList.get(j)),
                                toVector2D(pointList.get(k)),
                                toVector2D(pointList.get(a))));

                        if (isAInsideTriangleIJK) {
                            isTriangle = false;
                            break;
                        }
                    }
                    if (isTriangle) {
                        result.add(new Triangle(Lists.newArrayList(pointList.get(i), pointList.get(j), pointList.get(k))));
                    }
                }
            }
        }
        return result;
    }

    private boolean isInsideDelaunayCircle(List<Vector2D> vectorList) {
        if      (isClockWorkWise(vectorList) > 0) return (isInTriangle(vectorList) > 0);
        else if (isClockWorkWise(vectorList) < 0) return (isInTriangle(vectorList) < 0);
        return true;
    }

    private int isClockWorkWise(List<Vector2D> vectorList) {
        double area = triangleArea(vectorList);
        if      (area > 0) return +1;
        else if (area < 0) return -1;
        else               return  0;
    }

    private double triangleArea(List<Vector2D> vectorList) {
        return 0.5 * vectorList.get(0).crossProduct(vectorList.get(1), vectorList.get(2));
    }

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