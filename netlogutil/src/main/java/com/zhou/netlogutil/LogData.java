package com.zhou.netlogutil;


/**
 * Created by pqpo on 2017/11/21.
 */
public class LogData {

    private String tag;
    private String msg;
    private long logMemSize;

    private LogData next;

    private static final Object sPoolSync = new Object();
    private static LogData sPool;
    private static int sPoolSize = 0;
    private static final int MAX_POOL_SIZE = 50;

    private static LogData obtain() {
        synchronized (sPoolSync) {
            if (sPool != null) {
                LogData m = sPool;
                sPool = m.next;
                m.next = null;
                sPoolSize--;
                return m;
            }
        }
        return new LogData();
    }

    public static LogData obtain(String tag, String msg) {
        LogData obtain = obtain();
        obtain.tag = tag;
        obtain.msg = msg;
        obtain.logMemSize = (tag == null ? 0 : tag.length()) +
                (msg == null ? 0 : msg.length()) + 8;
        return obtain;
    }

    public void recycle() {
        tag = null;
        msg = null;
        logMemSize = 0;

        synchronized (sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                next = sPool;
                sPool = this;
                sPoolSize++;
            }
        }
    }

    public String getTag() {
        return tag;
    }

    public String getMsg() {
        return msg;
    }

    public long getLogMemSize() {
        return logMemSize;
    }
}
