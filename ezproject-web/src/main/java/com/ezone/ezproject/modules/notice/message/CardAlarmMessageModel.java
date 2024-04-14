package com.ezone.ezproject.modules.notice.message;

import com.ezone.ezproject.common.template.VelocityTemplate;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.modules.alarm.bean.AlarmItem;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.service.CardHelper;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.velocity.VelocityContext;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Map;

@SuperBuilder
@Getter
@Slf4j

/**
 * 卡片触发项目预警设置的通知
 */
public class CardAlarmMessageModel extends BaseMessageModel {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    private AlarmItem alarmItem;
    private Map<String, Object> cardDetail;
    private ProjectCardSchema projectCardSchema;

    @Override
    public String getFeiShuContent() {
        return render("/vm/notice/card/feiShu/cardAlarmContent.tpl");
    }

    @Override
    public String getContent() {
        return String.format("卡片[%s]触发了[%s]预警规则，", cardPathHtml(), alarmItem.getName()) +
                "触发字段类型为卡片字段，" +
                String.format("  字段名称：%s ，", projectCardSchema.findCardField(alarmItem.getDateFieldKey()).getName()) +
                String.format("  提醒规则：%s 。", alarmItem.alarmDateRuleFormat());
    }

    @Override
    public String getEmailContent() {
        return String.format("卡片[%s]触发了[%s]预警规则。<br/>", cardUrlHtml(), alarmItem.getName()) +
                "规则内容：<br/>" +
                "  字段类型：卡片字段 <br/>" +
                String.format("  字段名称：%s <br/>", projectCardSchema.findCardField(alarmItem.getDateFieldKey()).getName()) +
                String.format("  提醒规则：%s <br/>", alarmItem.alarmDateRuleFormat());

    }

    protected String render(String resource) {
        try {
            return VelocityTemplate.render(context(), resource);
        } catch (Exception e) {
            log.error("Render msg exception!", e);
            return e.getMessage();
        }
    }

    protected VelocityContext context() {
        VelocityContext context = new VelocityContext();
        context.put("StringEscapeUtils", StringEscapeUtils.class);
        context.put("FieldUtil", FieldUtil.class);
        context.put("projectKeyCardNum", projectKey() + "-" + seqNum());
        context.put("cardTitle", cardTitle());
        context.put("cardUrl", carUrl());
        context.put("alarmName", alarmItem.getName());
        context.put("alarmField", projectCardSchema.findCardField(alarmItem.getDateFieldKey()).getName());
        context.put("alarmDateRule", alarmItem.alarmDateRuleFormat());
        return context;
    }


    protected String carUrl() {
        return endpointHelper.cardDetailUrl(project.getCompanyId(), project.getKey(), FieldUtil.getSeqNum(cardDetail));
    }

    private String cardUrlHtml() {
        return String.format("<a href='%s'>[%s-%s] %s</a>", carUrl(), projectKey(), seqNum(), cardTitle());
    }

    private String cardPathHtml() {
        return CardHelper.cardPathHtml(project, cardDetail, endpointHelper);
    }

    private String cardTitle() {
        return FieldUtil.getTitle(cardDetail);
    }

    private String projectKey() {
        return project.getKey();
    }

    private Long seqNum() {
        return FieldUtil.getSeqNum(cardDetail);
    }

}
