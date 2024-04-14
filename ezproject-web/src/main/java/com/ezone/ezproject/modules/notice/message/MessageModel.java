package com.ezone.ezproject.modules.notice.message;


public interface MessageModel {

    /**
     * 标题
     *
     * @return
     */
    String getEscapeTitle();

    /**
     * 站内通知内容
     *
     * @return
     */
    String getContent();

    /**
     * 邮件通知内容
     *
     * @return
     */
    default String getEmailContent() {
        return getContent();
    }

    /**
     * 飞书通知内容
     *
     * @return
     */
    default String getFeiShuContent() {
        return getContent();
    }
}
