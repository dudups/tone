package com.ezone.ezproject.modules.notice.message;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@SuperBuilder
@Getter
@Slf4j
/**
 * 创建更新卡片
 */
public class CardStatusChangeMessageModel extends CardOperationMessageModel {
    private String fromStatusName;
    private String toStatusName;

    @Override
    public String getEscapeTitle() {
        return String.format("%s更新了[%s-%s]卡片状态：%s->%s", nickName, project.getKey(), seqNum(), fromStatusName, toStatusName);
    }

    @Override
    public String getContent() {
        return String.format("%s更新了卡片%s 状态从[%s]变为[%s]", super.userPathHtml(), cardPathHtml(), fromStatusName, toStatusName);
    }

    @Override
    public String getEmailContent() {
        return String.format("%s更新了卡片%s 状态从[%s]变为[%s]", super.userUrlHtml(), cardUrlHtml(), fromStatusName, toStatusName);
    }

}
