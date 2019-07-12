package com.zhou.example.messagelisenterservice;

import android.app.Application;

import com.zhou.netlogutil.NetLogUtil;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NetLogUtil.Init(this);
    }
}
