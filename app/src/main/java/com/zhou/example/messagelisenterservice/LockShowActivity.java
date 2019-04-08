package com.zhou.example.messagelisenterservice;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class LockShowActivity extends BaseActivity implements View.OnClickListener {

    private TextView showTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_lock_show);
        registerBroadCast();
        InitData();
    }

    private void registerBroadCast() {
        IntentFilter filter = new IntentFilter(Constant.FINISH_LOCK_SHOW_ACTIVITY);
        registerReceiver(finishActivityBroadcast, filter);
    }

    private void unregisterBroadCast() {
        unregisterReceiver(finishActivityBroadcast);
    }

    protected void InitView() {
        Button closeBtn = findViewById(R.id.closeBtn);
        closeBtn.setOnClickListener(this);
        showTv = findViewById(R.id.showTv);
    }

    private void InitData() {
        resetData(getIntent());
    }

    private void resetData(Intent intent) {
        String filterText = PreUtils.get(this, Constant.TITLE_FILTER_KEY, null);
        if (TextUtils.isEmpty(filterText)) {
            filterText = PreUtils.get(this, Constant.APP_PACKAGE_KEY, null);
        }
        if (TextUtils.isEmpty(filterText)) {
            filterText = String.format(getString(R.string.filter_text)
                    , PreUtils.get(this, Constant.MESSAGE_FILTER_KEY, "[NULL]"));
        }

        String showTvText = String.format(getString(R.string.show_message_count),
                filterText
                , intent.getIntExtra(Constant.GET_MESSAGE_LENGTH, -1));
        showTv.setText(showTvText);
    }

    private void sendPauseNotifyBroadcast() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Constant.CLOSE_ACTIVITY_STOP_NOTIFY_ACTION);
        sendBroadcast(sendIntent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        resetData(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.closeBtn:
                sendPauseNotifyBroadcast();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterBroadCast();
    }

    private BroadcastReceiver finishActivityBroadcast = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constant.FINISH_LOCK_SHOW_ACTIVITY.equals(intent.getAction())) {
                finishAndRemoveTask();
            } else if (Constant.UPDATA_MESSAGE_DATA_ACTION.equals(intent.getAction())) {
                resetData(intent);
            }
        }
    };
}
