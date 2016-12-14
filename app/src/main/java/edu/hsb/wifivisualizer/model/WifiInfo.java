package edu.hsb.wifivisualizer.model;

import android.net.wifi.ScanResult;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;
import org.greenrobot.greendao.annotation.NotNull;

@Entity
public class WifiInfo {
    @Id
    private Long id;
    @NotNull
    private Long pointId;
    @NotNull
    private String ssid;
    @NotNull
    private Integer strength;
    @Generated(hash = 1258071893)
    public WifiInfo(Long id, @NotNull Long pointId, @NotNull String ssid,
            @NotNull Integer strength) {
        this.id = id;
        this.pointId = pointId;
        this.ssid = ssid;
        this.strength = strength;
    }
    @Generated(hash = 1003716208)
    public WifiInfo() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getPointId() {
        return this.pointId;
    }
    public void setPointId(Long pointId) {
        this.pointId = pointId;
    }
    public String getSsid() {
        return this.ssid;
    }
    public void setSsid(String ssid) {
        this.ssid = ssid;
    }
    public Integer getStrength() {
        return this.strength;
    }
    public void setStrength(Integer strength) {
        this.strength = strength;
    }

    @Keep
    public static WifiInfo fromScanResult(ScanResult scanResult) {
        final WifiInfo result = new WifiInfo();
        result.setSsid(scanResult.SSID);
        result.setStrength(scanResult.level);
        return result;
    }
}
