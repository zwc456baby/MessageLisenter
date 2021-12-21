package com.zhou.netlogutil;

import android.annotation.SuppressLint;
import android.content.Context;

import com.zhou.netlogutil.handler.LogHandler;
import com.zhou.netlogutil.handler.NetLogConfig;

@SuppressWarnings({"WeakerAccess", "unused"})
public class NetLogUtil {

    @SuppressLint("StaticFieldLeak")
    private static LogHandler logHandler;

    public synchronized static void connect(Context context, NetLogConfig config) {
        LogHandler handler;
        if ((handler = logHandler) == null) {
            handler = new LogHandler();
            handler.connect(context, config);
            logHandler = handler;
        } else {
            handler.connect(context, config);
        }
    }

    public static NetLogConfig buildConfig() {
        return new ConfigImpl();
    }

    public static void log(String msg) {
        log(null, msg);
    }

    public static void log(String TAG, String msg) {
        LogHandler handler;
        if ((handler = logHandler) != null) {
            handler.log(TAG, msg);
        }
    }

    public static boolean isConnect() {
        LogHandler handler = logHandler;
        return handler != null && handler.isConnect();
    }

    public static boolean isOpen() {
        LogHandler handler = logHandler;
        return handler != null && handler.isOpen();
    }

    public static void clearQuere() {
        LogHandler handler;
        if ((handler = logHandler) != null) {
            handler.clearQuere();
        }
    }

    public synchronized static void reconnect() {
        LogHandler handler;
        if ((handler = logHandler) != null) {
            handler.reconnect();
        }
    }

    public synchronized static void disconnect() {
        LogHandler handler;
        if ((handler = logHandler) != null) {
            handler.disconnect();
        }
        logHandler = null;
    }

    public static boolean flush() {
        LogHandler handler = logHandler;
        return handler != null && handler.flush();
    }

    /*
     * 刷新队列中的消息，注意：会阻塞线程
     *
     */
    public static boolean flush(int timeOut) {
        LogHandler handler = logHandler;
        return handler != null && handler.flush(timeOut);
    }
}
