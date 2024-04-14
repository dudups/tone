package com.ezone.ezproject.es.entity.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum FieldType {
    BOOLEAN(ValueType.BOOLEAN, "布尔值"),
    LINE(ValueType.STRING, "单行文本"),
    LINES(ValueType.STRING, "多行文本"),
    LONG(ValueType.LONG, "整型数字"),
    LONGS(ValueType.LONGS, "整型数字数组"),
    FLOAT(ValueType.FLOAT, "数字"),
    DATE(ValueType.DATE, "日期"),
    DATE_TIME(ValueType.DATE, "日期和时间"),
    SELECT(ValueType.STRING, "下拉列表"),
    RADIO(ValueType.STRING, "单选"),
    CHECK_BOX(ValueType.STRINGS, "多选"),
    URL(ValueType.STRING, "URL"),
    LABELS(ValueType.STRINGS, "Label"),
    USER(ValueType.STRING, "用户"),
    USERS(ValueType.STRINGS, "用户"),
    MEMBER(ValueType.STRING, "项目成员"),
    MEMBERS(ValueType.STRINGS, "项目成员");

    private ValueType defaultValueType;

    private String description;

    FieldType(ValueType defaultValueType, String description) {
        this.defaultValueType = defaultValueType;
        this.description = description;
    }

    @Getter
    public enum ValueType {
        BOOLEAN("boolean"),
        STRING("keyword"),
        STRINGS("keyword"),
        TEXT("text"),
        FLOAT("float"),
        LONG("long"),
        LONGS("long"),
        DATE("date");
        private String esDataType;

        ValueType(String esDataType) {
            this.esDataType = esDataType;
        }
    }

    public static final  List<FieldType> CUSTOM_FILED_TYPES = Arrays.asList(
            LINE,
            LINES,
            FLOAT,
            DATE,
            DATE_TIME,
            SELECT,
            RADIO,
            CHECK_BOX,
            URL,
            LABELS,
            USERS
    );
}
