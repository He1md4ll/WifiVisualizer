package edu.hsb.wifivisualizer.model;


import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import org.greenrobot.greendao.DaoException;
import org.greenrobot.greendao.annotation.Convert;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.OrderBy;
import org.greenrobot.greendao.annotation.ToMany;
import org.greenrobot.greendao.converter.PropertyConverter;

import java.util.List;

import edu.hsb.wifivisualizer.database.DaoSession;
import edu.hsb.wifivisualizer.database.PointDao;
import edu.hsb.wifivisualizer.database.WifiInfoDao;


@Entity
public class Point {

    @Id
    private Long id;
    @NotNull
    @Convert(columnType = String.class, converter = LatLngConverter.class)
    private LatLng position;
    @ToMany(referencedJoinProperty = "pointId")
    @OrderBy("ssid ASC")
    private List<WifiInfo> signalStrength;
    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /** Used for active entity operations. */
    @Generated(hash = 1980395011)
    private transient PointDao myDao;

    @Generated(hash = 1444149411)
    public Point(Long id, @NotNull LatLng position) {
        this.id = id;
        this.position = position;
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

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 1007954086)
    public List<WifiInfo> getSignalStrength() {
        if (signalStrength == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            WifiInfoDao targetDao = daoSession.getWifiInfoDao();
            List<WifiInfo> signalStrengthNew = targetDao._queryPoint_SignalStrength(id);
            synchronized (this) {
                if (signalStrength == null) {
                    signalStrength = signalStrengthNew;
                }
            }
        }
        return signalStrength;
    }

    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 1218845129)
    public synchronized void resetSignalStrength() {
        signalStrength = null;
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#delete(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 128553479)
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.delete(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#refresh(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 1942392019)
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.refresh(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#update(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 713229351)
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.update(this);
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 1714415827)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getPointDao() : null;
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