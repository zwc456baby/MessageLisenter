package com.zhou.example.messagelisenterservice;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

public  abstract class  BaseActivity extends Activity {

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        InitView();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        InitView();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        InitView();
    }

    abstract void InitView();
}
