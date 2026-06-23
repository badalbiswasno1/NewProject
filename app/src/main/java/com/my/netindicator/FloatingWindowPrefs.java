package com.my.netindicator;

import android.content.Context;
import android.content.SharedPreferences;

public class FloatingWindowPrefs {
    private static final String PREFS_NAME = "FloatingWindowPrefs";
    private SharedPreferences prefs;
    
    public FloatingWindowPrefs(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
