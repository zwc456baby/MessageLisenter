package com.zhou.example.messagelisenterservice;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.POWER_SERVICE;

class Utils {

    private final static String dayType = "yyyy-MM-dd HH:mm:ss";
    private static int notifyDay = -1;

    static void putStr(Context context, String value) {
        if (context == null) {
            return;
        }
        File notifycationFilePath = context.getExternalFilesDir("log");
        if (notifycationFilePath == null || !canWrite(notifycationFilePath)) return;

        String notifycationFileName = "notifycation_file.txt";
        File file = new File(notifycationFilePath, notifycationFileName);
        // 为了防止文件无限增大，只保留当天的数据
        if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) != notifyDay) {
            if (file.exists() && file.canWrite()) {
                FileUtils fileUtils = new FileUtils();
                if (fileUtils.deleteFileSafely(file)) {
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            notifyDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        }

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


    static boolean checkBatteryWhiteList(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
            if (powerManager == null) return true;
            return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return true;
    }

    static void gotoBatterySetting(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            @SuppressLint("BatteryLife")
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        }
    }

    static String formatTime(Date time) {
        DateFormat dataFormat = new SimpleDateFormat(dayType, Locale.getDefault());
        return dataFormat.format(time);
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
            String channelId = "MessageNotify";
            int channelLevel = -1;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                channelLevel = NotificationManager.IMPORTANCE_HIGH;
            }
            int id = (channelId + System.currentTimeMillis()).hashCode();
            String showTvText = String.format("%s\n%s", title, text);
            Intent intent = new Intent(context, LockShowActivity.class);
            intent.putExtra(Constant.GET_MESSAGE_KEY, showTvText);
            intent.putExtra(LockShowActivity.GET_SHOW_ACTIVITY_TYPE, type);

            sendNotify(context, channelId, "remote message notify",
                    channelLevel, Notification.PRIORITY_HIGH, id,
                    title, text, intent);
        }
    }

    static void sendBatteryNotify(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            String channelId = "BatteryNotify";
            int channelLevel = -1;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                channelLevel = NotificationManager.IMPORTANCE_MAX;
            }
            @SuppressLint("BatteryLife")
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            int id = (channelId + System.currentTimeMillis()).hashCode();
            sendNotify(context, channelId, "battery white list notify",
                    channelLevel, Notification.PRIORITY_MAX, id,
                    context.getString(R.string.app_name), context.getString(R.string.click_add_to_battery_white_list),
                    intent);
        }
    }

    private static void sendNotify(Context context, String channelId, String channelName,
                                   int channelLevel, int priority, int id,
                                   String title, String text, Intent intent) {
        NotificationManager manager = (NotificationManager) context.
                getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;
        Notification.Builder notificationBuild;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (channelLevel < 0) {
                channelLevel = NotificationManager.IMPORTANCE_HIGH;
            }
            Utils.createNotificationChannel(context, channelId
                    , channelName
                    , channelLevel);
            notificationBuild = new Notification.Builder(context, channelId);
        } else {
            notificationBuild = new Notification.Builder(context);
        }
        notificationBuild.setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText(text))
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setPriority(priority)
                .setAutoCancel(true);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, id
                , intent, PendingIntent.FLAG_CANCEL_CURRENT);
        notificationBuild.setContentIntent(pendingIntent);
        // 正式发出通知
        manager.notify(id, notificationBuild.build());
    }

    static void addMsgToHistory(Context context, MessageBean messageBean) {
        SharedPreferences preferences = context.getSharedPreferences("history_list_preference"
                , Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        ArrayList<MessageBean> list = stringToMap(preferences.getString(Constant.KEY_HISTORY_LIST, ""));
        if (list.size() >= 100) {
            while (list.size() >= 100) {
                list.remove(list.size() - 1);
            }
        }
        list.add(0, messageBean);
        editor.putString(Constant.KEY_HISTORY_LIST, mapToString(list)).apply();
    }

    static ArrayList<MessageBean> getMsgHistory(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("history_list_preference"
                , Context.MODE_PRIVATE);
        return stringToMap(preferences.getString(Constant.KEY_HISTORY_LIST, ""));
    }

    private static String mapToString(ArrayList<MessageBean> list) {
        JSONArray jsonArray = new JSONArray();
        for (MessageBean entry : list) {
            JSONObject itemJson = new JSONObject();
            try {
                itemJson.put("time", entry.getTime());
                itemJson.put("title", entry.getTitle());
                itemJson.put("context", entry.getContext());
                jsonArray.put(itemJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return jsonArray.toString();
    }

    private static ArrayList<MessageBean> stringToMap(String string) {
        ArrayList<MessageBean> list = new ArrayList<>();
        if (!TextUtils.isEmpty(string)) {
            try {
                JSONArray jsonArray = new JSONArray(string);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject itemJson = jsonArray.getJSONObject(i);
                    MessageBean itemBean = new MessageBean();
                    itemBean.setTime(itemJson.optLong("time"));
                    itemBean.setTitle(itemJson.optString("title"));
                    itemBean.setContext(itemJson.optString("context"));
                    list.add(itemBean);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return list;
    }
}
