package com.zhou.example.messagelisenterservice;

import android.annotation.TargetApi;
import android.app.Notification;
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
import android.text.TextUtils;
import android.util.Log;

@SuppressWarnings("FieldCanBeLocal")
public class ForegroundServer extends Service {
    public static final String GET_SERVER_TYPE_KEY = "get_server_type_key";
    public static final String GET_NOTIFY_TITLE = "get_notify_title";
    public static final String GET_NOTIFY_TEXT = "get_notify_text";

    private final int FOREGROUND_ID = 1;

    private final String channel_name = "ForegoundServer";
    private final String CHANNEL_ID = "service";

    private Handler handler = new Handler(Looper.getMainLooper());

    private final long MIN_SHOW_TIME = 2000;
    private final long MAX_SHOW_TIME = 10000;

    private long enterTime;

    private int curShowType = -1;

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
        if (curShowType != 0) {
            if (intent.getIntExtra(GET_SERVER_TYPE_KEY, -1) != 0) {
                //如果不是常驻通知
                handler.postDelayed(stopServerRunnable, MAX_SHOW_TIME);
            } else {
                curShowType = intent.getIntExtra(GET_SERVER_TYPE_KEY, 0);
            }
        }
        updateNotify(intent);
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
        if (curShowType != -1) {
            return;
        }

        Utils.createNotificationChannel(this, CHANNEL_ID, channel_name
                , NotificationManager.IMPORTANCE_DEFAULT);
        Notification notification = getNotifycation(null);

        startForeground(FOREGROUND_ID, notification);
    }

    private void updateNotify(Intent intent) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            Notification notification = getNotifycation(intent);

            notificationManager.notify(FOREGROUND_ID, notification);
        }
    }

    private Notification getNotifycation(Intent intent) {
        String title = intent == null ? getString(R.string.app_name) :
                TextUtils.isEmpty(intent.getStringExtra(GET_NOTIFY_TITLE)) ? getString(R.string.app_name)
                        : intent.getStringExtra(GET_NOTIFY_TITLE);
        String text = intent == null ? getString(R.string.click_close_notify) :
                TextUtils.isEmpty(intent.getStringExtra(GET_NOTIFY_TEXT)) ? getString(R.string.click_close_notify)
                        : intent.getStringExtra(GET_NOTIFY_TEXT);
        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setContentTitle(title)//设置通知标题
                .setContentText(text)//设置通知内容
                .setPriority(Notification.PRIORITY_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setAutoCancel(true);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationBuilder.setChannelId(CHANNEL_ID);
        }

        Notification notification = notificationBuilder.build();

        Intent notificationIntent = new Intent(getApplicationContext(), StartReceive.class);
        notificationIntent.setAction(StartReceive.TRY_CLOSE_ACTIVITY_ACTION);
        notification.contentIntent = PendingIntent.getBroadcast(getApplicationContext(),
                0, notificationIntent, 0);
        return notification;
    }

    private void registerFinishBroad() {
        IntentFilter filter = new IntentFilter(Constant.FINISH_FOREGROUND_SERVICE);
        filter.addAction(Constant.CLOSE_ACTIVITY_STOP_NOTIFY_ACTION);
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
                if (curShowType == 0) {  //如果是常驻的通知，不接受停止前台指令
                    return;
                }
                long temp = SystemClock.elapsedRealtime() - enterTime;
                handler.removeCallbacks(stopServerRunnable);
                if (temp > MIN_SHOW_TIME) {
                    handler.post(stopServerRunnable);
                } else {
                    handler.postDelayed(stopServerRunnable, MIN_SHOW_TIME - temp);
                }
            } else if (Constant.CLOSE_ACTIVITY_STOP_NOTIFY_ACTION.equals(intent.getAction())) {
                int type = intent.getIntExtra(LockShowActivity.GET_SHOW_ACTIVITY_TYPE, -1);
                if (curShowType == 0 && curShowType == type) {
                    handler.post(stopServerRunnable);
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
