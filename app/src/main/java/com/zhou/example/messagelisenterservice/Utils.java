package com.zhou.example.messagelisenterservice;

import android.os.SystemClock;

import com.zhou.netlogutil.NetLogUtil;

public class Utils {

    public static void resetReconnectTime() {
        long curTime = NetLogUtil.getConfig().getReconnectTime();
        if (curTime < 60 * 1000) {
            curTime += curTime;
            if (curTime > 60 * 1000) {
                curTime = 60 * 1000;
            }
        } else {
            curTime += 60 * 1000;
            if (curTime > 5 * 60 * 1000) {
                curTime = 5 * 60 * 1000;
            }
        }
        NetLogUtil.getConfig().configReconnectTime(curTime);
    }

    public static boolean needUpload(int size, long time) {
        //如果长度大于 1000 必须上传
        if (size >= 100) return true;
        //如果时间超过两小时，必须上传
        if ((SystemClock.elapsedRealtime() - time) > 2 * 60 * 60 * 1000
                && size > 0) return true;

        int temp = 0;
        temp += size;
        temp += (((SystemClock.elapsedRealtime() - time) / (6 * 1000)) / 12);

        return temp >= 100;
    }
}
