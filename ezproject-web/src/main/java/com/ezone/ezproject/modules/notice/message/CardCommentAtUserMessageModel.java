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
public class CardCommentAtUserMessageModel extends CardOperationMessageModel {

    @Override
    public String getEscapeTitle() {
        return String.format("%s在卡片[%s-%s]的评论中提到了你", nickName, projectKey(), seqNum());
    }

    @Override
    public String getContent() {
        return String.format("%s>在卡片[%s]的评论中提到了你", userPathHtml(), cardPathHtml());
    }

    @Override
    public String getEmailContent() {
        return String.format("%s在卡片[%s]的评论中提到了你", userUrlHtml(), cardUrlHtml());
    }
}
