package com.ezone.ezproject.modules.notice.message;

import com.ezone.ezproject.common.template.VelocityTemplate;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.modules.common.EndpointHelper;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.velocity.VelocityContext;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

@SuperBuilder
@Getter
@Slf4j
/**
 * 创建更新卡片
 */
public class PlanMessageModel implements MessageModel {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    private Project project;
    /**
     * 新建或更新的计划
     */
    private Plan plan;
    private ProjectNoticeConfig.Type operationType;
    private String sender;
    private String nickName;
    private EndpointHelper endpointHelper;

    public String getEscapeTitle() {
        return null;
    }

    @Override
    public String getFeiShuContent() {
        return render("/vm/notice/plan/feiShu/planContent.tpl");
    }

    @Override
    public String getContent() {
        return String.format("%s在项目[%s]中%s了计划[%s]",
                userPathHtml(), project.getName(), operationType.getCnName(), planPathHtml());
    }

    @Override
    public String getEmailContent() {
        return String.format("%s在项目[%s]中%s了计划[%s]",
                userUrlHtml(), project.getName(), operationType.getCnName(), planUrlHtml());
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
        context.put("userNickOrName", nickName);
        context.put("userUrl", endpointHelper.userHomeUrl(project.getCompanyId(), sender));
        context.put("projectName", project.getName());
        context.put("operationType", operationType);
        context.put("planName", plan.getName());
        context.put("planUrl", endpointHelper.planDetailUrl(project.getCompanyId(), project.getKey(), plan.getId()));
        return context;
    }

    private String planPathHtml() {
        String url = endpointHelper.planDetailPath(project.getKey(), plan.getId());
        return String.format("<a href='%s'>%s</a>", url, plan.getName());
    }

    private String planUrlHtml() {
        String url = endpointHelper.planDetailUrl(project.getCompanyId(), project.getKey(), plan.getId());
        return String.format("<a href='%s'>%s</a>", url, plan.getName());
    }

    private String userUrlHtml() {
        return String.format("<a href='%s'>%s</a>", endpointHelper.userHomeUrl(project.getCompanyId(), sender), nickName);
    }

    protected String userPathHtml() {
        return String.format("<User>%s</User>", sender);
    }
}
