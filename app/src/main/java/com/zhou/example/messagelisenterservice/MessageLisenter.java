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
import com.zhou.netlogutil.handler.NetLogConfig;
import com.zhou.netlogutil.socket.SocketCallback;
import com.zhou.netlogutil.socket.ThreadUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by user68 on 2018/7/30.
 * 消息监听
 */

public class MessageLisenter extends NotificationListenerService implements Handler.Callback {
    private final String TAG = "LogUtils";

    private final ArrayList<MessageEnty> ntfMsgList = new ArrayList<>();
    // 暂存一下空的消息的包名和id
    private final ArrayList<MessageEnty> tmpNullMsgList = new ArrayList<>();
    //    private final ArrayList<String> uploadMsg = new ArrayList<>();
//    private long uploadTime = -1;
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
    private boolean waitBatteryNotify = false;
    private long closeNotifyTime = -1;
    private long enterForegroundTime = -1;

    private int reconnectCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        enterForeground();
        InitPlay();
        registerHomeBroad();
        reloadConfig();
        if (!StartReceive.isBootCompleted
                && !Utils.checkBatteryWhiteList(this)) {
            Utils.sendBatteryNotify(this);
        }
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
        clearNfSbnAndStopSound();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "message listen server destroy");
        super.onDestroy();
        clearNfSbnAndStopSound();
        unregisterHomeBroad();
        cleanPlay();
        NetLogUtil.flush(1000);
    }

    private void enterForeground() {
        enterForeground(-1, null, null);
    }

    private void enterForeground(int type, String title, String text) {
        Log.i(TAG, "enter fore ground");
        enterForegroundTime = SystemClock.elapsedRealtime();
        Intent intent = new Intent(this, ForegroundServer.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ForegroundServer.GET_SERVER_TYPE_KEY, type);
        intent.putExtra(ForegroundServer.GET_NOTIFY_TITLE, title == null ? "" : title);
        intent.putExtra(ForegroundServer.GET_NOTIFY_TEXT, text == null ? "" : text);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

    }

    private void enterForegroundActivity() {
        exitForegroundActivity();
        enterForegroundTime = SystemClock.elapsedRealtime();
        Intent startActivityIntent = new Intent(this, ForegroundActivity.class);
        startActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startActivityIntent);
    }

    private void exitForeground() {
        Log.i(TAG, "exitForeground");

        Intent intent1 = new Intent();
        intent1.setAction(Constant.FINISH_FOREGROUND_SERVICE);
        sendBroadcast(intent1);
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
        InitNetLog(config);
    }

    private void InitNetLog(ConfigEntry configEntry) {
        if (TextUtils.isEmpty(configEntry.getNetLogUrl())) {
            NetLogUtil.disconnect();
            return;
        }
        NetLogConfig config = NetLogUtil.buildConfig();
        config.configTitleKey(TextUtils.isEmpty(configEntry.getAccount()) ? "title" : configEntry.getAccount());
        config.configMessageKey(TextUtils.isEmpty(configEntry.getFilename()) ?
                "message" : configEntry.getFilename());
        config.configUrl(configEntry.getNetLogUrl());
        config.configMinReconnectTime(5000);
        config.configConnectTimeout(5000);
        config.configReconnectTime(10000);
        config.configMaxPoolSize(100);
        config.configSocketCallback(new SocketCallback() {
            boolean isForeground = false;

            @Override
            public void onSend() {
                if (isForeground) {
                    isForeground = false;
                    exitForeground();
                }
                reconnectCount = 0;
            }

            @Override
            public void onDisconnect() {
            }

            @Override
            public void onReconnect() {
                if (reconnectCount < Integer.MAX_VALUE) {
                    reconnectCount++;
                }
                //小米手机如果长时间不前台，则导致无法重连网络
                if (reconnectCount > 3
                        && (SystemClock.elapsedRealtime() - enterForegroundTime > 3 * 60 * 60 * 1000)
                        && getNetIsConnect() != 0) {
                    isForeground = true;
                    reconnectCount = 0;
                    enterForeground();
                }
            }
        });
        NetLogUtil.connect(this, config);
    }

    private int getNetIsConnect() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = null;
        if (connectivityManager != null) {
            info = connectivityManager.getActiveNetworkInfo();
        }
        if (info == null) {
            return -1;
        }
        return info.getState() == NetworkInfo.State.CONNECTED ? 1 : 0;
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
            if (startPlaySound()) {
                startLockActivity();
            }

        }
    }

    private boolean startPlaySound() {
        if (!handler.hasMessages(looperWhat)) {
            // 用户勾选了暂停后指定时间不提示，而通过判断，它确实没有到达指定时间，则暂停通知，且等待电池状态改变后唤起
            if (ConfigEntry.getInstance().isClosePauseNotify()
                    && !((SystemClock.elapsedRealtime() - (closeNotifyTime)) > config.getPauseNotifyTime())) {
                waitBatteryNotify = true;
                return false;
            }
            waitBatteryNotify = false;
            startLooper(0, battery);
        }
        return true;
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
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Constant.GET_MESSAGE_KEY, getShowMessage());
            startActivity(intent);
        } else {
            updataMessageData();
        }
    }

    private void finishLockActivity(int type) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Constant.FINISH_LOCK_SHOW_ACTIVITY);
        sendIntent.putExtra(LockShowActivity.GET_SHOW_ACTIVITY_TYPE, type);
        sendBroadcast(sendIntent);
    }

    private String getShowMessage() {
        String filterText = PreUtils.get(this, Constant.TITLE_FILTER_KEY, null);
        if (TextUtils.isEmpty(filterText)) {
            filterText = PreUtils.get(this, Constant.APP_PACKAGE_KEY, null);
        }
        if (TextUtils.isEmpty(filterText)) {
            filterText = String.format(getString(R.string.filter_text)
                    , PreUtils.get(this, Constant.MESSAGE_FILTER_KEY, "[NULL]"));
        }

        return String.format(getString(R.string.show_message_count),
                filterText
                , ntfMsgList.size());
    }

    private void updataMessageData() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Constant.UPDATA_MESSAGE_DATA_ACTION);
        sendIntent.putExtra(Constant.GET_MESSAGE_KEY, getShowMessage());
        sendBroadcast(sendIntent);
    }

    private void sendOtherMessageData(String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(message);
            String account = jsonObject.getString("account");
            if (TextUtils.isEmpty(account) || !account.equals(config.getAccount())) {
                return;
            }
            int type = jsonObject.optInt("type", 0);
            String title = jsonObject.getString("title");
            String text = jsonObject.getString("text");
            MessageBean messageBean = new MessageBean();
            messageBean.setTime(System.currentTimeMillis());
            messageBean.setTitle(title);
            messageBean.setContext(text);
            Utils.addMsgToHistory(this, messageBean);
            if (type > 0) {
                Utils.sendNotifyMessage(this, title, text, type);
                return;
            }
            String showTvText = String.format("%s\n%s\n%s", account, title, text);

            Intent intent = new Intent(this, LockShowActivity.class);
            intent.putExtra(Constant.GET_MESSAGE_KEY, showTvText);
            intent.putExtra(LockShowActivity.GET_SHOW_ACTIVITY_TYPE, 0);
            //            其它消息不受限制
            enterForeground(0, title, text);
            closeNotifyTime = -1;
            startPlaySound();
            startActivity(intent);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void tryStopPlaysound(StatusBarNotification sbns) {
        String pkgName = sbns.getPackageName();
        int sbnId = sbns.getId();
        removeNtfSbn(tmpNullMsgList, pkgName, sbnId);
        if (!hasMessage(ntfMsgList)) {
            return;
        }
        removeNtfSbn(ntfMsgList, pkgName, sbnId);
        if (!handler.hasMessages(looperWhat)) return;
        if (!hasMessage(ntfMsgList)) {
            if (config.isPauseApplyToAllPage())
                closeNotifyTime = SystemClock.elapsedRealtime();
            stopPlaySound();
        }
    }

    private void stopPlaySound() {
        stopPlaySound(-1);
    }

    private void stopPlaySound(int type) {
        finishLockActivity(type);

        if (type == -1 || type == 0) {
            handler.removeMessages(looperWhat);
            stopNotify(rt, vibrator);
        }
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
//        filter.addAction(Constant.RECEIVE_SOCKET_MESSAGE_ACTION);
//        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(sysBoardReceiver, filter);
    }

    private void unregisterHomeBroad() {
        unregisterReceiver(sysBoardReceiver);
    }

    private void writeNotifyToFile(StatusBarNotification sbn) {
        if (!config.isCancelable() && !sbn.isClearable())
            return;

        Log.i(TAG, "write notify message to file");
        //            具有写入权限，否则不写入
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

        String time = Utils.formatTime(Calendar.getInstance().getTime());

        String writText = "\n" + "[" + time + "]" + "[" + packageName + "]" + "\n" +
                "[" + notificationTitle + "]" + "\n" + "[" + notificationText + "]" + "\n" +
                "[" + subText + "]" + "\n";

        // 使用 post 异步的写入
        ThreadUtils.workPost(() -> {
            Utils.putStr(MessageLisenter.this, writText);
        });

        //如果不位于前台，则添加到列表中
        //如果处于前台，则直接清空队列并上传
        if (!TextUtils.isEmpty(config.getNetLogUrl())) {
            String simpleTime = Utils.formatTimeSimple(Calendar.getInstance().getTime());
            String notifyStr = "[" + notificationTitle + "]" + "[" + simpleTime + "]"
                    + "\n" + "[" + notificationText + "]";
            if (subText != null) {
                notifyStr += "\n" + "[" + subText + "]";
            }
            NetLogUtil.log(packageName, notifyStr);
        }
    }

    private boolean isEmptyMsg(CharSequence ntTitle, CharSequence ntText, CharSequence subText) {
        return TextUtils.isEmpty(ntTitle)
                && TextUtils.isEmpty(ntText)
                && TextUtils.isEmpty(subText);
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
        switch (msg.what) {
            case looperWhat:
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
        return false;
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
                Log.i(TAG, "receive close system dialog action");
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
                Log.i(TAG, "receive bettery change action");
                battery = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100);
                if (!waitBatteryNotify)
                    return;

                if (hasMessage(ntfMsgList) && battery > 20
                        && !handler.hasMessages(looperWhat)) {
                    if (startPlaySound()) {
                        startLockActivity();
                    }
                }
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                Log.i(TAG, "receive screen off action");
                startLockActivity = true;
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                Log.i(TAG, "receive screen on action");
                startLockActivity = false;
            } else if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                Log.i(TAG, "receive user presend action");
                startLockActivity = false;
                finishLockActivity(-1);
            } else if (Constant.CLOSE_ACTIVITY_STOP_NOTIFY_ACTION.equals(intent.getAction())) {
                Log.i(TAG, "receive close activity action");
                //当锁屏页面的activity 被关闭时，暂停通知
                closeNotifyTime = SystemClock.elapsedRealtime();
                stopPlaySound(intent.getIntExtra(LockShowActivity.GET_SHOW_ACTIVITY_TYPE, -1));
            }
        }
    };
}
