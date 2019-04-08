package com.zhou.example.messagelisenterservice.db;

import android.content.Context;

import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.query.WhereCondition;

import java.util.HashMap;
import java.util.List;

public class DBUtil {

    public static DBUtil INSTANT = DBUtilGenerate.instant;

    private static class DBUtilGenerate {
        private static DBUtil instant = new DBUtil();
    }

    private DBUtil() {
        msgTypeMap = new HashMap<>();
    }

    /****************  定义常量已经方法  ****************/

    private HashMap<String, WhereCondition> msgTypeMap;

    private DaoSession daoSession;

    public void INIT(Context mContext) {

        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(mContext, "notes-db");
        Database db = helper.getWritableDb();

        // encrypted SQLCipher database
        // note: you need to add SQLCipher to your dependencies, check the build.gradle file
        // DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "notes-db-encrypted");
        // Database db = helper.getEncryptedWritableDb("encryption-key");

        daoSession = new DaoMaster(db).newSession();
    }

    public long insert(MsgEntry entry) {
        return daoSession.insert(entry);
    }

    public List<MsgEntry> query(MsgType type, int start, int colum) {
        return daoSession.queryBuilder(MsgEntry.class)
                .where(getMsgTypeWhere(type)).offset(start).limit(colum).list();
    }

    public long getCount(MsgType type) {
        return daoSession.queryBuilder(MsgEntry.class)
                .where(getMsgTypeWhere(type)).count();
    }

    public List<MsgEntry> queryAll(MsgType type) {
        return daoSession.queryBuilder(MsgEntry.class)
                .where(getMsgTypeWhere(type)).list();
    }

    /**
     * 这个方法需要注意多线程情况下，多次的创建问题
     *
     * @param type msgtype
     */
    private WhereCondition getMsgTypeWhere(MsgType type) {
        WhereCondition msgTypeWhere = msgTypeMap.get(type.name());
        if (msgTypeWhere == null) {
            msgTypeWhere = msgTypeMap.put(type.name(), MsgEntryDao.Properties.MsgType.eq(type.name()));
        }
        return msgTypeWhere;
    }
}
