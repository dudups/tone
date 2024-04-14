package com.ezone.ezproject.modules.notice.message;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;

/**
 * 催办卡片通知
 */
@SuperBuilder
@Getter
@Slf4j
public class CardRemindMessageModel extends CardOperationMessageModel {
    private String remindContent;

    @Override
    public String getFeiShuContent() {
        return render("/vm/notice/card/feiShu/cardRemindContent.tpl");
    }

    @Override
    public String getEscapeTitle() {
        return String.format("%s催办了卡片%s-%s", nickName, projectKey(), seqNum());
    }

    @Override
    public String getContent() {
        return String.format("<User>%s</User>催办了卡片%s:%s", sender, super.cardPathHtml(), StringUtils.defaultString(remindContent));
    }

    @Override
    public String getEmailContent() {
        return String.format("%s催办卡片%s %s", super.userUrlHtml(), cardUrlHtml(), StringUtils.defaultString(remindContent));
    }

    @Override
    protected VelocityContext context() {
        VelocityContext context = super.context();
        context.put("remindContent", remindContent);
        return context;
    }
}
