package com.zhou.example.messagelisenterservice;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

@SuppressWarnings({"unused", "WeakerAccess"})
public class ConfigEntry {
    private String packageFilter;
    private String titleFilter;
    private String msgFilter;
    private boolean playMusic;
    private boolean zhenDong;
    private boolean cancelable;
    private long sleepTime;

    private ConfigEntry() {
    }

    private static class ConfigEntryInstant {
        private final static ConfigEntry instant = new ConfigEntry();
    }

    public static ConfigEntry getInstance() {
        return ConfigEntryInstant.instant;
    }

    public void InitConfig(Context context) {
        String configFilePath = getConfigFilePath(context);
        File configFile = configFilePath == null ? null : new File(configFilePath);
        if (configFilePath == null || !configFile.exists() || configFile.length() == 0) {
            loadConfigFroXml(context);
            writeConfigToFile(context);
        } else {
            loadConfigFromFile(context);
            writeConfigToXml(context);
        }
    }

    public void writeConfig(Context context) {
        writeConfigToFile(context);
        writeConfigToXml(context);
    }

    private String getConfigFilePath(Context context) {
        String configName = "config.json";
        File dataRootFile = context.getExternalFilesDir("config");
        return dataRootFile == null ? null : dataRootFile.getAbsolutePath() + File.separator + configName;
    }

    private void loadConfigFromFile(Context context) {
        String configFilePath = getConfigFilePath(context);
        FileUtils fileUtils = new FileUtils();
        JSONObject rootJson = null;
        try {
            String fileStr = fileUtils.readFileToString(configFilePath);
            if (!TextUtils.isEmpty(fileStr)) rootJson = new JSONObject(fileStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (rootJson == null) {
            loadConfigFroXml(context);
            return;
        }
        packageFilter = rootJson.optString(Constant.APP_PACKAGE_KEY, "abcdefg");
        titleFilter = rootJson.optString(Constant.TITLE_FILTER_KEY, null);
        msgFilter = rootJson.optString(Constant.MESSAGE_FILTER_KEY, null);
        playMusic = rootJson.optBoolean(Constant.PLAY_MUSIC_KEY, true);
        zhenDong = rootJson.optBoolean(Constant.PLAY_ZHENGDONG_KEY, false);
        cancelable = rootJson.optBoolean(Constant.CANCEL_ABLE_KEY, true);
        sleepTime = rootJson.optLong(Constant.PLAY_SLEEP_TIME_KEY, 4000L);
        if (sleepTime < 500) sleepTime = 500;

    }

    private void writeConfigToFile(Context context) {
        String configFilePath = getConfigFilePath(context);
        FileUtils fileUtils = new FileUtils();
        if (configFilePath == null) return;
        try {
            JSONObject rootJson = new JSONObject();
            rootJson.put(Constant.APP_PACKAGE_KEY, packageFilter);
            rootJson.put(Constant.TITLE_FILTER_KEY, titleFilter);
            rootJson.put(Constant.MESSAGE_FILTER_KEY, msgFilter);
            rootJson.put(Constant.PLAY_MUSIC_KEY, playMusic);
            rootJson.put(Constant.PLAY_ZHENGDONG_KEY, zhenDong);
            rootJson.put(Constant.CANCEL_ABLE_KEY, cancelable);
            rootJson.put(Constant.PLAY_SLEEP_TIME_KEY, sleepTime);

            fileUtils.putStringToFile(configFilePath, rootJson.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadConfigFroXml(Context context) {
        packageFilter = PreUtils.get(context, Constant.APP_PACKAGE_KEY, "abcdefg");
        titleFilter = PreUtils.get(context, Constant.TITLE_FILTER_KEY, null);
        msgFilter = PreUtils.get(context, Constant.MESSAGE_FILTER_KEY, null);
        playMusic = PreUtils.get(context, Constant.PLAY_MUSIC_KEY, true);
        zhenDong = PreUtils.get(context, Constant.PLAY_ZHENGDONG_KEY, false);
        cancelable = PreUtils.get(context, Constant.CANCEL_ABLE_KEY, true);
        sleepTime = PreUtils.get(context, Constant.PLAY_SLEEP_TIME_KEY, 4000L);
        if (sleepTime < 500) sleepTime = 500;
    }

    private void writeConfigToXml(Context context) {
        PreUtils.put(context, Constant.APP_PACKAGE_KEY,
                packageFilter);
        PreUtils.put(context, Constant.TITLE_FILTER_KEY,
                titleFilter);
        PreUtils.put(context, Constant.MESSAGE_FILTER_KEY,
                msgFilter);
        PreUtils.put(context, Constant.PLAY_MUSIC_KEY,
                playMusic);
        PreUtils.put(context, Constant.PLAY_ZHENGDONG_KEY,
                zhenDong);
        PreUtils.put(context, Constant.CANCEL_ABLE_KEY,
                cancelable);

        PreUtils.put(context, Constant.PLAY_SLEEP_TIME_KEY, sleepTime);
    }

    public void setConfig(Intent intent) {
        String appPackage = intent.getStringExtra(Constant.APP_PACKAGE_KEY);
        String titleFilter = intent.getStringExtra(Constant.TITLE_FILTER_KEY);
        String messageFilter = intent.getStringExtra(Constant.MESSAGE_FILTER_KEY);
        boolean playMusic = intent.getBooleanExtra(Constant.PLAY_MUSIC_KEY, true);
        boolean zhengDong = intent.getBooleanExtra(Constant.PLAY_ZHENGDONG_KEY, false);
        boolean cancelable = intent.getBooleanExtra(Constant.CANCEL_ABLE_KEY, true);
        long sleepTime = 4000;
        try {
            sleepTime = Long.valueOf(intent.getStringExtra(Constant.PLAY_SLEEP_TIME_KEY));
        } catch (Exception e) {
            e.printStackTrace();
        }
        setConfig(appPackage, titleFilter, messageFilter,
                playMusic, zhengDong, cancelable, sleepTime);
    }

    private void setConfig(String packageFilter, String titleFilter, String msgFilter
            , boolean playMusic, boolean zhenDong, boolean cancelable, long sleepTime) {
        this.packageFilter = packageFilter;
        this.titleFilter = titleFilter;
        this.msgFilter = msgFilter;
        this.playMusic = playMusic;
        this.zhenDong = zhenDong;
        this.cancelable = cancelable;
        this.sleepTime = sleepTime;
    }

    /************              get and se method *****************/
    public String getPackageFilter() {
        return packageFilter;
    }

    public void setPackageFilter(String packageFilter) {
        this.packageFilter = packageFilter;
    }

    public String getTitleFilter() {
        return titleFilter;
    }

    public void setTitleFilter(String titleFilter) {
        this.titleFilter = titleFilter;
    }

    public String getMsgFilter() {
        return msgFilter;
    }

    public void setMsgFilter(String msgFilter) {
        this.msgFilter = msgFilter;
    }

    public boolean isPlayMusic() {
        return playMusic;
    }

    public void setPlayMusic(boolean playMusic) {
        this.playMusic = playMusic;
    }

    public boolean isZhenDong() {
        return zhenDong;
    }

    public void setZhenDong(boolean zhenDong) {
        this.zhenDong = zhenDong;
    }

    public boolean isCancelable() {
        return cancelable;
    }

    public void setCancelable(boolean cancelable) {
        this.cancelable = cancelable;
    }

    public long getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(long sleepTime) {
        this.sleepTime = sleepTime;
    }
}
