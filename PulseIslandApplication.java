package com.pulseisland;

import android.app.Application;

import com.pulseisland.utils.ShizukuHelper;

public class PulseIslandApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ShizukuHelper.init(this);
    }
}
