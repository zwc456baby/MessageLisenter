package com.zhou.netlogutil;

import com.zhou.netlogutil.handler.NetLogConfig;
import com.zhou.netlogutil.socket.SocketCallback;

class ConfigImpl implements NetLogConfig {

    private String url;
    private long reconnectTime = 70000;
    private long minReconnectTime = 5000;
    private int maxPoolSize = 10000;
    private SocketCallback callback = null;
    private int connectTimeout = 60000;
    // 队列最大占用内存大小，默认 40M
    private long maxMemSize = 40 * 1024 * 1024;

    @Override
    public NetLogConfig configUrl(String url) {
        this.url = url;
        return this;
    }

    @Override
    public NetLogConfig configReconnectTime(long time) {
        if (time < 3000) {
            time = 3000;
        }
        if (time < minReconnectTime) {
            this.minReconnectTime = time;
        }
        this.reconnectTime = time;
        return this;
    }

    @Override
    public NetLogConfig configMaxPoolSize(int poolSize) {
        if (poolSize < 10) {
            poolSize = 10;
        }
        this.maxPoolSize = poolSize;
        return this;
    }

    @Override
    public NetLogConfig configSocketCallback(SocketCallback callback) {
        this.callback = callback;
        return this;
    }

    @Override
    public NetLogConfig configMinReconnectTime(long time) {
        if (time < 1000) {
            time = 1000;
        }
        if (time > reconnectTime) {
            this.reconnectTime = time;
        }
        this.minReconnectTime = time;
        return this;
    }

    @Override
    public NetLogConfig configConnectTimeout(int timeout) {
        this.connectTimeout = timeout;
        return this;
    }

    @Override
    public NetLogConfig configMaxMemSize(long memSize) {
        if (memSize < 10 * 1024) {
            memSize = 10 * 1024;
        }
        this.maxMemSize = memSize;
        return this;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public long getReconnectTime() {
        return this.reconnectTime;
    }

    @Override
    public long getMinReconnectTime() {
        return minReconnectTime;
    }

    @Override
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    @Override
    public SocketCallback getSocketCallback() {
        return callback;
    }


    @Override
    public int getConnectTimeout() {
        return connectTimeout;
    }

    @Override
    public long getMaxMemSize() {
        return maxMemSize;
    }

}
