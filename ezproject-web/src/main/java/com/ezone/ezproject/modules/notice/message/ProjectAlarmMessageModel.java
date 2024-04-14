package com.ezone.ezproject.modules.notice.message;

import com.ezone.ezproject.common.template.VelocityTemplate;
import com.ezone.ezproject.modules.alarm.bean.AlarmItem;
import com.ezone.ezproject.modules.alarm.bean.ProjectAlarmItem;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.velocity.VelocityContext;

@SuperBuilder
@Getter
@Slf4j

/**
 * 项目计划触发项目预警设置的通知
 */
public class ProjectAlarmMessageModel extends BaseMessageModel {

    private AlarmItem alarmItem;

    @Override
    public String getFeiShuContent() {
        return render("/vm/notice/card/feiShu/projectAlarmContent.tpl");
    }

    @Override
    public String getContent() {
        return String.format("项目[%s]触发了[%s]预警规则，", project.getName(), alarmItem.getName()) +
                "触发字段类型为项目，" +
                "字段名称为" + ProjectAlarmItem.filedName(alarmItem.getDateFieldKey()) + "，" +
                "规则为" + alarmItem.alarmDateRuleFormat() + "。";
    }

    @Override
    public String getEmailContent() {
        return String.format("项目[%s]触发了[%s]预警规则，", project.getName(), alarmItem.getName()) +
                "规则内容：<br/>" +
                "  字段类型：项目 <br/>" +
                String.format("  字段名称：%s <br/>", ProjectAlarmItem.filedName(alarmItem.getDateFieldKey())) +
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
        context.put("projectName", project.getName());
        context.put("alarmName", alarmItem.getName());
        context.put("alarmField", ProjectAlarmItem.filedName(alarmItem.getDateFieldKey()));
        context.put("alarmDateRule", alarmItem.alarmDateRuleFormat());
        return context;
    }
}
