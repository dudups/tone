package com.ezone.ezproject.es.entity;

public class OperationLogField {
    public static final String ID = "id";
    public static final String CREATE_TIME = "createTime";
    public static final String OPERATOR = "operator";
    public static final String IP = "ip";
    public static final String DETAIL = "detail";
    public static final String PROJECT_ID = "projectId";
    public static final String OPERATE_TYPE = "operateType";
    public static final String RESOURCE_ID = "resourceId";
    public static final String VALUE = "value";

    public static final String[] DEFAULT_SHOW_FIELDS = new String[]{
            CREATE_TIME, OPERATOR, IP, OPERATE_TYPE, DETAIL
    };

    public static final String[] DEFAULT_SHOW_FIELDS_NAME = new String[]{
            "创建时间", "操作人", "操作人IP", "操作类型", "事件描述"
    };
}
