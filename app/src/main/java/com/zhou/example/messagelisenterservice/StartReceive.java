package com.zhou.example.messagelisenterservice;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import java.io.File;

/**
 * Created by user68 on 2018/7/30.
 * <p>
 * 接收另一个app的广播启动本地服务
 */

public class StartReceive extends BroadcastReceiver {

    private final static String ACTION_BOOT = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Constant.REMOTE_SERVICE_START_ACTION.equals(intent.getAction())
                && !ACTION_BOOT.equals(intent.getAction())) {
            return;
        }

        if (Constant.REMOTE_SERVICE_START_ACTION.equals(intent.getAction())) {
            if (intent.getIntExtra(Constant.KILL_SERVICE_TAG_KEY, -1)
                    == Constant.KILL_SERVICE_TAG) {
                killProcess();
                return;
            }

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

            PreUtils.put(context, Constant.APP_PACKAGE_KEY,
                    appPackage);
            PreUtils.put(context, Constant.TITLE_FILTER_KEY,
                    titleFilter);
            PreUtils.put(context, Constant.MESSAGE_FILTER_KEY,
                    messageFilter);
            PreUtils.put(context, Constant.PLAY_MUSIC_KEY,
                    playMusic);
            PreUtils.put(context, Constant.PLAY_ZHENGDONG_KEY,
                    zhengDong);
            PreUtils.put(context, Constant.CANCEL_ABLE_KEY,
                    cancelable);

            PreUtils.put(context, Constant.PLAY_SLEEP_TIME_KEY, sleepTime);

            String configFilePath = MessageLisenter.getConfigFilePath(context);
            File configFile = configFilePath == null ? null : new File(configFilePath);
            if (configFile != null && configFile.exists()) {
                FileUtils fileUtils = new FileUtils();
                fileUtils.deleteFileSafely(configFile);
            }
        }
        Intent intent1 = new Intent(context, MessageLisenter.class);
        context.stopService(intent1);

        context.startService(intent1);
        toggleNotificationListenerService(context);
    }

    private void killProcess() {
        System.exit(0);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void toggleNotificationListenerService(Context context) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(
                new ComponentName(context, MessageLisenter.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        pm.setComponentEnabledSetting(
                new ComponentName(context, MessageLisenter.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }
}
