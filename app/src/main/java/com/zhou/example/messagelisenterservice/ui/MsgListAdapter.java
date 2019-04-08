package com.zhou.example.messagelisenterservice.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.zhou.example.messagelisenterservice.Constant;
import com.zhou.example.messagelisenterservice.R;
import com.zhou.example.messagelisenterservice.db.DBUtil;
import com.zhou.example.messagelisenterservice.db.MsgEntry;
import com.zhou.example.messagelisenterservice.db.MsgType;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.zhou.example.messagelisenterservice.Constant.LIST_PAGE_ITEM;

public class MsgListAdapter extends BaseAdapter {

    private final DateFormat dataFormat = new SimpleDateFormat(Constant.dayType, Locale.getDefault());

    private MsgType msgType;
    private Context mContext;
    private List<MsgEntry> msgEntryList;

    private int offset;

    public MsgListAdapter(MsgType type, Context context, int offsetPage) {
        this.msgType = type;
        this.mContext = context;

        setOffsetPage(offsetPage);
    }

    public void setOffsetPage(int offsetPage) {
        this.offset = offsetPage * LIST_PAGE_ITEM;

        if (offset < 0) {
            offset = 0;
        }

        getNextData();
    }

    public MsgType getMsgType() {
        return msgType;
    }

    public void clear() {
        msgEntryList.clear();
        notifyDataSetChanged();
    }

    private void getNextData() {
        long maxLength = DBUtil.INSTANT.getCount(msgType);

        if (maxLength == 0) {
            msgEntryList = new ArrayList<>();
            notifyDataSetChanged();
            return;
        }
        if (offset < 0) {
            offset = 0;
        }

        int getLength = (offset + LIST_PAGE_ITEM) > maxLength ? (int) (maxLength - offset) : LIST_PAGE_ITEM;
        msgEntryList = DBUtil.INSTANT.query(msgType, offset, getLength);

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return msgEntryList.size();
    }

    @Override
    public Object getItem(int position) {
        return msgEntryList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return msgEntryList.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (null == convertView) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.msg_list_item, parent, false);
            holder = new ViewHolder();
            holder.packageText = convertView.findViewById(R.id.msg_list_item_package);
            holder.dateText = convertView.findViewById(R.id.msg_list_item_time);
            holder.textView = convertView.findViewById(R.id.msg_list_item_body);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        MsgEntry msgEntry = (MsgEntry) getItem(position);
        holder.packageText.setText(msgEntry.getMsgPackage());
        holder.dateText.setText(dataFormat.format(msgEntry.getDate()));
        holder.textView.setText(msgEntry.getMessage());
        return convertView;
    }

    private class ViewHolder {
        TextView packageText;
        TextView dateText;
        TextView textView;
    }
}
