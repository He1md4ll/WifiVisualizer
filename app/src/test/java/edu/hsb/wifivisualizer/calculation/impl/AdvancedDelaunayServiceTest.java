package edu.hsb.wifivisualizer.calculation.impl;

import com.google.android.gms.maps.model.LatLng;
import com.google.common.collect.Lists;


import org.junit.Before;
import org.junit.Test;

import java.util.List;

import edu.hsb.wifivisualizer.calculation.IDelaunayService;
import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.Triangle;

import static org.junit.Assert.assertNull;

public class AdvancedDelaunayServiceTest {

    private IDelaunayService delaunayService;

    @Before
    public void setUp() {
        delaunayService = new AdvancedDelaunayService();
    }

    @Test
    public void calculate() throws Exception {
        // Given
        final List<Point> testList = Lists.newArrayList();
        testList.add(new Point(null, new LatLng(0,0), 0));
        testList.add(new Point(null, new LatLng(1,1), 1));
        testList.add(new Point(null, new LatLng(2,2), 2));


        for(Point p : testList) {
            if(testList.contains(p)) {
                //find triangle where p is in it
                if(PointInTriangle(p, testList.get(0), testList.get(1), testList.get(2))) {
                    testList.add(p);
                   //neues Dreieck
                }
            }
        }

        /*
        boolean isTriangle = true;
        //3 points w new point
        for(int i = 0; i < testList.size(); i++) {
            for(int j = i+1; j  < testList.size(); j++) {
                for(int k = j+1; k < testList.size(); k++) {
                    //for new point
                    for(int a = 0; a < testList.size(); a++) {
                        if(!(a==i || a==j || a==k)) break;
                        if(testList.get(a).getPosition() == current pos) {
                            isTriangle = false;
                            break;
                        }

                    }
                    //draw triangle
                    if(isTriangle) {

                    }
                }
            }
        }
        */

        // When
        List<Triangle> result = delaunayService.calculate(testList);

        // Then
        assertNull(result);
    }


    //how to get the x, y position??
    private float sign(Point p1, Point p2, Point p3) {
        return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y);
    }

    private boolean PointInTriangle(Point pr, Point p1, Point p2, Point p3) {
        boolean b1, b2, b3;
        b1 = sign(pr, p1, p2) < 0;
        b2 = sign(pr, p2, p3) < 0;
        b3 = sign(pr, p3, p1) < 0;

        return (b1 == b2) && (b2 == b3);
    }
}