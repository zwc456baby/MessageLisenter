package com.zhou.netlogutil.handler;

import com.zhou.netlogutil.socket.SocketCallback;

public interface NetLogConfig {
    NetLogConfig configUrl(String url);

    NetLogConfig configTitleKey(String account);

    NetLogConfig configMessageKey(String fileName);

    NetLogConfig configReconnectTime(long time);

    NetLogConfig configMaxPoolSize(int poolSize);

    NetLogConfig configMinReconnectTime(long time);

    NetLogConfig configConnectTimeout(int timeout);

    NetLogConfig configMaxMemSize(long memSize);

    NetLogConfig configSocketCallback(SocketCallback callback);

    String getUrl();

    String getTitleKey();

    String getMessageKey();

    long getReconnectTime();

    long getMinReconnectTime();

    int getMaxPoolSize();

    int getConnectTimeout();

    long getMaxMemSize();

    SocketCallback getSocketCallback();
}