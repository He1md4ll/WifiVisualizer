package edu.hsb.wifivisualizer;

import android.util.Log;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bolts.Task;
import edu.hsb.wifivisualizer.database.DaoSession;
import edu.hsb.wifivisualizer.model.Point;
import edu.hsb.wifivisualizer.model.WifiInfo;

/**
 * Provides tasks to work with local database:
 * Save points and wifi info data, remove points, load all points and clear all data
 * Database is generated from entities by GreenDao ('org.greenrobot:greendao:3.2.0')
 * Task abstraction from bolts library ('com.parse.bolts:bolts-tasks:1.4.0')
 */
public class DatabaseTaskController {

    // Thread pool operating the database --> Only this thread should work with the database to prevent transaction errors
    public static final ExecutorService DATABASE_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final String TAG = DatabaseTaskController.class.getSimpleName();
    private DaoSession daoSession;

    public DatabaseTaskController(DaoSession daoSession) {
        this.daoSession = daoSession;
    }

    /**
     * Task to save provided point in local database
     * @param point Point to save
     * @return Task containing saved point (greenDAO sets point-ID after successful save)
     */
    public Task<Point> savePoint(final Point point) {
        return Task.call(new Callable<Point>() {
            @Override
            public Point call() throws Exception {
                daoSession.getPointDao().save(point);
                Log.d(TAG, "Saved point in database");
                return point;
            }
        }, DATABASE_EXECUTOR);
    }

    /**
     * Task to save provided wifi info data in local database
     * @param wifiInfoList wifi info list to save
     * @return Task containing saved wifi info data
     */
    public Task<List<WifiInfo>> saveWifiInfoList(final List<WifiInfo> wifiInfoList) {
        return Task.call(new Callable<List<WifiInfo>>() {
            @Override
            public List<WifiInfo> call() throws Exception {
                daoSession.getWifiInfoDao().saveInTx(wifiInfoList);
                return wifiInfoList;
            }
        }, DATABASE_EXECUTOR);
    }

    /**
     * Task to remove poitn from local database --> Entity identified by ID
     * @param point Point to remove
     * @return Task
     */
    public Task<Void> removePoint(final Point point) {
        return Task.call(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                daoSession.getWifiInfoDao().deleteInTx(point.getSignalStrength());
                daoSession.getPointDao().delete(point);
                Log.d(TAG, "Deleted point in database");
                return null;
            }
        }, DATABASE_EXECUTOR);
    }

    /**
     * Task to loads all points from database
     * @return Task containing loaded points
     */
    public Task<List<Point>> getPointList() {
        return Task.call(new Callable<List<Point>>() {
            @Override
            public List<Point> call() throws Exception {
                return daoSession.getPointDao().loadAll();
            }
        }, DATABASE_EXECUTOR);
    }

    /**
     * Task to clear local database
     * @return Task
     */
    public Task<Void> clearAll() {
        return Task.call(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                daoSession.getWifiInfoDao().deleteAll();
                daoSession.getPointDao().deleteAll();
                return null;
            }
        }, DATABASE_EXECUTOR);
    }
}