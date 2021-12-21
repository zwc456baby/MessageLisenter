package com.zhou.netlogutil.socket;

public interface SocketCallback {

    void onDisconnect();

    void onReconnect();

    void onSend();
}
