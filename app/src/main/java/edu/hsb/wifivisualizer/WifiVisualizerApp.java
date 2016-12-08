package edu.hsb.wifivisualizer;

import android.app.Application;

import org.greenrobot.greendao.database.Database;

import edu.hsb.wifivisualizer.database.DaoMaster;
import edu.hsb.wifivisualizer.database.DaoSession;

public class WifiVisualizerApp extends Application {

    public static final String DATABASE_NAME = "wifi-db";
    private DaoSession daoSession;

    @Override
    public void onCreate() {
        super.onCreate();

        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, DATABASE_NAME);
        Database db = helper.getWritableDb();
        daoSession = new DaoMaster(db).newSession();
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }
}
