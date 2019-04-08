package com.zhou.example.messagelisenterservice;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.zhou.example.messagelisenterservice.db.DBUtil;
import com.zhou.example.messagelisenterservice.db.MsgType;
import com.zhou.example.messagelisenterservice.ui.MsgListAdapter;

import static com.zhou.example.messagelisenterservice.Constant.LIST_PAGE_ITEM;

public class MainActivity extends BaseActivity implements View.OnClickListener {


    private boolean isShowList = false;


    private RelativeLayout btnLayout;
    private LinearLayout listLayout;
    @SuppressWarnings("FieldCanBeLocal")
    private Button startToNotify, previousBtn, nextBtn;
    private ListView listView;

    private MsgListAdapter listAdapter;
    private int currentPage = 0;
    private int pageLength = 0;
    private long maxLength = 0;

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.activity_show);
    }


    @Override
    protected void InitView() {
        startToNotify = findViewById(R.id.startToNotifyBtn);
        btnLayout = findViewById(R.id.btn_layout);
        listLayout = findViewById(R.id.list_layout);
        previousBtn = findViewById(R.id.page_previsous_btn);
        nextBtn = findViewById(R.id.page_next_btn);
        listView = findViewById(R.id.msg_show_list);
        // set lisent
        startToNotify.setOnClickListener(this);
        previousBtn.setOnClickListener(this);
        nextBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startToNotifyBtn:
                showListLayout(MsgType.NOTIFY);
                break;
            case R.id.page_previsous_btn:
                previsousPage();
                break;
            case R.id.page_next_btn:
                nextPage();
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isShowList) {
            showBtnLayout();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void previsousPage() {
        if (listAdapter == null) return;

        readPage(listAdapter.getMsgType());

        currentPage--;

        if (currentPage < 0)
            currentPage = 0;

        listAdapter.setOffsetPage(currentPage);
    }

    private void nextPage() {
        if (listAdapter == null) return;

        readPage(listAdapter.getMsgType());

        currentPage++;

        if (currentPage >= maxLength)
            currentPage = (int) (maxLength - 1);

        listAdapter.setOffsetPage(currentPage);
    }

    @SuppressWarnings("SameParameterValue")
    private void showListLayout(MsgType msgType) {
        if (isShowList) return;
        isShowList = true;
        btnLayout.setVisibility(View.GONE);
        listLayout.setVisibility(View.VISIBLE);

        readPage(msgType);

        currentPage = pageLength <= 0 ? 0 : pageLength - 1;

        listAdapter = new MsgListAdapter(msgType, this, currentPage);
        listView.setAdapter(listAdapter);
    }

    private void showBtnLayout() {
        if (!isShowList) return;
        isShowList = false;


        if (null != listAdapter) listAdapter.clear();
        listView.removeAllViews();

        btnLayout.setVisibility(View.VISIBLE);
        listLayout.setVisibility(View.GONE);
    }

    private void readPage(MsgType msgType) {
        maxLength = DBUtil.INSTANT.getCount(msgType);
        if (maxLength > 0 && maxLength < LIST_PAGE_ITEM) {
            pageLength = 1;
        } else {
            pageLength = (int) (maxLength / LIST_PAGE_ITEM);
            if (pageLength * LIST_PAGE_ITEM < maxLength) {
                pageLength += 1;
            }
        }
    }

}
