package com.zhou.example.messagelisenterservice;

import android.app.Application;
import android.os.Process;
import android.util.Log;

import com.zhou.netlogutil.NetLogUtil;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Utils.putStr("exception:" + Log.getStackTraceString(throwable));
                Process.killProcess(Process.myPid());
            }
        });
        NetLogUtil.Init(this);
    }
}
