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
public class CardAtUserMessageModel extends CardOperationMessageModel {
    @Override
    public String getEscapeTitle() {
        return String.format("%s在[%s-%s]中提到了你", nickName, projectKey(), seqNum());
    }

    @Override
    public String getContent() {
        return String.format("%s在[%s]中提到了你", super.userPathHtml(), cardPathHtml());
    }

    @Override
    public String getEmailContent() {
        return String.format("%s在[%s]中提到了你", super.userUrlHtml(), cardUrlHtml());
    }
}
