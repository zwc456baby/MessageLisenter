package com.zhou.example.messagelisenterservice.db;

import org.greenrobot.greendao.converter.PropertyConverter;

public class MsgTypeConvert implements PropertyConverter<MsgType, String> {


    @Override
    public MsgType convertToEntityProperty(String databaseValue) {
        return MsgType.valueOf(databaseValue);
    }

    @Override
    public String convertToDatabaseValue(MsgType entityProperty) {
        return entityProperty.name();
    }
}
