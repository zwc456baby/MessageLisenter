package com.zhou.example.messagelisenterservice;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.widget.Toast;

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
import java.util.Locale;

/**
 * Created by user68 on 2018/7/30.
 * 消息监听
 */

public class MessageLisenter extends NotificationListenerService implements Handler.Callback {

    private final File notifycationFilePath = new File(Environment.getExternalStorageDirectory().getAbsolutePath());

    private final String dayType = "yyyy-MM-dd-HH:mm:ss";
    private final DateFormat dataFormat = new SimpleDateFormat(dayType, Locale.getDefault());
    private final ArrayList<MessageEnty> ntfMsgList = new ArrayList<>();
    // 暂存一下空的消息的包名和id
    private final ArrayList<MessageEnty> tmpNullMsgList = new ArrayList<>();
    //    配置相关
    private String packageFilter;
    private String titleFilter;
    private String msgFilter;
    private boolean playMusic;
    private boolean zhenDong;
    private boolean cancelable;
    private long sleepTime;

    //震动和唤醒
    private int battery = 100;
    private PowerManager pm;

    private Ringtone rt;
    private Vibrator vibrator;

    private final Handler handler = new Handler(this);
    private final int looperWhat = 0;

    private boolean startLockActivity = false;

    @Override
    public void onCreate() {
        super.onCreate();
        InitPlay();
        registerHomeBroad();
        reloadConfig();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "启动服务成功", Toast.LENGTH_SHORT).show();
        reloadConfig();
        clearNfSbnAndStopSound();
        checkAllNotify(getActiveNotifications());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        writeNotifyToFile(sbn);
        tryStartPlaySound(sbn);
    }


    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        tryStopPlaysound(sbn);
    }


    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        clearNfSbnAndStopSound();
        checkAllNotify(getActiveNotifications());
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        clearNfSbnAndStopSound();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clearNfSbnAndStopSound();
        unregisterHomeBroad();
        cleanPlay();
    }

    private void checkAllNotify(StatusBarNotification[] notifications) {
        for (StatusBarNotification sbn : notifications)
            tryStartPlaySound(sbn);
    }

    protected static String getConfigFilePath(Context context) {
        String configName = "config.json";
        File dataRootFile = context.getExternalFilesDir("config");
        return dataRootFile == null ? null : dataRootFile.getAbsolutePath() + File.separator + configName;
    }

    private void reloadConfig() {
        String configFilePath = getConfigFilePath(this);
        FileUtils fileUtils = new FileUtils();
        File configFile = configFilePath == null ? null : new File(configFilePath);
        if (configFilePath == null || !configFile.exists() || configFile.length() == 0) {
            loadConfigFroXml();
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
        } else {
            JSONObject rootJson = null;
            try {
                String fileStr = fileUtils.readFileToString(configFilePath);
                if (!TextUtils.isEmpty(fileStr)) rootJson = new JSONObject(fileStr);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (rootJson == null) {
                loadConfigFroXml();
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

            putConfigToXml(this, packageFilter, titleFilter, msgFilter,
                    playMusic, zhenDong, cancelable, sleepTime);
        }
    }

    private void loadConfigFroXml() {
        packageFilter = PreUtils.get(this, Constant.APP_PACKAGE_KEY, "abcdefg");
        titleFilter = PreUtils.get(this, Constant.TITLE_FILTER_KEY, null);
        msgFilter = PreUtils.get(this, Constant.MESSAGE_FILTER_KEY, null);
        playMusic = PreUtils.get(this, Constant.PLAY_MUSIC_KEY, true);
        zhenDong = PreUtils.get(this, Constant.PLAY_ZHENGDONG_KEY, false);
        cancelable = PreUtils.get(this, Constant.CANCEL_ABLE_KEY, true);
        sleepTime = PreUtils.get(this, Constant.PLAY_SLEEP_TIME_KEY, 4000L);
        if (sleepTime < 500) sleepTime = 500;
    }

    public static void putConfigToXml(Context context, String appPackage, String titleFilter, String messageFilter,
                                      boolean playMusic, boolean zhengDong, boolean cancelable, long sleepTime) {
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
    }

    private void clearNfSbnAndStopSound() {
        ntfMsgList.clear();
        tmpNullMsgList.clear();
        if (handler.hasMessages(looperWhat)) stopPlaySound();
    }

    private boolean isNtfMessage(MessageEnty enty, String packageName, int Id) {
        return enty.pkgName.equals(packageName)
                && enty.ntfId == Id;
    }

    private boolean isSettingMessage(StatusBarNotification sbn) {
        if (!(TextUtils.isEmpty(packageFilter)
                || packageFilter.equals(sbn.getPackageName())))
            return false;

        if (!cancelable && !sbn.isClearable())
            return false;

        Bundle extras = sbn.getNotification().extras;
        if (extras == null) return false;
        CharSequence notificationTitle = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence notificationTextChar = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);

        if (!TextUtils.isEmpty(titleFilter)) {
            if (TextUtils.isEmpty(notificationTitle))
                return false;
            if (!notificationTitle.toString().contains(titleFilter))
                return false;
        }
        if (!TextUtils.isEmpty(msgFilter)) {
            String notificationText;
            if (!TextUtils.isEmpty(notificationTextChar)) {
                if (!TextUtils.isEmpty(subText))
                    notificationText = notificationTextChar.toString() + subText.toString();
                else
                    notificationText = notificationTextChar.toString();
            } else if (!TextUtils.isEmpty(subText))
                notificationText = subText.toString();
            else return false;
            return notificationText.contains(msgFilter);
        }
        return true;
    }

    private void tryStartPlaySound(StatusBarNotification sbn) {
        if (isSettingMessage(sbn))
            startPlaySound(sbn);
    }

    private void startPlaySound(StatusBarNotification sbn) {
        addNtfMessage(ntfMsgList, sbn.getPackageName(), sbn.getId());
        if (!handler.hasMessages(looperWhat))
            startLooper(0, battery);
        startLockActivity();
    }

    private void startLooper(long delay, int battery) {
        Message msg = handler.obtainMessage(looperWhat, battery);
        if (delay > 0) {
            handler.sendMessageDelayed(msg, delay);
            if (pm != null)
                pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "messagelisenter:wakelock").acquire(Math.round(sleepTime * 1.5));
        } else {
            handler.sendMessage(msg);
        }
    }

    private void startLockActivity() {
        if (startLockActivity) {
            Intent intent = new Intent(this, LockShowActivity.class);
            intent.putExtra(Constant.GET_MESSAGE_LENGTH, ntfMsgList.size());

            startActivity(intent);
        } else
            updataMessageData();
    }

    private void finishLockActivity() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Constant.FINISH_LOCK_SHOW_ACTIVITY);
        sendBroadcast(sendIntent);
    }

    private void updataMessageData() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Constant.UPDATA_MESSAGE_DATA_ACTION);
        sendIntent.putExtra(Constant.GET_MESSAGE_LENGTH, ntfMsgList.size());
        sendBroadcast(sendIntent);
    }

    private void tryStopPlaysound(StatusBarNotification sbns) {
        String pkgName = sbns.getPackageName();
        int sbnId = sbns.getId();
        removeNtfSbn(ntfMsgList, pkgName, sbnId);
        removeNtfSbn(tmpNullMsgList, pkgName, sbnId);
        if (!handler.hasMessages(looperWhat)) return;
        if (!hasMessage(ntfMsgList)) stopPlaySound();
    }

    private void stopPlaySound() {
        finishLockActivity();

        handler.removeMessages(looperWhat);
        stopNotify(rt, vibrator);
    }

    /*
     * 播放一次系统提示音
     * */
    private void playNotifySound(Ringtone rt) {
        rt.play();
    }

    private void stopPlaySoundMusic(Ringtone rt) {
        if (rt.isPlaying())
            rt.stop();
    }

    /*
     * 震动
     * */
    private void zhengDong(Vibrator vibrator) {
        long vbtime = sleepTime / 4;
        if (vbtime > 1000) vbtime = 1000;
        long[] pattern = new long[]{vbtime, vbtime, vbtime, vbtime};
        if (vibrator != null)
            vibrator.vibrate(pattern, -1);
    }

    private void stopZhengDong(Vibrator vibrator) {
        if (vibrator != null && vibrator.hasVibrator())
            vibrator.cancel();
    }

    private void stopNotify(Ringtone rt, Vibrator vibrator) {
        stopPlaySoundMusic(rt);
        stopZhengDong(vibrator);
    }

    private void registerHomeBroad() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(sysBoardReceiver, filter);
    }

    private void unregisterHomeBroad() {
        unregisterReceiver(sysBoardReceiver);
    }

    private void writeNotifyToFile(StatusBarNotification sbn) {
        //            具有写入权限，否则不写入
        if (!notifycationFilePath.canWrite()) return;
        CharSequence notificationTitle = null;
        CharSequence notificationText = null;
        CharSequence subText = null;

        Bundle extras = sbn.getNotification().extras;
        if (extras != null) {
            notificationTitle = extras.getCharSequence(Notification.EXTRA_TITLE);
            notificationText = extras.getCharSequence(Notification.EXTRA_TEXT);
            subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
        }
        String packageName = sbn.getPackageName();
        int sbnId = sbn.getId();
        if (listHasNtfMsg(tmpNullMsgList, packageName, sbnId)) {
            if (isEmptyMsg(notificationTitle, notificationText, subText)) return;
            else removeNtfSbn(tmpNullMsgList, packageName, sbnId);
        } else if (isEmptyMsg(notificationTitle, notificationText, subText))
            addNtfMessage(tmpNullMsgList, packageName, sbnId);

        String time = dataFormat.format(Calendar.getInstance().getTime());

        String writText = "\n" + "[" + time + "]" + "[" + packageName + "]" + "\n" +
                "[" + notificationTitle + "]" + "\n" + "[" + notificationText + "]" + "\n" +
                "[" + subText + "]" + "\n";

        putStr(writText);
    }

    private boolean isEmptyMsg(CharSequence ntTitle, CharSequence ntText, CharSequence subText) {
        return TextUtils.isEmpty(ntTitle)
                && TextUtils.isEmpty(ntText)
                && TextUtils.isEmpty(subText);
    }


    private void putStr(String value) {
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

    private void addNtfMessage(ArrayList<MessageEnty> list, String pkgName, int Id) {
        if (listHasNtfMsg(list, pkgName, Id)) return;
        list.add(new MessageEnty(pkgName, Id));
    }

    private boolean listHasNtfMsg(ArrayList<MessageEnty> list, String pkgName, int Id) {
        if (!hasMessage(list)) return false;
        for (MessageEnty entyItem : list)
            if (isNtfMessage(entyItem, pkgName, Id)) return true;
        return false;
    }

    private boolean hasMessage(ArrayList<MessageEnty> list) {
        return list.size() > 0;
    }

    private void removeNtfSbn(ArrayList<MessageEnty> list, String pkgName, int Id) {
        if (!hasMessage(list)) return;
        ArrayList<MessageEnty> removeList = null;
        for (MessageEnty entyItem : list) {
            if (isNtfMessage(entyItem, pkgName, Id)) {
                if (removeList == null)
                    removeList = new ArrayList<>();
                removeList.add(entyItem);
            }
        }
        if (removeList != null) list.removeAll(removeList);
    }

    private void InitPlay() {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        rt = RingtoneManager.getRingtone(getApplicationContext(), uri);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        pm = (PowerManager) getSystemService(POWER_SERVICE);
    }

    private void cleanPlay() {
        rt = null;
        vibrator = null;
        pm = null;
    }

    @Override
    public boolean handleMessage(Message msg) {
//        if (!hasMessage()) return true;
        int startBattery = (int) (msg.obj == null ? -1 : msg.obj);
        if (!playMusic && !zhenDong || !(battery > 15)
                || (startBattery != -1 && (startBattery - battery > 5))) { // 判断耗电量，超过 %5 则不再提示
            finishLockActivity();
            return true;
        }

        startLooper(sleepTime, startBattery);

        if (playMusic) playNotifySound(rt);
        if (zhenDong) zhengDong(vibrator);

        return true;
    }

    private class MessageEnty {
        private String pkgName;
        private int ntfId;

        private MessageEnty(String pkgName, int Id) {
            this.pkgName = pkgName;
            this.ntfId = Id;
        }
    }

    private BroadcastReceiver sysBoardReceiver = new BroadcastReceiver() {
        private long clickTime = 0;
        private int clickCount = -1;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(intent.getAction())) {
                if (clickTime > SystemClock.elapsedRealtime() - 1000)
                    clickCount++;
                else
                    clickCount = 0;
                clickTime = SystemClock.elapsedRealtime();
                if (clickCount >= 6) {
                    clickCount = -1;
                    clickTime = 0;
                    Toast.makeText(context, "消息监听服务运行中", Toast.LENGTH_SHORT).show();
                }
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                int tmpBattery = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100);
                if (tmpBattery <= battery) {
                    battery = tmpBattery;
                    return; //电量在减少，不做任何操作
                }
                battery = tmpBattery;
                if (hasMessage(ntfMsgList) && battery > 20 && !handler.hasMessages(looperWhat)) {
                    startLooper(0, battery);
                    startLockActivity();
                }
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                startLockActivity = true;
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                startLockActivity = false;
            } else if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                startLockActivity = false;
                finishLockActivity();
            }
        }
    };
}
