package edu.hsb.wifivisualizer.model;


import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import org.greenrobot.greendao.annotation.Convert;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.converter.PropertyConverter;


@Entity
public class Point {

    @Id
    private Long id;
    @NotNull
    @Convert(columnType = String.class, converter = LatLngConverter.class)
    private LatLng position;
    @NotNull
    private Integer signalStrength;

    @Generated(hash = 263891712)
    public Point(Long id, @NotNull LatLng position, @NotNull Integer signalStrength) {
        this.id = id;
        this.position = position;
        this.signalStrength = signalStrength;
    }

    @Generated(hash = 1977038299)
    public Point() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LatLng getPosition() {
        return this.position;
    }

    public void setPosition(LatLng position) {
        this.position = position;
    }

    public Integer getSignalStrength() {
        return this.signalStrength;
    }

    public void setSignalStrength(Integer signalStrength) {
        this.signalStrength = signalStrength;
    }

    @Keep
    public static class LatLngConverter implements PropertyConverter<LatLng, String> {

        @Override
        public LatLng convertToEntityProperty(String databaseValue) {
            return new Gson().fromJson(databaseValue, LatLng.class);
        }

        @Override
        public String convertToDatabaseValue(LatLng entityProperty) {
            return new Gson().toJson(entityProperty, LatLng.class);
        }
    }
}