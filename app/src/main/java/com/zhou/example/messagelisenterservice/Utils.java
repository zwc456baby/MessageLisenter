package com.zhou.example.messagelisenterservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.util.Date;
import java.util.Locale;

import static android.content.Context.NOTIFICATION_SERVICE;

class Utils {

    private final static String dayType = "yyyy-MM-dd HH:mm:ss";

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
