package edu.hsb.wifivisualizer;

import android.app.Application;

import org.greenrobot.greendao.database.Database;

import edu.hsb.wifivisualizer.database.DaoMaster;
import edu.hsb.wifivisualizer.database.DaoSession;

/**
 * Main context of the app
 * Manages database session (that is needed to access local generated database)
 */
public class WifiVisualizerApp extends Application {

    public static final String DATABASE_NAME = "wifi-db";
    private DaoSession daoSession;

    /**
     * First method that is called in the app --> Creates database session
     */
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
