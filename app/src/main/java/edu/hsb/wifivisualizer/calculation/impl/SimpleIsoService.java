package edu.hsb.wifivisualizer.calculation.impl;

import com.google.android.gms.maps.model.LatLng;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

import edu.hsb.wifivisualizer.calculation.IIsoService;
import edu.hsb.wifivisualizer.model.Isoline;
import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.Triangle;

// TODO: Add implementation for isoline extraction

/**
 * For index reference and algorithm look here: https://en.wikipedia.org/wiki/Marching_squares#Meandering_triangles
 */
public class SimpleIsoService implements IIsoService {

    @Override
    public List<Isoline> extractIsolines(List<Triangle> triangleList, List<Integer> isoValues) {
        List<Isoline> isolines = new ArrayList<Isoline>();


        for (int isoValue : isoValues){
            List<Point> isolinePoints = new ArrayList<Point>();
            for (Triangle triangle : triangleList){
                Point p1 = triangle.getDefiningPointList().get(0);
                Point p2 = triangle.getDefiningPointList().get(1);
                Point p3 = triangle.getDefiningPointList().get(2);

                //calculate index of triangle
                byte index = 0;
                if (p1.getSignalStrength().get(0).getStrength() > isoValue){
                    index += 0b0100;
                }
                if (p2.getSignalStrength().get(0).getStrength() > isoValue){
                    index += 0b0010;
                }
                if (p3.getSignalStrength().get(0).getStrength() > isoValue){
                    index += 0b0001;
                }

                if (index == 0b0011 || index == 0b0101 || index == 0b0100 || index == 0b0010){
                    // interpolate point right side
                    LatLng latLng = interpolateLinear(p1, p2, isoValue);

                    Point intersection = new Point();
                    intersection.setPosition(latLng);
                    isolinePoints.add(intersection);
                }
                if (index == 0b0101 || index == 0b0110 || index == 0b0010 || index == 0b0001){
                    // interpolate point bottom side
                    LatLng latLng = interpolateLinear(p2, p3, isoValue);

                    Point intersection = new Point();
                    intersection.setPosition(latLng);
                    isolinePoints.add(intersection);
                }
                if (index == 0b0011 || index == 0b0110 || index == 0b0100 || index == 0b0001) {
                    // interpolate point left side
                    LatLng latLng = interpolateLinear(p3, p1, isoValue);

                    Point intersection = new Point();
                    intersection.setPosition(latLng);
                    isolinePoints.add(intersection);
                }

            }
            Isoline isoline = new Isoline(isolinePoints);
            isolines.add(isoline);
        }


        return isolines;
    }

    private LatLng interpolateLinear(Point p1, Point p2, int value){
        double latitude = 0;
        double longitude = 0;

        double p1Lat = p1.getPosition().latitude;
        double p1Lon = p1.getPosition().longitude;
        double p1Stength = p1.getSignalStrength().get(0).getStrength();

        double p2Lat = p2.getPosition().latitude;
        double p2Lon = p2.getPosition().longitude;
        double p2Stength = p2.getSignalStrength().get(0).getStrength();


        latitude = p1Lat + (p2Lat - p1Lat) *  (value-p1Stength) / (p2Stength-p1Stength);
        longitude = p1Lon + (p2Lon - p1Lon) *  (value-p1Stength) / (p2Stength-p1Stength);

        return new LatLng(latitude, longitude);
    }

}