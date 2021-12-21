package com.zhou.netlogutil.socket;

import android.os.SystemClock;
import android.text.TextUtils;

import com.zhou.netlogutil.LogData;
import com.zhou.netlogutil.handler.ILogQueue;
import com.zhou.netlogutil.handler.LogHandler;
import com.zhou.netlogutil.handler.NetLogConfig;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

public class PushSocket {
    private int netStat = -1;

    private boolean exit = true;

    private long autoReconnectTime = -1;

    private final NetLogConfig logConfig;
    private final ILogQueue logQueue;
    private final LogHandler logHandler;

    public PushSocket(NetLogConfig config,
                      LogHandler logHandler,
                      ILogQueue logQueue) {
        logConfig = config;
        this.logHandler = logHandler;
        this.logQueue = logQueue;
    }

    public void start() {
        if (!isExit()) {
            return;
        }
        exit = false;
        autoReconnectTime = SystemClock.elapsedRealtime();
        ThreadUtils.workPost(flashRunnable);
    }

    public void exit() {
        if (isExit()) {
            return;
        }
        System.out.println("web socket exit");
        exit = true;
        ThreadUtils.removeExecute(flashRunnable);
    }

    private boolean send(String tag, String msg) {
        if (isExit()) return false;
        if (tag == null) {
            tag = "";
        }
        if (msg == null) {
            msg = "";
        }
        try {
            return sendMessage(buildUrl(logConfig.getUrl()
                    , URLEncoder.encode(tag, "utf-8"), URLEncoder.encode(msg, "utf-8")));
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }

    private String buildUrl(String firstUrl, String title, String msg) {
        try {
            return String.format(firstUrl, title, msg);
        } catch (Exception e) {
            return firstUrl;
        }
    }

    private boolean sendMessage(String urlStr) {
        try {
            URL url = new URL(urlStr);
            //得到connection对象。
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(logConfig.getConnectTimeout());
            connection.setReadTimeout(logConfig.getConnectTimeout());
            //设置请求方式
            connection.setRequestMethod("GET");
            //连接
            connection.connect();
            //得到响应码
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return true;
            }
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void startPushWork() {
        ThreadUtils.removeWork(flashRunnable);
        ThreadUtils.workPost(flashRunnable);
    }

    private void autoReconnect() {
        double tempBl = ((double) (logConfig.getMaxPoolSize()
                - logQueue.getQuereSize())) / ((double) logConfig.getMaxPoolSize());
        if (tempBl < 0) {
            tempBl = 0;
        }
        long tempDelay = (long) (((double) (logConfig.getReconnectTime()
                - logConfig.getMinReconnectTime())) * tempBl)
                + logConfig.getMinReconnectTime();
        if (tempDelay < 1000) {
            tempDelay = 1000;
        }
        autoReconnect(tempDelay - (SystemClock.elapsedRealtime() - autoReconnectTime));
    }

    private void autoReconnect(long tempDelay) {
        ThreadUtils.removeExecute(flashRunnable);
        if (isExit()) {
            return;
        }
        if (logHandler.isflush() && tempDelay >= 0) { // 如果当前正在刷新数据
            tempDelay = -1;
        }
        ThreadUtils.executeDelayed(() -> {
            SocketCallback socketCallback = logConfig == null ?
                    null : logConfig.getSocketCallback();
            if (socketCallback != null) {  // 调用重连
                socketCallback.onReconnect();
            }
        }, tempDelay);
        ThreadUtils.executeDelayed(flashRunnable, tempDelay);
    }

    private boolean isExit() {
        return exit;
    }

    public void setNetStat(int netStat) {
        if (this.netStat != netStat && netStat == 1) {
            autoReconnect(-1);
        }
        this.netStat = netStat;
    }

    private final Runnable flashRunnable = new Runnable() {

        @Override
        public void run() {
            if (isExit() || logHandler == null) {
                return;
            }
            autoReconnectTime = SystemClock.elapsedRealtime();
            if (netStat == 0 && !logHandler.isflush()) {
                autoReconnect();
                return;
            }

            // 开始进行刷新
            if (logHandler.isflush()) { // 如果正处于flush阶段，则不再发送心跳,若处于open 状态，直接发送
                while (!isExit() && (logQueue.getQuereSize() > 0 || logQueue.resetQuereSize() > 0)) {
                    if (!pushLineLog()) {
                        logQueue.remove();  // push 失败也要依旧移除首条消息
                    }
                }
                logHandler.cancelFlush();
            } else {
                //取出数据并发送
                if (pushLineLog()) {
                    ThreadUtils.workPost(flashRunnable);
                } else if (logQueue.getQuereSize() > 0 || logQueue.resetQuereSize() > 0) {
                    autoReconnect();
                }
            }
        }

        private boolean pushLineLog() {
            LogData logData = logQueue.get();
            if (logData == null) {
                return false;
            }
            if (send(logData.getTag(), logData.getMsg())) {
                logQueue.remove();
                logData.recycle();
                // 发送成功后，调用一下回调
                SocketCallback socketCallback = logConfig == null ?
                        null : logConfig.getSocketCallback();
                if (socketCallback != null) {  // 调用重连
                    socketCallback.onSend();
                }
                return true;
            }
            return false;
        }
    };
}
