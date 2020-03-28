package com.zhou.example.messagelisenterservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static android.content.Context.NOTIFICATION_SERVICE;

class Utils {

//    private static final File notifycationFilePath = new File(Environment.getExternalStorageDirectory().getAbsolutePath());


    static void putStr(Context context, String value) {
        if (context == null) {
            return;
        }
        File notifycationFilePath = context.getExternalFilesDir("log");
        if (notifycationFilePath == null || !canWrite(notifycationFilePath)) return;

        String notifycationFileName = "notifycation_file.txt";
        File file = new File(notifycationFilePath, notifycationFileName);
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(file, true), 1024);
            out.write(value);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean canWrite(File notifycationFilePath) {
        return notifycationFilePath.canWrite();
    }

    static void createNotificationChannel(Context context, String channelId, CharSequence channelName, int importance) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);

            channel.enableLights(true);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    static void sendNotifyMessage(Context context, String title, String text, int type) {
        if (type == 1) {
            NotificationManager manager = (NotificationManager) context.
                    getSystemService(NOTIFICATION_SERVICE);
            if (manager == null) return;
            String channelId = "MessageNotify";
            Notification.Builder notificationBuild;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Utils.createNotificationChannel(context, channelId
                        , "remote message notify"
                        , NotificationManager.IMPORTANCE_HIGH);
                notificationBuild = new Notification.Builder(context, channelId);
            } else {
                notificationBuild = new Notification.Builder(context);
            }
            notificationBuild.setContentTitle(title)
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setShowWhen(true)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setAutoCancel(true);
            int id = (channelId + String.valueOf(System.currentTimeMillis())).hashCode();
            String showTvText = String.format("%s\n%s", title, text);
            Intent intent = new Intent(context, LockShowActivity.class);
            intent.putExtra(Constant.GET_MESSAGE_KEY, showTvText);
            intent.putExtra(LockShowActivity.GET_SHOW_ACTIVITY_TYPE, type);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, id
                    , intent, PendingIntent.FLAG_CANCEL_CURRENT);
            notificationBuild.setContentIntent(pendingIntent);
            // 正式发出通知
            manager.notify(id, notificationBuild.build());
        }

    }
}
