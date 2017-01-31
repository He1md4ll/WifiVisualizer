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
 * Service to calculate isolines from a given list of triangles. The calculation uses the
 * marching triangles algorithm (marching cubes/squares for triangles). The indices used in
 * the calculation are identical to the example in Wikipedia (see
 * https://en.wikipedia.org/wiki/Marching_squares#Meandering_triangles ).
 */
public class SimpleIsoService implements IIsoService {

    private static final int MAX_SIGNAL_STRENGTH = 0;
    private static final int MIN_SIGNAL_STRENGTH = -100;

    @Override
    /**
     * Method to calculate isolines
     * @param triangleList all triangles the algorithm is marching through
     * @param isoValues all isovalue which are to be drawn
     * @param ssid name of ssid to filter for specific networks
     * @return List of all calculated Isolines
     */
    public List<Isoline> extractIsolines(List<Triangle> triangleList, List<Integer> isoValues, String ssid) {
        final List<Isoline> isolineList = Lists.newArrayList();

        for (int isoValue : isoValues){
            //Check if all measured values are between max and min value
            Preconditions.checkArgument(isoValue >= MIN_SIGNAL_STRENGTH && isoValue <= MAX_SIGNAL_STRENGTH);

            final List<Isoline.Intersection> intersectionList = Lists.newArrayList();

            //loop/"marching" through every triangle in list
            for (Triangle triangle : triangleList){
                final Isoline.Intersection intersection = new Isoline.Intersection();
                final List<LatLng> correspondingPointList = Lists.newArrayList();

                //get points of triangle
                final Point p1 = triangle.getDefiningPointList().get(0);
                final Point p2 = triangle.getDefiningPointList().get(1);
                final Point p3 = triangle.getDefiningPointList().get(2);

                //get the signal strength at every point of triangle
                int strengthP1 = extractStrength(ssid, p1);
                int strengthP2 = extractStrength(ssid, p2);
                int strengthP3 = extractStrength(ssid, p3);

                //calculate index of triangle depending on the value of each triangle corner
                //the index is binary format
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

                //check if index is in the set of four where the right side has an intersection
                if (index == 0b0011 || index == 0b0101 || index == 0b0100 || index == 0b0010){
                    // interpolate point right side
                    final LatLng latLng = interpolateLinear(p1, p2, strengthP1, strengthP2, isoValue);
                    intersection.setIntersectionPoint(latLng);
                }
                //check if index is in the set of four where the bottom side has an intersection
                if (index == 0b0101 || index == 0b0110 || index == 0b0010 || index == 0b0001){
                    // interpolate point bottom side
                    LatLng latLng = interpolateLinear(p2, p3, strengthP2, strengthP3, isoValue);
                    intersection.setIntersectionPoint(latLng);
                }
                //check if index is in the set of four where the left side has an intersection
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
            Isoline isoline = new Isoline(intersectionList, isoValue);
            isolineList.add(isoline);
        }
        return isolineList;
    }

    /**
     * Methode to extract wifi signal strength from specific network sid at a given point
     * @param ssid name of network
     * @param point measured point
     * @return signal strength in -db (int)
     */
    private int extractStrength(final String ssid, final Point point) {
        //check if ssid exists otherwise return average/minimum value
        if (ssid != null && !ssid.isEmpty()) {
            final Optional<WifiInfo> wifiInfoOptional = Iterables.tryFind(point.getSignalStrength(), new Predicate<WifiInfo>() {
                @Override
                public boolean apply(WifiInfo input) {
                    return ssid.equals(input.getSsid());
                }
            });
            if (wifiInfoOptional.isPresent()) {
                //return signal strength if it is present
                return wifiInfoOptional.get().getStrength();
            } else {
                //otherwise return minimum value
                return MIN_SIGNAL_STRENGTH;
            }
        }
        return Optional.fromNullable(point.getAverageStrength()).or(MIN_SIGNAL_STRENGTH);
    }

    /**
     * Methode to interpolate a value linear between to givien points (position + value)
     * @param p1 first point
     * @param p2 second point
     * @param strengthP1 signal strength at first point
     * @param strengthP2 signal strength at second point
     * @param value value to be interpolated
     * @return position of interpolated point
     */
    private LatLng interpolateLinear(Point p1, Point p2, int strengthP1, int strengthP2, int value){

        //get position of two points
        double p1Lat = p1.getPosition().latitude;
        double p1Lon = p1.getPosition().longitude;
        double p2Lat = p2.getPosition().latitude;
        double p2Lon = p2.getPosition().longitude;

        //calculate latitude linear
        double latitude = p1Lat + (p2Lat - p1Lat) *  (value - strengthP1) / (strengthP2 - strengthP1);
        //calculare longitude linear
        double longitude = p1Lon + (p2Lon - p1Lon) *  (value - strengthP1) / (strengthP2 - strengthP1);

        return new LatLng(latitude, longitude);
    }
}