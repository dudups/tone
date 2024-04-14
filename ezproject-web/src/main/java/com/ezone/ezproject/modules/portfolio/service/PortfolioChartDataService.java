package com.ezone.ezproject.modules.portfolio.service;


import com.ezone.ezproject.dal.entity.Portfolio;
import com.ezone.ezproject.es.dao.PortfolioChartConfigDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.PortfolioConfig;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.bean.query.In;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.chart.ezinsight.config.EzInsightChart;
import com.ezone.ezproject.modules.chart.ezinsight.enums.InsightChartGroupType;
import com.ezone.ezproject.modules.chart.ezinsight.service.EzInsightDataService;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class PortfolioChartDataService {
    private PortfolioQueryService portfolioQueryService;
    private EzInsightDataService insightDataService;
    private PortfolioConfigService portfolioConfigService;
    private PortfolioChartConfigDao chartConfigDao;
    private RelPortfolioProjectService portfolioProjectService;
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public Object chartData(Long portfolioId, Long chartId, Map<String, Object> overwriteConfig) throws Exception {
        Portfolio portfolio = portfolioQueryService.select(portfolioId);
        PortfolioConfig config = portfolioConfigService.getConfig(portfolioId);

        List<Long> projectIds = portfolioProjectService.queryRelationProjectIds(portfolioId, config.getChartContainDescendant());
        Map<String, Object> chartConfig = new HashMap<>();
        chartConfig.putAll(chartConfigDao.find(chartId));
        chartConfig.putAll(overwriteConfig);

//        List<Query> queries = new ArrayList<>();
//        if (CollectionUtils.isEmpty(projectIds)) {
//            return null;
//        } else {
//            queries.add(In.builder().field(CardField.PROJECT_ID).values(projectIds.stream().map(String::valueOf).collect(Collectors.toList())).build());
//        }
//        queries.add(Eq.builder().field(CardField.DELETED).value(String.valueOf(false)).build());

        EzInsightChart ezInsightChart = JSON_MAPPER.convertValue(chartConfig, EzInsightChart.class);
        return insightDataService.chartData(InsightChartGroupType.PROJECT_SET.name(), portfolio.getCompanyId(), projectIds, ezInsightChart);
    }
}
