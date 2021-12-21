package com.zhou.netlogutil.handler;

import android.os.Build;

import com.zhou.netlogutil.LogData;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

public class LogQueue implements ILogQueue {

    private int quereSize = 0;
    private long memSize = 0;
    private final Queue<LogData> pushQuere;

    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            pushQuere = new LinkedTransferQueue<>();
        } else {
            pushQuere = new LinkedBlockingQueue<>();
        }
    }


    @Override
    public void push(LogData logData) {
        pushQuere.add(logData);
        quereSize++;
        memSize += logData.getLogMemSize();
    }

    @Override
    public LogData poll() {
        LogData logData = pushQuere.poll();
        if (logData != null) {
            quereSize--;
            memSize -= logData.getLogMemSize();
        }
        return logData;
    }

    @Override
    public LogData get() {
        return pushQuere.peek();
    }

    @Override
    public LogData remove() {
        return poll();
    }

    @Override
    public int resetQuereSize() {
        int size = pushQuere.size();
        quereSize = size;
        if (size == 0) {
            memSize = 0;
        }
        return size;
    }

    @Override
    public int getQuereSize() {
        return quereSize;
    }

    @Override
    public long getMemSize() {
        return memSize;
    }

    @Override
    public void clearQuere() {
        try {
            pushQuere.clear();
        } catch (UnsupportedOperationException ignore) {
        } finally {
            resetQuereSize();
        }
    }
}
