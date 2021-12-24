package com.zhou.netlogutil.socket;

import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by zhouwenchao on 2017-04-27.
 */
public final class ThreadUtils {
    // 后台线程（注意：不要死锁了，否则导致其它页面卡死）
    private static volatile Handler asyncHandler;

    static {
        new Thread(() -> {
            Looper.prepare();
            asyncHandler = new Handler(Looper.myLooper());
            // 线程优先级
            Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);
            while (true) {
                try {
                    Looper.loop();
                } catch (Exception e) {
                    Log.i("ThreadUtils", "loop work error: " + Log.getStackTraceString(e));
                }
            }
        }).start();
    }

    /**
     * 异步执行一个任务, 所有任务都在同一个线程中执行
     * <p>
     * 优点：为了进行程序优化，许多任务都采用异步执行
     * 但是异步执行时，不同的线程可能导致数据、状态等不同
     * 如果采用单个线程，将能够有效避免这些问题
     * <p>
     * 经过实际测试，采用协程开启任务和采用异步 Handler 的方式
     * Handler 更有优势，如果是单个任务，建议采用 Handler
     * <p>
     * GlobalScop 开启一个协程约 1ms ，Handler post 任务耗时<0.1ms
     *
     * @param r 任务对象
     */
    public static void workPost(Runnable r) {
        //noinspection StatementWithEmptyBody
        while (asyncHandler == null) {  // 等待初始化
        }
        asyncHandler.post(r);
    }

    public static void workPostDelay(Runnable r, long time) {
        //noinspection StatementWithEmptyBody
        while (asyncHandler == null) {  // 等待初始化
        }
        asyncHandler.postDelayed(r, time);
    }

    public static void removeWork(Runnable r) {
        //noinspection StatementWithEmptyBody
        while (asyncHandler == null) {  // 等待初始化
        }
        asyncHandler.removeCallbacks(r);
    }
}
