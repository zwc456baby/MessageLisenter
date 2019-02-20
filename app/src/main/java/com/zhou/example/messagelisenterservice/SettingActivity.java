package com.zhou.example.messagelisenterservice;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class SettingActivity extends Activity {


    private EditText appNameEdit, titleInEdit, messageInEdit, playSleepTime;

    private Button startBtn, startSettingBtn;

    private CheckBox playMusic, zhengDong, containsService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findView();
        setViewData();
        setViewLisenter();
    }

    private void findView() {
        appNameEdit = findViewById(R.id.input_app_name);
        titleInEdit = findViewById(R.id.input_title_filter);
        messageInEdit = findViewById(R.id.input_message_filter);
        playSleepTime = findViewById(R.id.playSleepTime);

        startBtn = findViewById(R.id.startBtn);
        startSettingBtn = findViewById(R.id.startSettingBtn);

        playMusic = findViewById(R.id.playMusicCheck);
        zhengDong = findViewById(R.id.zhendongCheck);
        containsService = findViewById(R.id.containsService);
    }

    private void setViewData() {
        ConfigEntry configEntry = ConfigEntry.getInstance();
        appNameEdit.setText(configEntry.getPackageFilter());
        titleInEdit.setText(configEntry.getTitleFilter());
        messageInEdit.setText(configEntry.getMsgFilter());
        playSleepTime.setText(String.valueOf(configEntry.getSleepTime()));

        playMusic.setChecked(configEntry.isPlayMusic());
        zhengDong.setChecked(configEntry.isZhenDong());
        containsService.setChecked(configEntry.isCancelable());
    }

    private void setViewLisenter() {
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRemoteService();
            }
        });

        startSettingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoNotificationAccessSetting(SettingActivity.this);
            }
        });
    }

    private void startRemoteService() {
        //包名 包名+类名（全路径）

        Intent intent = new Intent(Constant.REMOTE_SERVICE_START_ACTION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        intent.putExtra(Constant.APP_PACKAGE_KEY, appNameEdit.getText().toString());
        intent.putExtra(Constant.TITLE_FILTER_KEY, titleInEdit.getText().toString());
        intent.putExtra(Constant.MESSAGE_FILTER_KEY, messageInEdit.getText().toString());
        intent.putExtra(Constant.PLAY_SLEEP_TIME_KEY, playSleepTime.getText().toString());

        intent.putExtra(Constant.PLAY_MUSIC_KEY, playMusic.isChecked());
        intent.putExtra(Constant.PLAY_ZHENGDONG_KEY, zhengDong.isChecked());
        intent.putExtra(Constant.CANCEL_ABLE_KEY, containsService.isChecked());

        intent.setComponent(new ComponentName("com.zhou.example.messagelisenterservice",
                "com.zhou.example.messagelisenterservice.StartReceive"));
        sendBroadcast(intent);
    }

    private void gotoNotificationAccessSetting(Context context) {
        try {
            Intent intent;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            } else {
                intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {//普通情况下找不到的时候需要再特殊处理找一次
            try {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.Settings$NotificationAccessSettingsActivity");
                intent.setComponent(cn);
                intent.putExtra(":settings:show_fragment", "NotificationAccessSettings");
                context.startActivity(intent);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAndRemoveTask();
    }

    @Override
    protected void onStop() {
        super.onStop();
        finishAndRemoveTask();
    }
}