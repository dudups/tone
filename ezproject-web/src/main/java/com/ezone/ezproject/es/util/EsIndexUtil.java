package com.ezone.ezproject.es.util;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EsIndexUtil {
    private static String PREFIX = StringUtils.EMPTY;

    @Value("${es.prefix}")
    public void setPrefix(String prefix) {
        PREFIX = prefix;
    }

    public static String indexForCompanyCardSchema() {
        return PREFIX + "company-card-schema";
    }

    public static String indexForCompanyProjectSchema() {
        return PREFIX + "company-project-schema";
    }

    public static String indexForProjectExtend() {
        return PREFIX + "project-extend";
    }

    public static String indexForProjectTemplateDetail() {
        return PREFIX + "project-template-detail";
    }

    public static String indexForProjectCardSchema() {
        return PREFIX + "project-card-schema";
    }

    public static String indexForProjectRoleSchema() {
        return PREFIX + "project-role-schema";
    }

    public static String indexForCompanyProjectRoleSchema() {
        return PREFIX + "company-project-role-schema";
    }

    public static String indexForCardEvent() {
        return PREFIX + "project-card-event";
    }

    public static String indexForCardComment() {
        return PREFIX + "project-card-comment";
    }

    public static String indexForCardQueryView() {
        return PREFIX + "project-card-query-view";
    }

    public static String indexForPlanSummary() {
        return PREFIX + "project-plan-summary";
    }

    public static String indexForPlanNotice() {
        return PREFIX + "project-plan-notice";
    }

    public static String indexForProjectNoticeBoard() {
        return PREFIX + "project-notice-board";
    }

    public static String indexForProjectSummary() {
        return PREFIX + "project-summary";
    }

    public static String indexForProjectMenu() {
        return PREFIX + "project-menu";
    }

    public static String indexForChart() {
        return PREFIX + "project-chart";
    }

    public static String indexForCard() {
        return PREFIX + "project-card";
    }

    public static String indexForCardTemplate() {
        return PREFIX + "project-card-template";
    }

    public static String indexForCardDraft() {
        return PREFIX + "project-card-draft";
    }

    public static String indexForWebHookProject() {
        return PREFIX + "project-webhook-rel";
    }

    public static String indexForStoryMapQuery() {
        return PREFIX + "project-story-map-query";
    }

    public static String indexForCardBpmFlow() {
        return PREFIX + "card-bpm-flow";
    }

    @Deprecated
    public static String indexForCard(Long projectId) {
        return String.format("project-%s-card", projectId);
    }

    @Deprecated
    public static String indexForCardTemplate(Long projectId) {
        return String.format("project-%s-card-template", projectId);
    }

    public static String indexForOperationLog() {
        return PREFIX + "project-operation-log";
    }

    public static String indexProjectNoticeConfig() {
        return PREFIX + "project-notice-config";
    }

    public static String indexForPortfolioRoleSchema() {
        return PREFIX + "portfolio-role-schema";
    }

    public static String indexForPortfolioChartConfig() {
        return PREFIX + "portfolio-chart-config";
    }

    public static String indexForPortfolioConfig() {
        return PREFIX + "portfolio-config";
    }

    public static String indexProjectAlarmConfig() {
        return PREFIX + "project-alarm";
    }

    public static String indexForCompanyWorkloadSetting() {
        return PREFIX + "company-workload-setting";
    }

    public static String indexForProjectWorkloadSetting() {
        return PREFIX + "project-workload-setting";
    }

    public static String indexForCardIncrWorkload() {
        return PREFIX + "card-incr-workload";
    }
}
