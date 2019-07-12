package com.zhou.example.messagelisenterservice;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;

public class ForegroundActivity extends Activity {

    private final long DELAY_FINISH_TIME = 2000;
    private long enterTime;

    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forground);
        enterTime = SystemClock.elapsedRealtime();
        handler.postDelayed(finishRunnable, DELAY_FINISH_TIME);

        registerBroadCast();
    }

    private void registerBroadCast() {
        IntentFilter filter = new IntentFilter(Constant.FINISH_FOREGROUND_ACTIVITY);
        registerReceiver(finishActivityBroadcast, filter);
    }

    private void unregisterBroadCast() {
        try {
            unregisterReceiver(finishActivityBroadcast);
        } catch (Exception ignore) {
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterBroadCast();
    }

    private Runnable finishRunnable = new Runnable() {
        @Override
        public void run() {
            if (!ForegroundActivity.this.isFinishing()) {
                finishAndRemoveTask();
            }
        }
    };

    private BroadcastReceiver finishActivityBroadcast = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constant.FINISH_FOREGROUND_ACTIVITY.equals(intent.getAction())) {
                handler.removeCallbacks(finishRunnable);
                long tempTime = SystemClock.elapsedRealtime() - enterTime;
                if (tempTime > DELAY_FINISH_TIME) {
                    handler.post(finishRunnable);
                } else {
                    handler.postDelayed(finishRunnable, DELAY_FINISH_TIME - tempTime);
                }
            }
        }
    };
}
