package com.ezone.ezproject.modules.log.bean;

/**
 * @author cf
 */
public enum LogOperationType {

    /**
     * 增加子计划、计划删除、修改起止时间
     */
    PLAN_UPDATE("计划变更"),

    /**
     * 删除卡片、转换卡片类型（如将计划中某卡片由前端task转为后端task）
     */
    CARD_UPDATE("卡片变更"),

    ROLE_MEMBER_UPDATE("权限变更"),

    ROLE_UPDATE("角色变更"),

    CARD_FIELD_UPDATE("字段变更"),

    PROJECT_BASE_UPDATE("基本信息变更"),

    /**
     * 设置卡片类型的颜色与描述等的修改
     */
    CARD_TYPE_UPDATE("卡片类型"),

    /**
     * 卡片流程状态添加与删除
     */
    CARD_TYPE_STATUS_UPDATE("流程状态"),

    /**
     * 如工作流的修改以及自动流转的设置增删
     */
    CARD_TYPE_FLOW_UPDATE("卡片设置"),

    PROJECT_CLEAN_UPDATE("运维记录");

    private String cnName;

    LogOperationType(String cnName) {
        this.cnName = cnName;
    }

    public String getCnName() {
        return cnName;
    }
}