package com.zhou.example.messagelisenterservice;

import android.app.Application;

import com.zhou.example.messagelisenterservice.db.DBUtil;

public class ServiceApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DBUtil.INSTANT.INIT(this);
    }
}
