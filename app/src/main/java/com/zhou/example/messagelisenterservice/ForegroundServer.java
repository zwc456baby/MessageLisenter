package com.zhou.example.messagelisenterservice;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

@SuppressWarnings("FieldCanBeLocal")
public class ForegroundServer extends Service {

    private final int FOREGROUND_ID = 1;

    private final String channel_name = "ForegoundServer";
    private final String CHANNEL_ID = "service";

    private Handler handler = new Handler(Looper.getMainLooper());

    private final long MIN_SHOW_TIME = 2000;
    private final long MAX_SHOW_TIME = 10000;

    private long enterTime;

    @Override
    public void onCreate() {
        super.onCreate();
        enterTime = SystemClock.elapsedRealtime();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            setForegroundService();
        }
        registerFinishBroad();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.removeCallbacks(stopServerRunnable);
        handler.postDelayed(stopServerRunnable, MAX_SHOW_TIME);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        unregisterFinishBroad();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Log.i("ForegoundServer", "stop notification");
        if (notificationManager != null) {
            notificationManager.cancel(FOREGROUND_ID);
        }
        handler.removeCallbacks(stopServerRunnable);
        super.onDestroy();
    }

    /**
     * 通过通知启动服务
     */
    @TargetApi(Build.VERSION_CODES.O)
    private void setForegroundService() {
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channel_name, importance);
        channel.enableLights(true);
        channel.setShowBadge(true);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            Log.i("ForegoundServer", "create notify channel");
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))//设置通知标题
                .setChannelId(CHANNEL_ID)
                .setContentText(getString(R.string.wait_service_connect))//设置通知内容
                .setPriority(Notification.PRIORITY_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher)
                .setAutoCancel(true)
                .build();//设置处于运行状态

        Intent notificationIntent = new Intent(getApplicationContext(), SettingActivity.class);
//        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        notification.contentIntent = PendingIntent.getActivity(getApplicationContext(),
                0, notificationIntent, 0);

        startForeground(FOREGROUND_ID, notification);
    }

    private void registerFinishBroad() {
        IntentFilter filter = new IntentFilter(Constant.FINISH_FOREGROUND_SERVICE);
        registerReceiver(finishServiceBroadcast, filter);
    }

    private void unregisterFinishBroad() {
        try {
            this.unregisterReceiver(finishServiceBroadcast);
        } catch (Exception ignore) {
        }
    }

    private Runnable stopServerRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent1 = new Intent(ForegroundServer.this, ForegroundServer.class);
            stopService(intent1);
        }
    };

    private BroadcastReceiver finishServiceBroadcast = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constant.FINISH_FOREGROUND_SERVICE.equals(intent.getAction())) {
                long temp = SystemClock.elapsedRealtime() - enterTime;
                handler.removeCallbacks(stopServerRunnable);
                if (temp > MIN_SHOW_TIME) {
                    handler.post(stopServerRunnable);
                } else {
                    handler.postDelayed(stopServerRunnable, MIN_SHOW_TIME - temp);
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
