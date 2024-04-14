package com.ezone.ezproject.modules.notice.message;

import com.ezone.ezproject.common.template.VelocityTemplate;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.modules.alarm.bean.AlarmItem;
import com.ezone.ezproject.modules.alarm.bean.PlanAlarmItem;
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
public class PlanAlarmMessageModel extends BaseMessageModel {
    private AlarmItem alarmItem;
    private Plan plan;

    @Override
    public String getFeiShuContent() {
        return render("/vm/notice/card/feiShu/planAlarmContent.tpl");
    }

    @Override
    public String getContent() {
        return String.format("项目[%s]中的计划[%s]触发了[%s]预警规则，", project.getName(), plan.getName(), alarmItem.getName()) +
                "触发字段类型为计划，" +
                "字段名称为" + PlanAlarmItem.filedName(alarmItem.getDateFieldKey()) + "，" +
                "规则为" + alarmItem.alarmDateRuleFormat() + "。";
    }

    @Override
    public String getEmailContent() {
        return String.format("项目[%s]中的计划[%s]触发了[%s]预警规则。<br/>", project.getName(), plan.getName(), alarmItem.getName()) +
                "规则内容：<br/>" +
                "  字段类型：计划 <br/>" +
                String.format("  字段名称：%s <br/>", PlanAlarmItem.filedName(alarmItem.getDateFieldKey())) +
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
        context.put("projectName", project.getName());
        context.put("planName", plan.getName());
        context.put("planUrl", planUrl());
        context.put("alarmName", alarmItem.getName());
        context.put("alarmField", PlanAlarmItem.filedName(alarmItem.getDateFieldKey()));
        context.put("alarmDateRule", alarmItem.alarmDateRuleFormat());
        return context;
    }


    private String planPathHtml() {
        String url = endpointHelper.planDetailPath(project.getKey(), plan.getId());
        return String.format("<a href='%s'>%s</a>", url, plan.getName());
    }

    private String planUrl() {
        return endpointHelper.planDetailUrl(project.getCompanyId(), project.getKey(), plan.getId());
    }

}
