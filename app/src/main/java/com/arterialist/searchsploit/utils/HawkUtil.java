package com.arterialist.searchsploit.utils;

import android.annotation.SuppressLint;
import android.support.annotation.StringDef;

import com.arterialist.searchsploit.models.Exploit;
import com.orhanobut.hawk.Hawk;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Calendar;

public class HawkUtil {

    public static final String DATABASE_CONFIGURED_KEY = "DATABASE_CONFIGURED";
    public static final String DATABASE_PATH_KEY = "DATABASE_PATH";
    public static final String BOOKMARKED_EXPLOITS_KEY = "BOOKMARKED_EXPLOITS";
    public static final String LAST_DATABASE_UPDATE_KEY = "LAST_DATABASE_UPDATE";

    public static <T> void set(@Keys String key, T value) {
        Hawk.put(key, value);
    }

    public static <T> T get(@Keys String key) {
        return Hawk.get(key);
    }

    @SuppressLint("SdCardPath")
    public static void fill() {
        if (!Hawk.contains(DATABASE_CONFIGURED_KEY)) {
            set(DATABASE_CONFIGURED_KEY, false);
        }
        if (!Hawk.contains(DATABASE_PATH_KEY)) {
            set(DATABASE_PATH_KEY, "/sdcard/");
        }
        if (!Hawk.contains(BOOKMARKED_EXPLOITS_KEY)) {
            set(BOOKMARKED_EXPLOITS_KEY, new ArrayList<Exploit>());
        }
        if (!Hawk.contains(LAST_DATABASE_UPDATE_KEY)) {
            Calendar instance = Calendar.getInstance();
            instance.set(Calendar.DAY_OF_YEAR, instance.get(Calendar.DAY_OF_YEAR) - 1);
            set(LAST_DATABASE_UPDATE_KEY, instance.getTimeInMillis());
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({DATABASE_CONFIGURED_KEY, DATABASE_PATH_KEY, BOOKMARKED_EXPLOITS_KEY, LAST_DATABASE_UPDATE_KEY})
    @interface Keys {
    }
}
