package edu.hsb.wifivisualizer;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import edu.hsb.wifivisualizer.model.WifiInfo;

public class PointUtils {

    public static int calculateAverageStrength(List<WifiInfo> wifiInfoList) {
        int result = 0;
        if (wifiInfoList != null && !wifiInfoList.isEmpty()) {
            for (WifiInfo wifiInfo : wifiInfoList) {
                result += wifiInfo.getStrength();
            }
            result = result / wifiInfoList.size();
        }
        return result;
    }

    public static LatLng findUpperLeftPoint(List<LatLng> latLngList) {
        LatLng top = latLngList.get(0);
        for(int i = 1; i < latLngList.size(); i++) {
            final LatLng temp = latLngList.get(i);
            if(temp.longitude > top.longitude || (temp.longitude == top.longitude && temp.latitude < top.latitude)) {
                top = temp;
            }
        }
        return top;
    }
}