package com.arterialist.searchsploit.common;

import android.app.Application;

import com.arterialist.searchsploit.BuildConfig;
import com.arterialist.searchsploit.utils.HawkUtil;
import com.orhanobut.hawk.Hawk;

import timber.log.Timber;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        Hawk.init(this).build();
        HawkUtil.fill();
    }
}
