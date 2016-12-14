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

public class DatabaseTaskController {

    public static final ExecutorService DATABASE_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final String TAG = DatabaseTaskController.class.getSimpleName();
    private DaoSession daoSession;

    public DatabaseTaskController(DaoSession daoSession) {
        this.daoSession = daoSession;
    }

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

    public Task<List<WifiInfo>> saveWifiInfoList(final List<WifiInfo> wifiInfoList) {
        return Task.call(new Callable<List<WifiInfo>>() {
            @Override
            public List<WifiInfo> call() throws Exception {
                daoSession.getWifiInfoDao().saveInTx(wifiInfoList);
                return wifiInfoList;
            }
        }, DATABASE_EXECUTOR);
    }

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

    public Task<List<Point>> getPointList() {
        return Task.call(new Callable<List<Point>>() {
            @Override
            public List<Point> call() throws Exception {
                return daoSession.getPointDao().loadAll();
            }
        }, DATABASE_EXECUTOR);
    }
}