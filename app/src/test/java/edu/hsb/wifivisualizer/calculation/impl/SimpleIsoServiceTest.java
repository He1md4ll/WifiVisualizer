package edu.hsb.wifivisualizer.calculation.impl;

import com.google.android.gms.maps.model.LatLng;
import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import edu.hsb.wifivisualizer.calculation.IIsoService;
import edu.hsb.wifivisualizer.model.Isoline;
import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.Triangle;

import static org.junit.Assert.assertEquals;

public class SimpleIsoServiceTest {

    private IIsoService isoService;

    @Before
    public void setUp() {
        isoService = new SimpleIsoService();
    }

    @Test
    public void testOneTriangle() throws Exception {
        // Given
        List<Point> trianglePoints = Lists.newArrayList();
        Point p1 = new Point();
        p1.setPosition(new LatLng(1, 1));
        p1.setAverageStrength(10);
        trianglePoints.add(p1);

        Point p2 = new Point();
        p2.setPosition(new LatLng(3, 1));
        p2.setAverageStrength(20);
        trianglePoints.add(p2);

        Point p3 = new Point();
        p3.setPosition(new LatLng(2, 2));
        p3.setAverageStrength(30);
        trianglePoints.add(p3);

        List<Triangle> testTriangles = Lists.newArrayList(new Triangle((trianglePoints)));
        List<Integer> isoValues = Lists.newArrayList(15);

        // When
        List<Isoline> result = isoService.extractIsolines(testTriangles, isoValues, null);

        // Then
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getIntersectionList().size());
    }

    @Test
    public void testTwoTriangles() throws Exception {
        // Given
        List<Point> trianglePoints = Lists.newArrayList();
        Point p1 = new Point();
        p1.setPosition(new LatLng(1, 1));
        p1.setAverageStrength(10);
        trianglePoints.add(p1);

        Point p2 = new Point();
        p2.setPosition(new LatLng(3, 1));
        p2.setAverageStrength(20);
        trianglePoints.add(p2);

        Point p3 = new Point();
        p3.setPosition(new LatLng(2, 2));
        p3.setAverageStrength(30);
        trianglePoints.add(p3);

        Point p4 = new Point();
        p4.setPosition(new LatLng(-2, -2));
        p4.setAverageStrength(40);

        List<Triangle> testTriangles = Lists.newArrayList(new Triangle((trianglePoints)));
        List<Point> triangle2Points = Lists.newArrayList(p1, p2, p4);
        testTriangles.add(new Triangle((triangle2Points)));
        List<Integer> isoValues = Lists.newArrayList(15);

        // When
        List<Isoline> result = isoService.extractIsolines(testTriangles, isoValues, null);

        // Then
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getIntersectionList().size());
    }

    @Test
    public void testNoIsoLine() throws Exception {
        // Given
        List<Point> trianglePoints = Lists.newArrayList();
        Point p1 = new Point();
        p1.setPosition(new LatLng(1, 1));
        p1.setAverageStrength(10);
        trianglePoints.add(p1);

        Point p2 = new Point();
        p2.setPosition(new LatLng(3, 1));
        p2.setAverageStrength(20);
        trianglePoints.add(p2);

        Point p3 = new Point();
        p3.setPosition(new LatLng(2, 2));
        p3.setAverageStrength(30);
        trianglePoints.add(p3);

        List<Triangle> testTriangles = Lists.newArrayList(new Triangle((trianglePoints)));
        List<Integer> isoValues =Lists.newArrayList(40);

        // When
        List<Isoline> result = isoService.extractIsolines(testTriangles, isoValues, null);

        // Then
        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getIntersectionList().size());
    }

}