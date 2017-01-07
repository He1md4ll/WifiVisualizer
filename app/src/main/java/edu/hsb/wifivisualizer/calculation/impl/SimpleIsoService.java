package edu.hsb.wifivisualizer.calculation.impl;

import com.google.android.gms.maps.model.LatLng;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;

import edu.hsb.wifivisualizer.calculation.IIsoService;
import edu.hsb.wifivisualizer.model.Isoline;
import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.Triangle;
import edu.hsb.wifivisualizer.model.WifiInfo;

/**
 * For index reference and algorithm look here: https://en.wikipedia.org/wiki/Marching_squares#Meandering_triangles
 */
public class SimpleIsoService implements IIsoService {

    private static final int MAX_SIGNAL_STRENGTH = 0;
    private static final int MIN_SIGNAL_STRENGTH = -100;

    @Override
    public List<Isoline> extractIsolines(List<Triangle> triangleList, List<Integer> isoValues, String ssid) {
        final List<Isoline> isolineList = Lists.newArrayList();

        for (int isoValue : isoValues){
            Preconditions.checkArgument(isoValue >= MIN_SIGNAL_STRENGTH && isoValue <= MAX_SIGNAL_STRENGTH);

            final List<Isoline.Intersection> intersectionList = Lists.newArrayList();
            for (Triangle triangle : triangleList){
                final Isoline.Intersection intersection = new Isoline.Intersection();
                final List<LatLng> correspondingPointList = Lists.newArrayList();
                final Point p1 = triangle.getDefiningPointList().get(0);
                final Point p2 = triangle.getDefiningPointList().get(1);
                final Point p3 = triangle.getDefiningPointList().get(2);

                int strengthP1 = extractStrength(ssid, p1);
                int strengthP2 = extractStrength(ssid, p2);
                int strengthP3 = extractStrength(ssid, p3);

                //calculate index of triangle
                byte index = 0;
                if (strengthP1 > isoValue){
                    index += 0b0100;
                    correspondingPointList.add(p1.getPosition());
                }
                if (strengthP2 > isoValue){
                    index += 0b0010;
                    correspondingPointList.add(p2.getPosition());
                }
                if (strengthP3 > isoValue){
                    index += 0b0001;
                    correspondingPointList.add(p3.getPosition());
                }

                if (index == 0b0011 || index == 0b0101 || index == 0b0100 || index == 0b0010){
                    // interpolate point right side
                    final LatLng latLng = interpolateLinear(p1, p2, strengthP1, strengthP2, isoValue);
                    intersection.setIntersectionPoint(latLng);
                }
                if (index == 0b0101 || index == 0b0110 || index == 0b0010 || index == 0b0001){
                    // interpolate point bottom side
                    LatLng latLng = interpolateLinear(p2, p3, strengthP2, strengthP3, isoValue);
                    intersection.setIntersectionPoint(latLng);
                }
                if (index == 0b0011 || index == 0b0110 || index == 0b0100 || index == 0b0001) {
                    // interpolate point left side
                    LatLng latLng = interpolateLinear(p3, p1, strengthP3, strengthP1, isoValue);
                    intersection.setIntersectionPoint(latLng);
                }

                // No corresponding point = triangle outside of iso area limited by iso line
                if (!correspondingPointList.isEmpty()) {
                    intersection.setCorrespondingPointList(correspondingPointList);
                    intersectionList.add(intersection);
                }
            }
            Isoline isoline = new Isoline(intersectionList);
            isolineList.add(isoline);
        }
        return isolineList;
    }

    private int extractStrength(final String ssid, final Point point) {
        if (ssid != null && !ssid.isEmpty()) {
            final Optional<WifiInfo> wifiInfoOptional = Iterables.tryFind(point.getSignalStrength(), new Predicate<WifiInfo>() {
                @Override
                public boolean apply(WifiInfo input) {
                    return ssid.equals(input.getSsid());
                }
            });
            if (wifiInfoOptional.isPresent()) {
                return wifiInfoOptional.get().getStrength();
            } else {
                return MIN_SIGNAL_STRENGTH;
            }
        }
        return Optional.fromNullable(point.getAverageStrength()).or(MIN_SIGNAL_STRENGTH);
    }

    private LatLng interpolateLinear(Point p1, Point p2, int strengthP1, int strengthP2, int value){
        double p1Lat = p1.getPosition().latitude;
        double p1Lon = p1.getPosition().longitude;

        double p2Lat = p2.getPosition().latitude;
        double p2Lon = p2.getPosition().longitude;

        double latitude = p1Lat + (p2Lat - p1Lat) *  (value - strengthP1) / (strengthP2 - strengthP1);
        double longitude = p1Lon + (p2Lon - p1Lon) *  (value - strengthP1) / (strengthP2 - strengthP1);

        return new LatLng(latitude, longitude);
    }
}