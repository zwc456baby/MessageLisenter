package com.zhou.example.messagelisenterservice;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
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
import android.util.Log;
import android.widget.Toast;

import com.zhou.netlogutil.NetLogUtil;

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
    private final String TAG = "MessageLisenter";
    private final File notifycationFilePath = new File(Environment.getExternalStorageDirectory().getAbsolutePath());

    private final String dayType = "yyyy-MM-dd HH:mm:ss";
    private final DateFormat dataFormat = new SimpleDateFormat(dayType, Locale.getDefault());
    private final ArrayList<MessageEnty> ntfMsgList = new ArrayList<>();
    // 暂存一下空的消息的包名和id
    private final ArrayList<MessageEnty> tmpNullMsgList = new ArrayList<>();
    private final ArrayList<String> uploadMsg = new ArrayList<>();
    private long uploadTime = -1;
    //    配置相关
    private ConfigEntry config;

    //震动和唤醒
    private int battery = 100;
    private PowerManager pm;

    private Ringtone rt;
    private Vibrator vibrator;

    private final Handler handler = new Handler(this);
    private final int looperWhat = 0;

    private boolean startLockActivity = false;
    private boolean isForeground = true;
    private boolean waitBatteryNotify = false;
    private long closeNotifyTime = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        enterForeground();
        InitPlay();
        registerHomeBroad();
        reloadConfig();
        autoStartNetLog();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "message listen start command");
        Toast.makeText(this, "启动服务成功", Toast.LENGTH_SHORT).show();
        reloadConfig();
        clearNfSbnAndStopSound();
        checkAllNotify(getActiveNotifications());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.i(TAG, "message listen post notifycation");
        super.onNotificationPosted(sbn);
        writeNotifyToFile(sbn);
        tryStartPlaySound(sbn);
    }


    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG, "message listen remove notifycation");
        super.onNotificationRemoved(sbn);
        tryStopPlaysound(sbn);
    }


    @Override
    public void onListenerConnected() {
        Log.i(TAG, "message listen connect");
        super.onListenerConnected();

        exitForeground();
        clearNfSbnAndStopSound();
        checkAllNotify(getActiveNotifications());
    }

    @Override
    public void onListenerDisconnected() {
        Log.i(TAG, "message listen disconnect");
        super.onListenerDisconnected();
        uploadMsg();
        clearNfSbnAndStopSound();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "message listen server destroy");
        super.onDestroy();
        uploadMsg();
        clearNfSbnAndStopSound();
        unregisterHomeBroad();
        cleanPlay();
    }

    private void enterForeground() {

        exitForeground();
        Intent intent = new Intent(this, ForegroundServer.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

    }

    private void enterForegroundActivity() {
        exitForegroundActivity();
        Intent startActivityIntent = new Intent(this, ForegroundActivity.class);
        startActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startActivityIntent);

    }

    private void exitForeground() {
        Log.i(TAG, "exitForeground");


        Intent intent1 = new Intent(this, ForegroundServer.class);
        stopService(intent1);
    }

    private void exitForegroundActivity() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Constant.FINISH_FOREGROUND_ACTIVITY);
        sendBroadcast(sendIntent);
    }

    private void checkAllNotify(StatusBarNotification[] notifications) {
        for (StatusBarNotification sbn : notifications)
            tryStartPlaySound(sbn);
    }

    private void reloadConfig() {
        waitBatteryNotify = false;
        closeNotifyTime = -1;
        config = ConfigEntry.getInstance();
        config.InitConfig(this);
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
        if (!(TextUtils.isEmpty(config.getPackageFilter())
                || config.getPackageFilter().equals(sbn.getPackageName())))
            return false;

        if (!config.isCancelable() && !sbn.isClearable())
            return false;

        Bundle extras = sbn.getNotification().extras;
        if (extras == null) return false;
        CharSequence notificationTitle = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence notificationTextChar = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);

        if (!TextUtils.isEmpty(config.getTitleFilter())) {
            if (TextUtils.isEmpty(notificationTitle))
                return false;
            if (!notificationTitle.toString().contains(config.getTitleFilter()))
                return false;
        }
        if (!TextUtils.isEmpty(config.getMsgFilter())) {
            String notificationText;
            if (!TextUtils.isEmpty(notificationTextChar)) {
                if (!TextUtils.isEmpty(subText))
                    notificationText = notificationTextChar.toString() + subText.toString();
                else
                    notificationText = notificationTextChar.toString();
            } else if (!TextUtils.isEmpty(subText))
                notificationText = subText.toString();
            else return false;
            return notificationText.contains(config.getMsgFilter());
        }
        return true;
    }

    private void tryStartPlaySound(StatusBarNotification sbn) {
        if (isSettingMessage(sbn)) {
            addNtfMessage(ntfMsgList, sbn.getPackageName(), sbn.getId());
            startPlaySound();
        }
    }

    private void startPlaySound() {
        if (!handler.hasMessages(looperWhat)) {
            // 用户勾选了暂停后指定时间不提示，而通过判断，它确实没有到达指定时间，则暂停通知，且等待电池状态改变后唤起
            if (ConfigEntry.getInstance().isClosePauseNotify()
                    && !((SystemClock.elapsedRealtime() - (closeNotifyTime)) > config.getPauseNotifyTime())) {
                waitBatteryNotify = true;
                return;
            }
            waitBatteryNotify = false;
            startLooper(0, battery);
        }
        startLockActivity();
    }

    private void startLooper(long delay, int battery) {
        Message msg = handler.obtainMessage(looperWhat, battery);
        if (delay > 0) {
            handler.sendMessageDelayed(msg, delay);
            if (pm != null)
                pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "messagelisenter:wakelock")
                        .acquire(Math.round(config.getSleepTime() * 1.5));
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
        if (!hasMessage(ntfMsgList)) {
            if (config.isPauseApplyToAllPage())
                closeNotifyTime = SystemClock.elapsedRealtime();
            stopPlaySound();
        }
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
        long vbtime = config.getSleepTime() / 4;
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
        filter.addAction(Constant.CLOSE_ACTIVITY_STOP_NOTIFY_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(NetLogUtil.RECONNECT_ACTION);
        filter.addAction(NetLogUtil.CONNECT_ACTION);
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

        //如果不位于前台，则添加到列表中
        //如果处于前台，则直接清空队列并上传
        if (!TextUtils.isEmpty(config.getNetLogUrl())) {
            addMsgAndUpload(writText);
        }
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

    private void addMsgAndUpload(String msg) {
        if (TextUtils.isEmpty(NetLogUtil.getConfig().getUrl())) {
            return;
        }
        if (isForeground && getNetIsConnect() && NetLogUtil.isConnect()) {
            uploadMsg();
            NetLogUtil.log(msg);
        } else {
            uploadMsg.add(msg);
            if (Utils.needUpload(uploadMsg.size(), uploadTime)) {
                if (!isForeground) {
                    enterForeground();
                    uploadMsg();
                    exitForeground();
                } else {
                    uploadMsg();
                }
            }
            //为防止内存泄漏，最大只允许 100 条数据
            if (uploadMsg.size() >= 100) {
                uploadMsg.remove(0);
            }

        }

    }

    private void uploadMsg() {
        if (uploadMsg.size() <= 0) return;
        if (!getNetIsConnect()) {
            return;
        }
        NetLogUtil.resume();
        if (!NetLogUtil.isConnect()) {
            return;
        }
        for (String msg : uploadMsg) {
            NetLogUtil.log(msg);
        }
        uploadTime = SystemClock.elapsedRealtime();
        uploadMsg.clear();
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

    /**
     * 自动判断并启动网络日志
     */
    private void autoStartNetLog() {
        if (getNetIsConnect()) {
            NetLogUtil.resume();
        } else {
            NetLogUtil.pause();
        }
    }

    private boolean getNetIsConnect() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this
                .getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = null;
        if (connectivityManager != null) {
            info = connectivityManager.getActiveNetworkInfo();
        }
        return info != null
                && info.getState() == NetworkInfo.State.CONNECTED;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (!(battery > 15)) {
            waitBatteryNotify = true;
            stopPlaySound();
            return true;
        }

        int startBattery = (int) (msg.obj == null ? -1 : msg.obj);
        if (!config.isPlayMusic() && !config.isZhenDong()
                || (startBattery != -1 && (startBattery - battery > 5))) { // 判断耗电量，超过 %5 则不再提示
            stopPlaySound();
            return true;
        }

        startLooper(config.getSleepTime(), startBattery);

        if (config.isPlayMusic()) playNotifySound(rt);
        if (config.isZhenDong()) zhengDong(vibrator);

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
                battery = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100);
                if (!waitBatteryNotify)
                    return;

                if (hasMessage(ntfMsgList) && battery > 20
                        && !handler.hasMessages(looperWhat)) {
                    startPlaySound();
                }
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                startLockActivity = true;
                isForeground = false;
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                startLockActivity = false;
                isForeground = false;
            } else if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                startLockActivity = false;
                isForeground = true;
                finishLockActivity();
                if (Utils.needUpload(uploadMsg.size(), uploadTime)) {
                    uploadMsg();
                }
            } else if (Constant.CLOSE_ACTIVITY_STOP_NOTIFY_ACTION.equals(intent.getAction())) {
                //当锁屏页面的activity 被关闭时，暂停通知
                closeNotifyTime = SystemClock.elapsedRealtime();
                stopPlaySound();
            } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                NetLogUtil.getConfig().configReconnectTime(5 * 1000);
                autoStartNetLog();
            } else if (NetLogUtil.RECONNECT_ACTION.equals(intent.getAction())) {
//                enterForegroundActivity();
                Utils.resetReconnectTime();

                boolean faild = NetLogUtil.EXTERNAL_FAILD.equals(
                        intent.getStringExtra(NetLogUtil.EXTERNAL_KEY)
                );
                if (faild) {
                    enterForeground();
                }
            } else if (NetLogUtil.CONNECT_ACTION.equals(intent.getAction())) {
                exitForeground();
//                exitForegroundActivity();
            }
        }
    };
}
