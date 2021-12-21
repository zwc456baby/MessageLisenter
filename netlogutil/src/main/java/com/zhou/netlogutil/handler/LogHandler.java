package com.zhou.netlogutil.handler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;

import com.zhou.netlogutil.LogData;
import com.zhou.netlogutil.socket.PushSocket;
import com.zhou.netlogutil.socket.SocketCallback;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class LogHandler {
    private final Object flushLock = new Object();

    private boolean connect = false;
    private Context mContext = null;
    private boolean receiveNetWorkAction = false;
    private boolean isFlush = false;
    private PushSocket pushSocket;
    private ILogQueue logQueue;
    private NetLogConfig logConfig;

    public void connect(Context context, NetLogConfig config) {
        if (config == null) {
            disconnect();
            return;
        }
        _disconnect();
        System.out.println("client connect");
        logConfig = config;
        mContext = context == null ?
                null : context.getApplicationContext();

        listenNetwork();
        if (logQueue == null) {
            logQueue = new LogQueue();
        }
        PushSocket socket = new PushSocket(config, this,
                logQueue);
        connect = true;
        pushSocket = socket;
        socket.start();
    }

    public void log(String TAG, String msg) {
        NetLogConfig localConfig;
        if (!connect || (localConfig = logConfig) == null) {
            return;
        }
        ILogQueue logQueue = this.logQueue;
        if (logQueue != null) {
            // MaxPoolSize 和 MaxMemerySize 在多线程下无法精确确定，如果用同步锁，会导致性能较差，所以这是一个不精确的计算，但符合要求
            // 防止队列过长，或者防止内存溢出，默认最大 一万条队列 和 40M 内存
            if (logQueue.getQuereSize() > localConfig.getMaxPoolSize()
                    || logQueue.getMemSize() > localConfig.getMaxMemSize()) {
                while (logQueue.getQuereSize() > localConfig.getMaxPoolSize()
                        || logQueue.getMemSize() > localConfig.getMaxMemSize()) {
                    logQueue.poll();
                }
            }
            logQueue.push(LogData.obtain(TAG, msg));
        }
        PushSocket socket = this.pushSocket;
        if (socket != null) {
            socket.startPushWork();
        }
    }

    public boolean isConnect() {
        return connect;
    }

    public boolean isOpen() {
        PushSocket socket = this.pushSocket;
        return socket != null;
    }

    public void clearQuere() {
        ILogQueue logQueue = this.logQueue;
        if (logQueue != null) {
            logQueue.clearQuere();
        }
    }

    public void reconnect() {
        if (!connect) {
            return;
        }
        System.out.println("client reconnect");
        _disconnect();
        NetLogConfig localConfig = this.logConfig;
        if (localConfig == null
                || logQueue == null) {
            disconnect();
            return;
        }
        PushSocket socket = new PushSocket(localConfig, this,
                logQueue);
        pushSocket = socket;
        socket.start();
    }

    /**
     * 有回调的退出
     */
    public void disconnect() {
        System.out.println("client disconnect");
        NetLogConfig localConfig = this.logConfig;
        SocketCallback socketCallback = localConfig == null ?
                null : localConfig.getSocketCallback();
        connect = false;
        this.logConfig = null;
        this.mContext = null;
        cancelFlush();
        ILogQueue logQueue = this.logQueue;
        if (logQueue != null) {
            logQueue.clearQuere();
        }
        _disconnect();
        this.logQueue = null;
        unlistenNetwork();
        if (socketCallback != null) {
            socketCallback.onDisconnect();
        }
    }

    /**
     * 无回调的退出，用来重连
     */
    private void _disconnect() {
        PushSocket socket = this.pushSocket;
        if (socket != null) {
            socket.exit();
            pushSocket = null;
        }
    }

    public boolean isflush() {
        return this.isFlush;
    }

    public boolean flush() {
        NetLogConfig localConfig = this.logConfig;
        return localConfig != null && flush(localConfig.getConnectTimeout());
    }

    /*
     * 刷新队列中的消息，注意：会阻塞线程
     *
     */
    public boolean flush(int timeOut) {
        if (!connect) {
            return false;
        }
        this.isFlush = true;
        boolean flushSuccess = reflush(SystemClock.elapsedRealtime(), timeOut);
        cancelFlush();
        return flushSuccess;
    }

    public void cancelFlush() {
        this.isFlush = false;
        synchronized (flushLock) {
            flushLock.notifyAll();
        }
    }

    private boolean reflush(long enterTime, int timeOut) {
        if (!connect) { //设置最大调用栈深度，防止栈溢出
            return false;
        }
        PushSocket socket = pushSocket;
        if (socket == null) {
            return false;
        }
        ILogQueue logQueue = this.logQueue;
        if (logQueue == null) {
            return false;
        }
        socket.startPushWork();
        while (logQueue.getQuereSize() > 0 && this.isFlush) { // 等待flush 完成
            if (!connect) return false;
            long waitTime = timeOut - (SystemClock.elapsedRealtime() - enterTime);
            if (waitTime <= 0) {
                return false;
            }
            try {
                synchronized (flushLock) {
                    flushLock.wait(waitTime);
                }
            } catch (InterruptedException ignore) {
            }
        }
        return true;
    }

    private void listenNetwork() {
        if (receiveNetWorkAction || !connect || mContext == null) {
            return;
        }
        receiveNetWorkAction = true;
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        try {
            mContext.registerReceiver(networkReceiver, filter);
        } catch (Exception ignore) {
        }
    }

    private void unlistenNetwork() {
        if (!receiveNetWorkAction || mContext == null) {
            return;
        }
        receiveNetWorkAction = false;
        try {
            mContext.unregisterReceiver(networkReceiver);
        } catch (Exception ignore) {
        }
    }

    private final BroadcastReceiver networkReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (receiveNetWorkAction) {
                int tempNetStat = getNetIsConnect();
                PushSocket pushSocket = LogHandler.this.pushSocket;
                if (pushSocket != null) {
                    pushSocket.setNetStat(tempNetStat);
                }
            } else {
                unlistenNetwork();
            }
        }
    };

    private int getNetIsConnect() {
        if (mContext == null) {
            return -1;
        }
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext
                .getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = null;
        if (connectivityManager != null) {
            info = connectivityManager.getActiveNetworkInfo();
        }
        if (info == null) {
            return -1;
        }
        return info.getState() == NetworkInfo.State.CONNECTED ? 1 : 0;
    }
}
