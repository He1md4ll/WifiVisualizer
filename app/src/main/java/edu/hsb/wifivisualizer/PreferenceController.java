package edu.hsb.wifivisualizer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceController {

    public static final String PREF_FILTER = "pref_filter";
    public static final String PREF_ISO_VALUES = "pref_iso_values";

    private SharedPreferences sharedPreferences;

    public PreferenceController(Context context) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getIsoValues() {
        return sharedPreferences.getString(PREF_ISO_VALUES, "");
    }

    public void setIsoValues(String isoValues) {
        sharedPreferences.edit().putString(PREF_ISO_VALUES, isoValues).commit();
    }

    public int getFilter() {
        return sharedPreferences.getInt(PREF_FILTER, 0);
    }

    public void setFilter(int filter) {
        sharedPreferences.edit().putInt(PREF_FILTER, filter).commit();
    }
}
