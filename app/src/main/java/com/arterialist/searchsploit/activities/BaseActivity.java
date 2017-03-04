package com.arterialist.searchsploit.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import static butterknife.ButterKnife.bind;
import static timber.log.Timber.tag;

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logLifeCycle("onCreate");
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        bind(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        logLifeCycle("onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        logLifeCycle("onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        logLifeCycle("onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        logLifeCycle("onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        logLifeCycle("onRestart");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        logLifeCycle("onSaveInstanceState");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        logLifeCycle("onRestoreInstanceState");
    }

    private void logLifeCycle(String event) {
        tag("Activity").d("%s - %s", getClass().getName(), event);
    }
}