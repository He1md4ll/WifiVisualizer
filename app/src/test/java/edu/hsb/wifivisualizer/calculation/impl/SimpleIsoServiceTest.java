package edu.hsb.wifivisualizer.calculation.impl;

import com.google.android.gms.maps.model.LatLng;
import com.google.common.collect.Interner;
import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import edu.hsb.wifivisualizer.calculation.IDelaunayService;
import edu.hsb.wifivisualizer.calculation.IIsoService;
import edu.hsb.wifivisualizer.calculation.impl.SimpleIsoService;
import edu.hsb.wifivisualizer.model.Isoline;
import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.Triangle;
import edu.hsb.wifivisualizer.model.WifiInfo;

import static org.junit.Assert.*;

public class SimpleIsoServiceTest {

    private IIsoService isoService;

    @Before
    public void setUp() {
        isoService = new SimpleIsoService();
    }

    @Test
    public void calculate() throws Exception {
        // Given
        List<Point> trianglePoints = new ArrayList<Point>();
        WifiInfo wifiInfoP1 = new WifiInfo();
        wifiInfoP1.setStrength(10);
        Point p1 = new Point();
        p1.setPosition(new LatLng(1, 1));
        List<WifiInfo> wifiInfos = new ArrayList<WifiInfo>();
        wifiInfos.add(wifiInfoP1);
        p1.setSignalStrength(wifiInfos);
        trianglePoints.add(p1);

        WifiInfo wifiInfoP2 = new WifiInfo();
        wifiInfoP2.setStrength(20);
        Point p2 = new Point();
        p2.setPosition(new LatLng(3, 1));
        wifiInfos = new ArrayList<WifiInfo>();
        wifiInfos.add(wifiInfoP2);
        p2.setSignalStrength(wifiInfos);
        trianglePoints.add(p2);

        WifiInfo wifiInfoP3 = new WifiInfo();
        wifiInfoP3.setStrength(30);
        Point p3 = new Point();
        p3.setPosition(new LatLng(2, 2));
        wifiInfos = new ArrayList<WifiInfo>();
        wifiInfos.add(wifiInfoP3);
        p3.setSignalStrength(wifiInfos);
        trianglePoints.add(p3);

        List<Triangle> testTriangles = Lists.newArrayList();
        testTriangles.add(new Triangle((trianglePoints)));

        List<Integer> isoValues = new ArrayList<Integer>();
        isoValues.add(15);

        // When
        List<Isoline> result = isoService.extractIsolines(testTriangles, isoValues);

        // Then
        assertNull(result);
    }

}