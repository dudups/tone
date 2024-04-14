package com.ezone.ezproject.modules.notice.bean;

public enum NoticeType {
    // todo 短信和微信未接入
    SYSTEM, EMAIL, WE_CHAT, MOBILE, FEI_SHU;

    public static final NoticeType DEFAULT = NoticeType.SYSTEM;
    public static final NoticeType[] DEFAULTS = new NoticeType[]{NoticeType.SYSTEM};
}
