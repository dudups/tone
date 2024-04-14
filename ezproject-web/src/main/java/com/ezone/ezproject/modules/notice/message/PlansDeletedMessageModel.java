package com.ezone.ezproject.modules.notice.message;

import com.ezone.ezproject.common.template.VelocityTemplate;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.modules.common.EndpointHelper;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

@SuperBuilder
@Getter
@Slf4j
/**
 * 创建更新卡片
 */
public class PlansDeletedMessageModel implements MessageModel {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    private static final ProjectNoticeConfig.Type operationType = ProjectNoticeConfig.Type.DELETE;
    private Project project;
    /**
     * 删除的计划名称
     */
    private List<String> deletedPlanNames;
    private String sender;
    private String nickName;
    private EndpointHelper endpointHelper;

    public String getEscapeTitle() {
        return null;
    }

    @Override
    public String getFeiShuContent() {
        return render("/vm/notice/plan/feiShu/plansDeletedContent.tpl");
    }

    @Override
    public String getContent() {
        return String.format("%s在项目[%s]中%s了计划[%s]",
                userPathHtml(), project.getName(), operationType.getCnName(), getPlanNames());
    }

    @Override
    public String getEmailContent() {
        return String.format("%s在项目[%s]中%s了计划[%s]",
                userUrlHtml(), project.getName(), operationType.getCnName(), getPlanNames());
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
        context.put("deletedPlanNames", deletedPlanNames);
        return context;
    }

    private String getPlanNames() {
        return CollectionUtils.isEmpty(deletedPlanNames) ? "" : StringUtils.join(deletedPlanNames, "、");
    }

    private String userUrlHtml() {
        return String.format("<a href='%s'>%s</a>", endpointHelper.userHomeUrl(project.getCompanyId(), sender), nickName);
    }

    protected String userPathHtml() {
        return String.format("<User>%s</User>", sender);
    }
}
