package com.zhou.example.messagelisenterservice;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by hasee on 2017/6/10.
 * 配置类
 */
class PreUtils {
    private static final String User = "user_preference";

    public static boolean put(Context context, String key, long value) {
        SharedPreferences preferences = context.getSharedPreferences(User, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(key, value);
        return editor.commit();
    }

    public static long get(Context context, String key, long defaultLong) {
        SharedPreferences preferences = context.getSharedPreferences(User, Context.MODE_PRIVATE);
        return preferences.getLong(key, defaultLong);
    }
    public static boolean put(Context context, String key, String value) {
        SharedPreferences preferences = context.getSharedPreferences(User, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    public static String get(Context context, String key, String defaultStr) {
        SharedPreferences preferences = context.getSharedPreferences(User, Context.MODE_PRIVATE);
        return preferences.getString(key, defaultStr);
    }

    public static boolean put(Context context, String key, boolean value) {
        SharedPreferences preferences = context.getSharedPreferences(User, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        return editor.commit();
    }

    public static boolean get(Context context, String key, boolean defaultBoo) {
        SharedPreferences preferences = context.getSharedPreferences(User, Context.MODE_PRIVATE);
        return preferences.getBoolean(key, defaultBoo);
    }
}
