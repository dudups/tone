package com.ezone.ezproject.es;

import com.ezone.ezproject.common.EsUtil;
import com.ezone.ezproject.common.template.VelocityTemplate;
import com.ezone.ezproject.es.util.EsIndexUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.VelocityContext;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class EsInitializer {
    private RestHighLevelClient es;
    // 确保依赖注入提前注入好EsIndexUtil静态属性：es索引前缀
    private EsIndexUtil esIndexUtil;

    @Getter(lazy = true)
    private final String indexSettings = VelocityTemplate.render(new VelocityContext(), "/es/index-settings.yaml");

    @Getter(lazy = true)
    private final Map<String, String> indexMapping = new HashMap<String, String>() {{
        put(EsIndexUtil.indexForCompanyCardSchema(), "/es/company-card-schema-mapping.yaml");
        put(EsIndexUtil.indexForCompanyProjectSchema(), "/es/company-project-schema-mapping.yaml");
        put(EsIndexUtil.indexForProjectExtend(), "/es/project-extend-mapping.yaml");
        put(EsIndexUtil.indexForProjectCardSchema(), "/es/project-card-schema-mapping.yaml");
        put(EsIndexUtil.indexForProjectRoleSchema(), "/es/project-role-schema-mapping.yaml");
        put(EsIndexUtil.indexForCompanyProjectRoleSchema(), "/es/project-role-schema-mapping.yaml");
        put(EsIndexUtil.indexForProjectTemplateDetail(), "/es/project-template-detail-mapping.yaml");
        put(EsIndexUtil.indexForCard(), "/es/card-mapping.yaml");
        put(EsIndexUtil.indexForCardTemplate(), "/es/card-template-mapping.yaml");
        put(EsIndexUtil.indexForCardEvent(), "/es/card-event-mapping.yaml");
        put(EsIndexUtil.indexForCardComment(), "/es/card-comment-mapping.yaml");
        put(EsIndexUtil.indexForCardQueryView(), "/es/card-query-view-mapping.yaml");
        put(EsIndexUtil.indexForCardDraft(), "/es/card-draft-mapping.yaml");
        put(EsIndexUtil.indexForPlanSummary(), "/es/plan-summary-mapping.yaml");
        put(EsIndexUtil.indexForPlanNotice(), "/es/plan-notice-mapping.yaml");
        put(EsIndexUtil.indexForProjectNoticeBoard(), "/es/project-notice-board-mapping.yaml");
        put(EsIndexUtil.indexForProjectSummary(), "/es/project-summary-mapping.yaml");
        put(EsIndexUtil.indexForChart(), "/es/chart-mapping.yaml");
        put(EsIndexUtil.indexForWebHookProject(), "/es/webhook-project-rel-mapping.yaml");
        put(EsIndexUtil.indexForStoryMapQuery(), "/es/story-map-query-mapping.yaml");
        put(EsIndexUtil.indexForOperationLog(), "/es/operation-log-mapping.yaml");
        put(EsIndexUtil.indexProjectNoticeConfig(), "/es/project-notice-config-mapping.yaml");
        put(EsIndexUtil.indexForCardBpmFlow(), "/es/card-bpm-flow-mapping.yaml");
        put(EsIndexUtil.indexForPortfolioRoleSchema(), "/es/portfolio-role-schema-mapping.yaml");
        put(EsIndexUtil.indexForPortfolioChartConfig(), "/es/portfolio-chart-config-mapping.yaml");
        put(EsIndexUtil.indexForPortfolioConfig(), "/es/portfolio-config-mapping.yaml");
        put(EsIndexUtil.indexForProjectMenu(), "/es/project-menu-mapping.yaml");
        put(EsIndexUtil.indexProjectAlarmConfig(), "/es/project-alarm-mapping.yaml");

        put(EsIndexUtil.indexForCompanyWorkloadSetting(), "/es/company-workload-setting-mapping.yaml");
        put(EsIndexUtil.indexForProjectWorkloadSetting(), "/es/project-workload-setting-mapping.yaml");
        put(EsIndexUtil.indexForCardIncrWorkload(), "/es/card-incr-workload-mapping.yaml");
    }};

    @PostConstruct
    public void ensureIndexes() throws IOException {
        for (Map.Entry<String, String> entry : getIndexMapping().entrySet()) {
            ensureIndex(entry.getKey(), entry.getValue());
        }
    }

    private void ensureIndex(String index, String resource) throws IOException {
        GetIndexRequest request = new GetIndexRequest(index);
        if (!es.indices().exists(request, EsUtil.REQUEST_OPTIONS)) {
            String yaml = VelocityTemplate.render(new VelocityContext(), resource);
            CreateIndexRequest createIndexRequest = new CreateIndexRequest(index);
            createIndexRequest.settings(getIndexSettings(), XContentType.YAML);
            createIndexRequest.mapping(yaml, XContentType.YAML);
            es.indices().create(createIndexRequest, EsUtil.REQUEST_OPTIONS);
        }
    }
}
