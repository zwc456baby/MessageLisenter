package com.zhou.example.messagelisenterservice.db;

import org.greenrobot.greendao.annotation.Convert;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Index;

import java.util.Date;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.NotNull;

@Entity(indexes = {
        @Index(value = "message, date DESC", unique = true)
})
public class MsgEntry {

    @org.greenrobot.greendao.annotation.Id
    private long Id;

    @Convert(converter = MsgTypeConvert.class, columnType = String.class)
    private MsgType msgType;

    private String msgPackage;
    private java.util.Date date;
    @NotNull
    private String message;

    public MsgEntry(MsgType msgType, String msgPackage, Date date, String message) {
        this.msgType = msgType;
        this.msgPackage = msgPackage;
        this.date = date;
        this.message = message;
    }

    public MsgType getMsgType() {
        return msgType;
    }

    public void setMsgType(MsgType msgType) {
        this.msgType = msgType;
    }

    public String getMsgPackage() {
        return msgPackage;
    }

    public void setMsgPackage(String msgPackage) {
        this.msgPackage = msgPackage;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
