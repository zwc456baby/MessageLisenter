package com.zhou.example.messagelisenterservice;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * Created by user68 on 2018/7/30.
 * <p>
 * 接收另一个app的广播启动本地服务
 */

public class StartReceive extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Constant.START_SETTING_ACTIVITY_ACTION.equals(intent.getAction())) {
            Intent startActivityIntent = new Intent(context, SettingActivity.class);
            startActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(startActivityIntent);
            return;
        }
        if (Constant.REMOTE_SERVICE_START_ACTION.equals(intent.getAction())) {
            if (intent.getIntExtra(Constant.KILL_SERVICE_TAG_KEY, -1)
                    == Constant.KILL_SERVICE_TAG) {
                Intent intent1 = new Intent(context, MessageLisenter.class);
                context.stopService(intent1);
                killProcess();
                return;
            }
            ConfigEntry configEntry = ConfigEntry.getInstance();
            
            configEntry.setConfig(intent);
            configEntry.writeConfig(context);
        }
        Intent intent1 = new Intent(context, MessageLisenter.class);
        context.stopService(intent1);

        intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(intent1);
        toggleNotificationListenerService(context);
    }

    private void killProcess() {
        System.exit(0);
//        android.os.Process.killProcess(android.os.Process.myPid());
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
