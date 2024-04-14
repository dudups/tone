package com.ezone.ezproject.modules.chart.ezinsight.service;

import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.stream.CollectorsV2;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.dao.CardEventDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.enums.InnerCardType;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bean.CardBean;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.card.bean.query.Between;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.bean.query.Gte;
import com.ezone.ezproject.modules.card.bean.query.In;
import com.ezone.ezproject.modules.card.bean.query.Lt;
import com.ezone.ezproject.modules.card.bean.query.Lte;
import com.ezone.ezproject.modules.card.bean.query.NotEq;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.card.service.CardReferenceValueHelper;
import com.ezone.ezproject.modules.card.service.CardSearchService;
import com.ezone.ezproject.modules.chart.config.enums.MetricType;
import com.ezone.ezproject.modules.chart.config.range.DateRange;
import com.ezone.ezproject.modules.chart.ezinsight.config.BugOwnerTop;
import com.ezone.ezproject.modules.chart.ezinsight.config.BugTrend;
import com.ezone.ezproject.modules.chart.ezinsight.config.CardDelayList;
import com.ezone.ezproject.modules.chart.ezinsight.config.CardEndTrend;
import com.ezone.ezproject.modules.chart.ezinsight.config.CardOwnerTop;
import com.ezone.ezproject.modules.chart.ezinsight.config.CardSummary;
import com.ezone.ezproject.modules.chart.ezinsight.config.CardTrend;
import com.ezone.ezproject.modules.chart.ezinsight.config.EzInsightCardsBar;
import com.ezone.ezproject.modules.chart.ezinsight.config.EzInsightCardsHistogram;
import com.ezone.ezproject.modules.chart.ezinsight.config.EzInsightCardsMultiTrend;
import com.ezone.ezproject.modules.chart.ezinsight.config.EzInsightCardsPie;
import com.ezone.ezproject.modules.chart.ezinsight.config.EzInsightChart;
import com.ezone.ezproject.modules.chart.ezinsight.config.EzInsightOneDimensionTable;
import com.ezone.ezproject.modules.chart.ezinsight.config.EzInsightProjectsSummaryTable;
import com.ezone.ezproject.modules.chart.ezinsight.config.EzInsightTwoDimensionTable;
import com.ezone.ezproject.modules.chart.ezinsight.config.EzInsightUserGantt;
import com.ezone.ezproject.modules.chart.ezinsight.data.BugTrendParser;
import com.ezone.ezproject.modules.chart.ezinsight.data.CardEndTrendParser;
import com.ezone.ezproject.modules.chart.ezinsight.data.CardMultiTrendParser;
import com.ezone.ezproject.modules.chart.ezinsight.data.CardOwnerTopParser;
import com.ezone.ezproject.modules.chart.ezinsight.data.CardSummaryParser;
import com.ezone.ezproject.modules.chart.ezinsight.data.CardTrendParser;
import com.ezone.ezproject.modules.chart.ezinsight.data.ProjectsSummaryTableParser;
import com.ezone.ezproject.modules.chart.ezinsight.enums.InsightChartGroupType;
import com.ezone.ezproject.modules.chart.service.ChartDataService;
import com.ezone.ezproject.modules.project.service.ProjectCardSchemaHelper;
import com.ezone.ezproject.modules.project.service.ProjectMemberQueryService;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class EzInsightDataService {
    private CardDao cardDao;
    private CardEventDao cardEventDao;
    private CardSearchService cardSearchService;
    private CardReferenceValueHelper cardReferenceValueHelper;
    private ProjectCardSchemaHelper projectCardSchemaHelper;
    private ChartDataService chartDataService;
    private UserService userService;
    private ProjectMemberQueryService memberQueryService;
    private ProjectQueryService projectQueryService;

    public static final int ES_TERMS_SIZE = 30;

    // 保留一位小数
    private static final Function<Double, Double> ROUND_DOUBLE_DECIMAL =
            d -> BigDecimal.valueOf(d).setScale(1, RoundingMode.HALF_EVEN).doubleValue();

    // 用户计算前的value格式化，avg计算后的结果未找到对应支持文档
    private static final Script SCRIPT_ROUND_DECIMAL = new Script("BigDecimal.valueOf(_value).setScale(1, RoundingMode.HALF_UP)");

    public Object chartData(String chartGroupType, Long companyId, List<Long> projectIds, EzInsightChart chart) throws Exception {
        List<Query> queries = new ArrayList<>();
        if (InsightChartGroupType.COMPANY.name().equals(chartGroupType)) {
            queries.add(Eq.builder().field(CardField.COMPANY_ID).value(String.valueOf(companyId)).build());
        } else {
            if (CollectionUtils.isEmpty(projectIds)) {
                throw new CodedException(HttpStatus.BAD_REQUEST, "未选择项目！");
            } else {
                queries.add(In.builder().field(CardField.PROJECT_ID).values(projectIds.stream().map(String::valueOf).collect(Collectors.toList())).build());
            }
        }
        queries.add(Eq.builder().field(CardField.DELETED).value(String.valueOf(false)).build());

        if (chart.getClass() == CardEndTrend.class) {
            return cardEndTrend(queries, (CardEndTrend) chart);
        }
        if (chart.getClass() == CardTrend.class) {
            return cardTrend(queries, (CardTrend) chart);
        }
        if (chart.getClass() == EzInsightCardsMultiTrend.class) {
            return cardMultiTrend(queries, (EzInsightCardsMultiTrend) chart);
        }
        if (chart.getClass() == BugTrend.class) {
            return bugTrend(queries, (BugTrend) chart);
        }
        if (chart.getClass() == CardSummary.class) {
            return cardSummary(queries, (CardSummary) chart);
        }
        if (chart.getClass() == CardOwnerTop.class) {
            return cardOwnerTop(queries, (CardOwnerTop) chart);
        }
        if (chart.getClass() == BugOwnerTop.class) {
            return bugOwnerTop(queries, (BugOwnerTop) chart);
        }
        if (chart.getClass() == CardDelayList.class) {
            return cardDelayList(queries, (CardDelayList) chart);
        }
        if (chart.getClass() == EzInsightOneDimensionTable.class) {
            return oneDimensionTable(queries, (EzInsightOneDimensionTable) chart);
        }
        if (chart.getClass() == EzInsightTwoDimensionTable.class) {
            return twoDimensionTable(queries, (EzInsightTwoDimensionTable) chart);
        }
        if (chart.getClass() == EzInsightCardsBar.class) {
            return cardsBar(queries, (EzInsightCardsBar) chart);
        }
        if (chart.getClass() == EzInsightCardsPie.class) {
            return cardsPie(queries, (EzInsightCardsPie) chart);
        }
        if (chart.getClass() == EzInsightCardsHistogram.class) {
            return cardsHistogram(queries, (EzInsightCardsHistogram) chart);
        }
        if (chart.getClass() == EzInsightUserGantt.class) {
            return userGantt(companyId, projectIds, queries, (EzInsightUserGantt) chart);
        }
        if (chart.getClass() == EzInsightProjectsSummaryTable.class) {
            return projectsSummaryTable(chartGroupType, companyId, projectIds, queries, (EzInsightProjectsSummaryTable) chart);
        }
        throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "未支持的报表类型！");
    }

    public Object cardEndTrend(List<Query> queries, CardEndTrend chart) throws IOException {
        DateRange range = chart.getDateRange();
        List<DateRange.TimeSpan> timeSpans = range.timeSpans(chart.getDateInterval());
        if (CollectionUtils.isNotEmpty(chart.getCardTypes())) {
            queries.add(In.builder().field(CardField.TYPE).values(chart.getCardTypes()).build());
        }
        if (chart.isExcludeNoPlan()) {
            queries.add(NotEq.builder().field(CardField.PLAN_ID).value("0").build());
        }
        Consumer<SearchSourceBuilder> sourceBuilderConsumer = builder -> buildCardEventDateBuckets(
                builder,
                timeSpans,
                AggregationBuilders
                        .filter("end", QueryBuilders.termQuery(CardEvent.cardProp(CardField.CALC_IS_END), true))
        );
        Aggregations aggs = cardEventDao.aggs(queries, sourceBuilderConsumer);

        Map<String, Object> result = new HashMap<>();
        result.put("x", timeSpans);
        result.put("data", new CardEndTrendParser().parse(aggs));
        return result;
    }

    public Object cardTrend(List<Query> queries, CardTrend chart) throws IOException {
        DateRange range = chart.getDateRange();
        List<DateRange.TimeSpan> timeSpans = range.timeSpans(chart.getDateInterval(), -1, 0);
        if (CollectionUtils.isNotEmpty(chart.getCardTypes())) {
            queries.add(In.builder().field(CardField.TYPE).values(chart.getCardTypes()).build());
        }
        if (chart.isExcludeNoPlan()) {
            queries.add(NotEq.builder().field(CardField.PLAN_ID).value("0").build());
        }
        Consumer<SearchSourceBuilder> sourceBuilderConsumer = builder -> buildCardEventDateBuckets(
                builder,
                timeSpans,
                AggregationBuilders
                        .filter("notEnd", QueryBuilders.termQuery(CardEvent.cardProp(CardField.CALC_IS_END), false))
                        .subAggregation(AggregationBuilders.terms("types").field(CardEvent.cardProp(CardField.TYPE))),
                AggregationBuilders
                        .filter("end", QueryBuilders.termQuery(CardEvent.cardProp(CardField.CALC_IS_END), true))

        );
        Aggregations aggs = cardEventDao.aggs(queries, sourceBuilderConsumer);

        Map<String, Object> result = new HashMap<>();
        result.put("x", timeSpans.subList(1, timeSpans.size()));
        result.put("data", new CardTrendParser().parse(aggs));
        return result;
    }

    public Object cardMultiTrend(List<Query> queries, EzInsightCardsMultiTrend chart) throws IOException {
        DateRange range = chart.getDateRange();
        List<DateRange.TimeSpan> timeSpans = range.timeSpans(chart.getDateInterval(), -1, 0);
        int[] index = new int[]{0};
        AggregationBuilder[] subAggs = chart.getYConfigs().stream()
                .filter(config -> CollectionUtils.isNotEmpty(config.getQueries()))
                .map(config -> {
                    BoolQueryBuilder bool = QueryBuilders.boolQuery();
                    config.getQueries().forEach(query -> bool.filter(query.queryBuilder(CardEvent.CARD_DETAIL)));
                    if (config.isExcludeNoPlan()) {
                        bool.filter(NotEq.builder().field(CardField.PLAN_ID).value("0").build().queryBuilder(CardEvent.CARD_DETAIL));
                    }
                    return AggregationBuilders.filter(String.valueOf(index[0]++), bool);
                })
                .toArray(AggregationBuilder[]::new);
        Consumer<SearchSourceBuilder> sourceBuilderConsumer = builder -> buildCardEventDateBuckets(
                builder,
                timeSpans,
                subAggs
        );
        Aggregations aggs = cardEventDao.aggs(queries, sourceBuilderConsumer);

        Map<String, Object> result = new HashMap<>();
        result.put("x", timeSpans.subList(1, timeSpans.size()));
        result.put("data", new CardMultiTrendParser().parse(chart, aggs));
        return result;
    }

    public Object bugTrend(List<Query> queries, BugTrend chart) throws IOException {
        DateRange range = chart.getDateRange();
        List<DateRange.TimeSpan> timeSpans = range.timeSpans(chart.getDateInterval(), -1, 0);
        if (CollectionUtils.isNotEmpty(chart.getBugCardTypes())) {
            queries.add(In.builder().field(CardField.TYPE).values(chart.getBugCardTypes()).build());
        }
        if (chart.isExcludeNoPlan()) {
            queries.add(NotEq.builder().field(CardField.PLAN_ID).value("0").build());
        }
        Consumer<SearchSourceBuilder> sourceBuilderConsumer = builder -> buildCardEventDateBuckets(
                builder,
                timeSpans,
                AggregationBuilders
                        .filter("notEnd", QueryBuilders.termQuery(CardEvent.cardProp(CardField.CALC_IS_END), false))
                        .subAggregation(AggregationBuilders.terms("importances").field(CardEvent.cardProp(CardField.IMPORTANCE)).missing(StringUtils.EMPTY)),
                AggregationBuilders
                        .filter("end", QueryBuilders.termQuery(CardEvent.cardProp(CardField.CALC_IS_END), true))

        );
        Aggregations aggs = cardEventDao.aggs(queries, sourceBuilderConsumer);

        Map<String, Object> result = new HashMap<>();
        result.put("x", timeSpans.subList(1, timeSpans.size()));
        result.put("data", new BugTrendParser().parse(aggs));
        return result;
    }

    public Object cardSummary(List<Query> queries, CardSummary chart) throws IOException {
        if (CollectionUtils.isNotEmpty(chart.getCardTypes())) {
            queries.add(In.builder().field(CardField.TYPE).values(chart.getCardTypes()).build());
        }
        if (chart.isExcludeNoPlan()) {
            queries.add(NotEq.builder().field(CardField.PLAN_ID).value("0").build());
        }
        Consumer<SearchSourceBuilder> cardBuilderConsumer = builder -> builder
                .aggregation(AggregationBuilders.terms("isEnd").field(CardField.CALC_IS_END).missing(false))
                .aggregation(AggregationBuilders.terms("blocked").field(CardField.BLOCKED).missing(false))
                .aggregation(AggregationBuilders
                        .filter("ownerUsers", QueryBuilders.existsQuery(CardField.OWNER_USERS))
                        .subAggregation(AggregationBuilders.cardinality("ownerUsers").field(CardField.OWNER_USERS))
                )
                .aggregation(AggregationBuilders
                        .filter("bug", QueryBuilders.termQuery(CardField.INNER_TYPE, InnerCardType.bug.name()))
                        .subAggregation(AggregationBuilders.terms("isEnd").field(CardField.CALC_IS_END).missing(false))
                );

        Aggregations aggs = cardDao.aggs(queries, cardBuilderConsumer);

        return new CardSummaryParser().parse(aggs);
    }

    public Object cardOwnerTop(List<Query> queries, CardOwnerTop chart) throws IOException {
        DateRange range = chart.getDateRange();
        if (chart.getFilterType() == CardOwnerTop.FilterType.END) {
            queries.add(Eq.builder().field(CardField.CALC_IS_END).value(String.valueOf(true)).build());
            queries.add(Between.builder().field(CardField.LAST_END_TIME).start(String.valueOf(range.start().getTime())).end(String.valueOf(range.end().getTime())).build());
        } else {
            queries.add(Between.builder().field(CardField.CREATE_TIME).start(String.valueOf(range.start().getTime())).end(String.valueOf(range.end().getTime())).build());
        }
        if (CollectionUtils.isNotEmpty(chart.getCardTypes())) {
            queries.add(In.builder().field(CardField.TYPE).values(chart.getCardTypes()).build());
        }
        if (chart.isExcludeNoPlan()) {
            queries.add(NotEq.builder().field(CardField.PLAN_ID).value("0").build());
        }
        Consumer<SearchSourceBuilder> cardBuilderConsumer = builder -> builder
                .aggregation(AggregationBuilders.terms("ownerUsers").field(CardField.OWNER_USERS).size(20));

        Aggregations aggs = cardDao.aggs(queries, cardBuilderConsumer);
        return new CardOwnerTopParser().parse(aggs);
    }

    public Object bugOwnerTop(List<Query> queries, BugOwnerTop chart) throws IOException {
        DateRange range = chart.getDateRange();
        if (CollectionUtils.isNotEmpty(chart.getBugCardTypes())) {
            queries.add(In.builder().field(CardField.TYPE).values(chart.getBugCardTypes()).build());
        }
        if (chart.getFilterType() == CardOwnerTop.FilterType.END) {
            queries.add(Eq.builder().field(CardField.CALC_IS_END).value(String.valueOf(true)).build());
            queries.add(Between.builder().field(CardField.LAST_END_TIME).start(String.valueOf(range.start().getTime())).end(String.valueOf(range.end().getTime())).build());
        } else {
            queries.add(Between.builder().field(CardField.CREATE_TIME).start(String.valueOf(range.start().getTime())).end(String.valueOf(range.end().getTime())).build());
        }
        if (chart.isExcludeNoPlan()) {
            queries.add(NotEq.builder().field(CardField.PLAN_ID).value("0").build());
        }
        Consumer<SearchSourceBuilder> cardBuilderConsumer = builder -> builder
                .aggregation(AggregationBuilders.terms("ownerUsers").field(CardField.OWNER_USERS).size(20));

        Aggregations aggs = cardDao.aggs(queries, cardBuilderConsumer);
        return new CardOwnerTopParser().parse(aggs);
    }

    private static final String[] DELAY_CARD_FIELDS = new String[]{
            CardField.SEQ_NUM, CardField.TITLE, CardField.PROJECT_ID,
            CardField.CREATE_USER, CardField.OWNER_USERS, CardField.STATUS, CardField.TYPE,
            CardField.END_DATE, CardField.LAST_END_TIME, CardField.CREATE_TIME
    };

    public Object cardDelayList(List<Query> queries, CardDelayList chart) throws IOException {
        if (CollectionUtils.isNotEmpty(chart.getCardTypes())) {
            queries.add(In.builder().field(CardField.TYPE).values(chart.getCardTypes()).build());
        }
        queries.add(Eq.builder().field(CardField.CALC_IS_END).value(String.valueOf(chart.isEnd())).build());
        if (chart.isEnd()) {
            queries.add(Eq.builder().field(CardField.LAST_END_DELAY).value(String.valueOf(chart.isEnd())).build());
        } else {
            queries.add(Lt.builder().field(CardField.END_DATE).value(String.valueOf(System.currentTimeMillis())).build());
        }
        if (chart.isExcludeNoPlan()) {
            queries.add(NotEq.builder().field(CardField.PLAN_ID).value("0").build());
        }
        DateRange range = chart.getDateRange();
        if (range != null) {
            queries.add(Between.builder().field(CardField.CREATE_TIME).start(String.valueOf(range.start().getTime())).end(String.valueOf(range.end().getTime())).build());
        }
        SearchEsRequest search = SearchEsRequest.builder()
                .queries(queries)
                .fields(DELAY_CARD_FIELDS)
                .build();
        TotalBean<CardBean> totalBean = cardSearchService.search(search, chart.getPageNumber(), chart.getPageSize());
        cardReferenceValueHelper.tryLoadProjectSchemas(totalBean.getRefs(), search.getFields());
        return totalBean;
    }

    public Object oneDimensionTable(List<Query> queries, EzInsightOneDimensionTable chart) throws Exception {
        if (chart.isExcludeNoPlan()) {
            queries.add(NotEq.builder().field(CardField.PLAN_ID).value("0").build());
        }
        return chartDataService.oneDimensionTableData(chart, union(queries, chart.getQueries()), missingValue(chart.getClassifyField()));
    }

    public Object twoDimensionTable(List<Query> queries, EzInsightTwoDimensionTable chart) throws Exception {
        if (chart.isExcludeNoPlan()) {
            queries.add(NotEq.builder().field(CardField.PLAN_ID).value("0").build());
        }
        return chartDataService.twoDimensionTableData(chart, union(queries, chart.getQueries()), missingValue(chart.getClassifyXField()), missingValue(chart.getClassifyYField()));
    }

    public Object cardsBar(List<Query> queries, EzInsightCardsBar chart) throws Exception {
        if (chart.isExcludeNoPlan()) {
            queries.add(NotEq.builder().field(CardField.PLAN_ID).value("0").build());
        }
        return oneDimensionTable(
                queries,
                EzInsightOneDimensionTable.builder()
                        .queries(chart.getQueries())
                        .classifyField(chart.getClassifyField())
                        .metricType(MetricType.CountCard)
                        .build()
        );
    }

    public Object cardsPie(List<Query> queries, EzInsightCardsPie chart) throws Exception {
        if (chart.isExcludeNoPlan()) {
            queries.add(NotEq.builder().field(CardField.PLAN_ID).value("0").build());
        }
        return oneDimensionTable(
                queries,
                chart
        );
    }

    public Object cardsHistogram(List<Query> queries, EzInsightCardsHistogram chart) throws Exception {
        if (chart.isExcludeNoPlan()) {
            queries.add(NotEq.builder().field(CardField.PLAN_ID).value("0").build());
        }
        return chartDataService.cardsHistogramChartData(chart, union(queries, chart.getQueries()), missingValue(chart.getClassifyYField()));
    }

    public Object userGantt(Long companyId, List<Long> projectIds, List<Query> queries, EzInsightUserGantt chart) throws Exception {
        Map<String, Object> result = new HashMap<>();
        if (CollectionUtils.isEmpty(projectIds)) {
            return result;
        }
        result.put("projects", projectQueryService.select(projectIds));
        Set<String> users = memberQueryService.selectProjectMemberFinalUsers(companyId, projectIds, 200);
        result.put("users", users);
        if (CollectionUtils.isNotEmpty(users)) {
            DateRange range = chart.getDateRange();
            queries.add(Lte.builder().field(CardField.START_DATE).value(String.valueOf(range.end().getTime())).build());
            queries.add(Gte.builder().field(CardField.END_DATE).value(String.valueOf(range.start().getTime())).build());
            queries.add(In.builder().field(CardField.OWNER_USERS).values(new ArrayList<>(users)).build());
            result.put("cards", cardDao.searchAsMap(
                    queries,
                    CardField.OWNER_USERS, CardField.PROJECT_ID, CardField.TITLE, CardField.SEQ_NUM, CardField.START_DATE, CardField.END_DATE, CardField.ESTIMATE_WORKLOAD));
        }
        return result;
    }

    public Object projectsSummaryTable(String chartGroupType, Long companyId, List<Long> projectIds, List<Query> queries, EzInsightProjectsSummaryTable chart) throws Exception {
        if (CollectionUtils.isNotEmpty(chart.getCardTypes())) {
            queries.add(In.builder().field(CardField.TYPE).values(chart.getCardTypes()).build());
        }
        Map<Long, Map<EzInsightProjectsSummaryTable.Metric, Number>> projectMetricsValues = new HashMap<>();
        Set<EzInsightProjectsSummaryTable.Metric> cardMetrics = chart.metrics(EzInsightProjectsSummaryTable.MetricSource.CARD);
        if (CollectionUtils.isNotEmpty(cardMetrics)) {
            TermsAggregationBuilder terms = AggregationBuilders
                    .terms("projects")
                    .field(CardEvent.cardProp(CardField.PROJECT_ID))
                    .size(100);
            chart.cardMetricsAggs().forEach(terms::subAggregation);
            Date timestamp = chart.getEndDay() == null ? new Date() : new DateTime(chart.getEndDay()).millisOfDay().withMaximumValue().toDate();
            BoolQueryBuilder bool = CardEvent.timeQueryBuilder(timestamp);
            FilterAggregationBuilder filter = AggregationBuilders
                    .filter("filter", bool)
                    .subAggregation(terms);
            Consumer<SearchSourceBuilder> sourceBuilderConsumer = builder -> builder.aggregation(filter);
            Aggregations aggs = cardEventDao.aggs(queries, sourceBuilderConsumer);
            projectMetricsValues = new ProjectsSummaryTableParser().parse(aggs);
        }
        final List<Long> finalProjectIds = "COMPANY".equals(chartGroupType) ? new ArrayList<>(projectMetricsValues.keySet()) : projectIds;

        Map<Long, Map<EzInsightProjectsSummaryTable.Column, Number>> projectColumnsValues = new HashMap<>();
        List<EzInsightProjectsSummaryTable.Column> columns = chart.getColumns();
        for (Map.Entry<Long, Map<EzInsightProjectsSummaryTable.Metric, Number>> e : projectMetricsValues.entrySet()) {
            Map<EzInsightProjectsSummaryTable.Metric, Number> metricValues = projectMetricsValues.get(e.getKey());
            Map<EzInsightProjectsSummaryTable.Column, Number> columnValues = columns.stream().collect(CollectorsV2.toMap(
                    Function.identity(),
                    column -> column.calc.apply(metricValues::get)));
            projectColumnsValues.put(e.getKey(), columnValues);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("projects", projectQueryService.select(finalProjectIds));
        result.put("projectColumnsValues", projectColumnsValues);
        return result;
    }

    private List<Query> union(List<Query> queries1, List<Query> queries2) {
        List<Query> queries = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(queries1)) {
            queries.addAll(queries1);
        }
        if (CollectionUtils.isNotEmpty(queries2)) {
            queries.addAll(queries2);
        }
        return queries;
    }

    private void buildCardEventDateBuckets(
            SearchSourceBuilder builder,
            List<DateRange.TimeSpan> timeSpans,
            AggregationBuilder... sugAggs) {
        for (DateRange.TimeSpan timeSpan : timeSpans) {
            Date end = timeSpan.getEnd();
            FilterAggregationBuilder filter = AggregationBuilders.filter(timeSpan.getStartAsString(), CardEvent.timeQueryBuilder(end));
            for (AggregationBuilder sugAgg : sugAggs) {
                filter.subAggregation(sugAgg);
            }
            builder.aggregation(filter);
        }
    }

    private Object missingValue(String fieldKey) {
        ProjectCardSchema schema = projectCardSchemaHelper.getSysProjectCardSchema();
        CardField field = schema.findCardField(fieldKey);
        if (field == null) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "未找到指定分类字段！");
        }
        switch (field.getValueType()) {
            case BOOLEAN:
                return false;
            case LONG:
            case LONGS:
            case FLOAT:
            case DATE:
                return 0;
            default:
                return StringUtils.EMPTY;
        }
    }
}
