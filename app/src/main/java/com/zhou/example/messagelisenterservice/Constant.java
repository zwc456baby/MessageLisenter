package com.zhou.example.messagelisenterservice;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by user68 on 2018/7/30.
 */

@SuppressWarnings({"unused", "WeakerAccess"})
public class Constant {

    public final static String dayType = "yyyy-MM-dd HH:mm:ss";

    public final static String APP_PACKAGE_KEY = "app_package_key";
    public final static String TITLE_FILTER_KEY = "title_filter_key";
    public final static String MESSAGE_FILTER_KEY = "message_filter_key";
    public final static String PLAY_MUSIC_KEY = "play_music_key";
    public final static String PLAY_ZHENGDONG_KEY = "play_zhengdong_key";
    public final static String PLAY_SLEEP_TIME_KEY = "play_sleep_time_key";
    public final static String KILL_SERVICE_TAG_KEY = "kill_service_tag_key";
    public final static String CANCEL_ABLE_KEY = "cancel_able_key";
    public final static String CLOSE_PAUSE_NOTIFY_ENABLE_KEY = "close_pause_notify_enable_key";
    public final static String PAUSE_NOTIFY_TIME_KEY = "pause_notify_time_key";
    public final static String PAUSE_ALL_PAGE_ENABLE_KEY = "pause_all_page_enable_key";

    public final static String GET_MESSAGE_LENGTH = "get_message_length";

    public final static String FINISH_LOCK_SHOW_ACTIVITY = "finish_lock_show_activity";
    public final static String UPDATA_MESSAGE_DATA_ACTION = "updata_message_data_action";
    public final static String CLOSE_ACTIVITY_STOP_NOTIFY_ACTION = "close_activity_stop_notify_action";

    public static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

    //    启动远程服务的广播事件
    public static final String REMOTE_SERVICE_START_ACTION = "com.zhou.example.messagelisenterservice.START_SERVICE";

    public static final String START_SETTING_ACTIVITY_ACTION = "android.provider.Telephony.SECRET_CODE";

    public final static int KILL_SERVICE_TAG = 1;

    public final static int LIST_PAGE_ITEM = 100;
}
