package com.zhou.example.messagelisenterservice;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HistoryActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        registerReceiver(sysBoardReceiver, new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        showList();
    }

    /**
     * 展示历史数据
     */
    private void showList() {
        ListView historyListView = findViewById(R.id.historyListView);
        ArrayList<MessageBean> list = Utils.getMsgHistory(this);
        if (list.size() == 0) {
            TextView nullTip = findViewById(R.id.historyNullTipTv);
            historyListView.setVisibility(View.GONE);
            nullTip.setVisibility(View.VISIBLE);
            return;
        }
        historyListView.setAdapter(new ListAdapter(this, R.layout.listview_history_item, list));
    }

    class ListAdapter extends ArrayAdapter<MessageBean> {

        private List<MessageBean> list;
        private Context context;
        private int resourceId;

        ListAdapter(Context context, int resource, List<MessageBean> objects) {
            super(context, resource, objects);
            this.list = objects;
            this.context = context;
            this.resourceId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final MessageBean messageBean = list.get(position);
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(resourceId, null);
                viewHolder = new ViewHolder();
                viewHolder.textView = convertView.findViewById(R.id.messageBeanTv);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            // 一个是去除前后回车符的，一个是未去除回车符的
            String showText = String.format("%s\n%s\n%s"
                    , Utils.formatTime(new Date(messageBean.getTime()))
                    , messageBean.getTitle()
                    , messageBean.getContext().replace('\n', ' '));
            viewHolder.textView.setText(showText);
            viewHolder.textView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    String showText = String.format("%s\n%s\n%s"
                            , Utils.formatTime(new Date(messageBean.getTime()))
                            , messageBean.getTitle()
                            , messageBean.getContext());
                    Intent intent = new Intent(context, LockShowActivity.class);
                    intent.putExtra(Constant.GET_MESSAGE_KEY, showText);
                    intent.putExtra(LockShowActivity.GET_SHOW_ACTIVITY_TYPE, -2);
                    context.startActivity(intent);
                }
            });
            return convertView;
        }

        class ViewHolder {
            TextView textView;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (!isFinishing()) {
            finishAndRemoveTask();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(sysBoardReceiver);
    }

    private BroadcastReceiver sysBoardReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(intent.getAction())) {
                if (!HistoryActivity.this.isFinishing()) {
                    HistoryActivity.this.finishAndRemoveTask();
                }
            }
        }
    };
}
