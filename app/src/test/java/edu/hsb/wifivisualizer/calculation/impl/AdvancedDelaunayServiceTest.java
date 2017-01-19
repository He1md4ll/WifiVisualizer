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
    }
}