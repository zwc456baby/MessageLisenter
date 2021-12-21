package com.zhou.netlogutil.handler;

import com.zhou.netlogutil.LogData;

public interface ILogQueue {

    void push(LogData logData);

    LogData poll();

    LogData get();

    LogData remove();

    int resetQuereSize();

    int getQuereSize();

    long getMemSize();

    void clearQuere();
}
