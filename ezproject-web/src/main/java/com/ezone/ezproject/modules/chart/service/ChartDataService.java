package com.ezone.ezproject.modules.chart.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.template.JsTemplate;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.dao.CardEventDao;
import com.ezone.ezproject.es.dao.ChartDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardStatus;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.enums.InnerCardType;
import com.ezone.ezproject.modules.card.bean.CardBean;
import com.ezone.ezproject.modules.card.bean.ReferenceValues;
import com.ezone.ezproject.modules.card.bean.query.And;
import com.ezone.ezproject.modules.card.bean.query.Between;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.bean.query.Gte;
import com.ezone.ezproject.modules.card.bean.query.In;
import com.ezone.ezproject.modules.card.bean.query.Lte;
import com.ezone.ezproject.modules.card.bean.query.NotEq;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.card.event.service.CardEventQueryService;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.service.CardHelper;
import com.ezone.ezproject.modules.card.service.CardQueryService;
import com.ezone.ezproject.modules.card.service.CardReferenceValueHelper;
import com.ezone.ezproject.modules.chart.config.BurnDown;
import com.ezone.ezproject.modules.chart.config.BurnUp;
import com.ezone.ezproject.modules.chart.config.CardsBar;
import com.ezone.ezproject.modules.chart.config.CardsHistogramChart;
import com.ezone.ezproject.modules.chart.config.CardsMultiTrend;
import com.ezone.ezproject.modules.chart.config.CardsPie;
import com.ezone.ezproject.modules.chart.config.CardsProgressBurnUp;
import com.ezone.ezproject.modules.chart.config.CardsSummary;
import com.ezone.ezproject.modules.chart.config.CardsTrend;
import com.ezone.ezproject.modules.chart.config.Chart;
import com.ezone.ezproject.modules.chart.config.CumulativeFlowDiagram;
import com.ezone.ezproject.modules.chart.config.OneDimensionTable;
import com.ezone.ezproject.modules.chart.config.PlanMeasure;
import com.ezone.ezproject.modules.chart.config.TeamRateChart;
import com.ezone.ezproject.modules.chart.config.TwoDimensionTable;
import com.ezone.ezproject.modules.chart.config.UserGantt;
import com.ezone.ezproject.modules.chart.config.enums.DateInterval;
import com.ezone.ezproject.modules.chart.config.enums.MetricType;
import com.ezone.ezproject.modules.chart.config.range.DateBetweenRange;
import com.ezone.ezproject.modules.chart.config.range.DateRange;
import com.ezone.ezproject.modules.chart.data.GanttChart;
import com.ezone.ezproject.modules.chart.ezinsight.data.CardSummaryParser;
import com.ezone.ezproject.modules.chart.ezinsight.data.OneDimensionTableParser;
import com.ezone.ezproject.modules.chart.ezinsight.data.ProjectCardSummaryParser;
import com.ezone.ezproject.modules.chart.ezinsight.data.TwoDimensionTableParser;
import com.ezone.ezproject.modules.chart.helper.CFD;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import com.ezone.ezproject.modules.portfolio.bean.ProjectCardSummary;
import com.ezone.ezproject.modules.project.data.BurnDownParser;
import com.ezone.ezproject.modules.project.data.BurnUpParser;
import com.ezone.ezproject.modules.project.data.CFDParser;
import com.ezone.ezproject.modules.project.data.CardMultiTrendParser;
import com.ezone.ezproject.modules.project.data.ProgressBurnUpParser;
import com.ezone.ezproject.modules.project.data.ProjectCardTrendParser;
import com.ezone.ezproject.modules.project.service.ProjectMemberQueryService;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
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
public class ChartDataService {
    private ChartDao chartDao;

    private CardDao cardDao;
    private CardQueryService cardQueryService;
    private PlanQueryService planQueryService;
    private CardEventQueryService cardEventQueryService;
    private CardEventDao cardEventDao;

    private ProjectSchemaQueryService projectSchemaQueryService;
    private ProjectMemberQueryService memberQueryService;
    private ProjectQueryService projectQueryService;

    private CardReferenceValueHelper referenceValueHelper;

    public static final int ES_TERMS_SIZE = 30;

    // 保留一位小数
    private static final Function<Double, Double> ROUND_DOUBLE_DECIMAL =
            d -> BigDecimal.valueOf(d).setScale(1, RoundingMode.HALF_EVEN).doubleValue();

    // 用户计算前的value格式化，avg计算后的结果未找到对应支持文档
    private static final Script SCRIPT_ROUND_DECIMAL = new Script("BigDecimal.valueOf(_value).setScale(1, RoundingMode.HALF_UP)");

    public GanttChart ganttData(Long projectId) throws IOException {
        List<Plan> plans = planQueryService.selectByProjectId(projectId, true);
        if (CollectionUtils.isEmpty(plans)) {
            return GanttChart.builder().build();
        }
        List<Card> cards = cardQueryService.selectByPlanIds(plans.stream().map(Plan::getId).collect(Collectors.toList()));
        if (CollectionUtils.isEmpty(cards)) {
            return GanttChart.builder().plans(plans).build();
        }
        List<CardBean> cardBeans = cardDao.findAsMap(cards.stream().map(Card::getId).collect(Collectors.toList()),
                CardField.PLAN_ID, CardField.TYPE, CardField.TITLE, CardField.OWNER_USERS,
                CardField.ESTIMATE_WORKLOAD, CardField.REMAIN_WORKLOAD,
                CardField.START_DATE, CardField.END_DATE
        ).entrySet().stream().map(e -> CardBean.builder().id(e.getKey()).card(e.getValue()).build()).collect(Collectors.toList());
        return GanttChart.builder().plans(plans).cardBeans(cardBeans).build();
    }

    public Object planCardsProgressBurn(Plan plan, boolean containsDescendantPlan, List<Query> queries, String metricField, MetricType metricType) throws Exception {
        List<Long> planIds = new ArrayList<>();
        planIds.add(plan.getId());
        if (containsDescendantPlan) {
            List<Plan> plans = planQueryService.selectDescendant(plan, true);
            if (CollectionUtils.isNotEmpty(plans)) {
                plans.forEach(p -> planIds.add(p.getId()));
            }
        }
        List<Query> finalQueries = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(queries)) {
            finalQueries.addAll(queries);
        }
        finalQueries.add(In.builder().field(CardField.PLAN_ID).values(planIds.stream().map(String::valueOf).collect(Collectors.toList())).build());
        finalQueries.add(Eq.builder().field(CardField.DELETED).value(String.valueOf(false)).build());
        return burnUp(
                plan.getProjectId(),
                BurnUp.builder()
                        .dateInterval(DateInterval.DAY)
                        .dateRange(DateBetweenRange.builder().start(plan.getStartTime()).end(DateUtils.addDays(plan.getEndTime(), 1)).build())
                        .metricField(metricField)
                        .metricType(metricType)
                        .build(),
                finalQueries);
    }

    public Object chartData(Long projectId, Long id) throws Exception {
        Chart chart = chartDao.find(id);
        List<Query> queries = new ArrayList<>();
        queries.add(Eq.builder().field(CardField.PROJECT_ID).value(String.valueOf(projectId)).build());
        queries.add(Eq.builder().field(CardField.DELETED).value(String.valueOf(false)).build());
        if (CollectionUtils.isNotEmpty(chart.getQueries())) {
            queries.addAll(chart.getQueries());
        }
        if (chart.isExcludeNoPlan()) {
            queries.add(NotEq.builder().field(CardField.PLAN_ID).value("0").build());
        }
        Function<String, Object> missingValue = field -> missingValue(projectSchemaQueryService.getProjectCardSchema(projectId), field);
        if (chart.getConfig().getClass() == OneDimensionTable.class) {
            OneDimensionTable oneDimensionTable = (OneDimensionTable) chart.getConfig();
            return oneDimensionTableData(oneDimensionTable, queries, missingValue.apply(oneDimensionTable.getClassifyField()));
        } else if (chart.getConfig().getClass() == TwoDimensionTable.class) {
            TwoDimensionTable twoDimensionTable = (TwoDimensionTable) chart.getConfig();
            ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
            return twoDimensionTableData(twoDimensionTable, queries, missingValue(schema, twoDimensionTable.getClassifyXField()), missingValue(schema, twoDimensionTable.getClassifyYField()));
        } else if (chart.getConfig().getClass() == CardsBar.class) {
            CardsBar cardsBar = (CardsBar) chart.getConfig();
            return oneDimensionTableData(OneDimensionTable.builder()
                    .classifyField(cardsBar.getClassifyField())
                    .metricType(MetricType.CountCard)
                    .build(), queries, missingValue.apply(cardsBar.getClassifyField()));
        } else if (chart.getConfig().getClass() == CardsPie.class) {
            CardsPie cardsPie = (CardsPie) chart.getConfig();
            return oneDimensionTableData(cardsPie, queries, missingValue.apply(cardsPie.getClassifyField()));
        } else if (chart.getConfig().getClass() == CardsHistogramChart.class) {
            CardsHistogramChart cardsHistogramChart = (CardsHistogramChart) chart.getConfig();
            return cardsHistogramChartData(cardsHistogramChart, queries, missingValue.apply(cardsHistogramChart.getClassifyYField()));
        } else if (chart.getConfig().getClass() == CardsProgressBurnUp.class) {
            CardsProgressBurnUp cardsProgressBurnUp = (CardsProgressBurnUp) chart.getConfig();
            return cardsProgressBurnUp(cardsProgressBurnUp, queries);
        } else if (chart.getConfig().getClass() == CardsTrend.class) {
            CardsTrend cardsTrend = (CardsTrend) chart.getConfig();
            return cardsTrend(projectId, cardsTrend, queries);
        } else if (chart.getConfig().getClass() == CardsMultiTrend.class) {
            CardsMultiTrend cardsMultiTrend = (CardsMultiTrend) chart.getConfig();
            return cardsMultiTrend(projectId, queries, cardsMultiTrend);
        } else if (chart.getConfig().getClass() == PlanMeasure.class) {
            PlanMeasure planMeasure = (PlanMeasure) chart.getConfig();
            return planMeasure(projectId, planMeasure, queries);
        } else if (chart.getConfig().getClass() == TeamRateChart.class) {
            TeamRateChart teamRateChart = (TeamRateChart) chart.getConfig();
            return teamRateChart(projectId, teamRateChart, queries);
        } else if (chart.getConfig().getClass() == CumulativeFlowDiagram.class) {
            CumulativeFlowDiagram cumulativeFlowDiagram = (CumulativeFlowDiagram) chart.getConfig();
            return cumulativeFlowDiagram(projectId, cumulativeFlowDiagram, queries);
        } else if (chart.getConfig().getClass() == BurnUp.class) {
            BurnUp burnUp = (BurnUp) chart.getConfig();
            return burnUp(projectId, burnUp, queries);
        } else if (chart.getConfig().getClass() == BurnDown.class) {
            BurnDown burnDown = (BurnDown) chart.getConfig();
            return burnDown(projectId, burnDown, queries);
        } else if (chart.getConfig().getClass() == CardsSummary.class) {
            CardsSummary cardsSummary = (CardsSummary) chart.getConfig();
            return cardsSummary(projectId, cardsSummary, queries);
        } else if (chart.getConfig().getClass() == UserGantt.class) {
            UserGantt userGantt = (UserGantt) chart.getConfig();
            return userGantt(projectId, userGantt, queries);
        }
        return null;
    }

    /**
     * 事项数据概览
     *
     * @param projectId
     * @param cardSummary
     * @param queries
     * @return
     */
    private Object cardsSummary(Long projectId, CardsSummary cardSummary, List<Query> queries) {
        List<Query> all = new ArrayList<>();
        all.add(Eq.builder().field(CardField.PROJECT_ID).value(String.valueOf(projectId)).build());
        all.add(In.builder().field(CardField.TYPE).values(cardSummary.getCardTypes()).build());
        if (CollectionUtils.isNotEmpty(queries)) {
            all.addAll(queries);
        }
        Consumer<SearchSourceBuilder> cardBuilderConsumer = builder -> builder
                .aggregation(AggregationBuilders.terms("isEnd").field(CardField.CALC_IS_END).missing(false))
                .aggregation(AggregationBuilders.terms("blocked").field(CardField.BLOCKED).missing(false))
                .aggregation(AggregationBuilders
                        .filter("ownerUsers", QueryBuilders.existsQuery(CardField.OWNER_USERS))
                        .subAggregation(AggregationBuilders.cardinality("ownerUsers").field(CardField.OWNER_USERS))
                )
                .aggregation(AggregationBuilders
                        .filter("bug", QueryBuilders.termQuery(CardField.INNER_TYPE, InnerCardType.bug))
                        .subAggregation(AggregationBuilders.terms("isEnd").field(CardField.CALC_IS_END).missing(false))
                );

        Aggregations aggs = null;
        try {
            aggs = cardDao.aggs(all, cardBuilderConsumer);
        } catch (Exception e) {
            log.error("cardSummary exception!", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        return new CardSummaryParser().parse(aggs);
    }

    public Object userGantt(Long projectId, UserGantt chart, List<Query> queries) throws Exception {
        Map<String, Object> result = new HashMap<>();
        Set<String> users = memberQueryService.selectProjectMemberFinalUsers(projectQueryService.getProjectCompany(projectId), Arrays.asList(projectId), 200);
        result.put("users", users);
        if (CollectionUtils.isNotEmpty(users)) {
            DateRange range = chart.getDateRange();
            queries.add(Lte.builder().field(CardField.START_DATE).value(String.valueOf(range.end().getTime())).build());
            queries.add(Gte.builder().field(CardField.END_DATE).value(String.valueOf(range.start().getTime())).build());
            queries.add(In.builder().field(CardField.OWNER_USERS).values(new ArrayList<>(users)).build());
            result.put("cards", cardDao.searchAsMap(
                    queries,
                    CardField.OWNER_USERS, CardField.TYPE, CardField.TITLE, CardField.SEQ_NUM, CardField.START_DATE, CardField.END_DATE, CardField.ESTIMATE_WORKLOAD));
        }
        return result;
    }

    public Object oneDimensionTableData(OneDimensionTable config, List<Query> queries, Object xMissingValue) throws Exception {
        Function<String, AggregationBuilder> metricAggBuild;
        switch (config.getMetricType()) {
            case SumCardField:
                metricAggBuild = name -> AggregationBuilders.sum(name).field(config.getMetricField());
                break;
            case AvgCardField:
                metricAggBuild = name -> AggregationBuilders.avg(name).field(config.getMetricField()).missing(0);
                break;
            default:
                metricAggBuild = name -> AggregationBuilders.count(name).field(CardField.SEQ_NUM);
        }
        Consumer<SearchSourceBuilder> aggregation = builder -> builder
                .aggregation(metricAggBuild.apply("total"))
                .aggregation(AggregationBuilders.count("count").field(CardField.SEQ_NUM))
                .aggregation(AggregationBuilders
                        .terms("X").field(config.getClassifyField()).missing(xMissingValue).size(ES_TERMS_SIZE)
                        .subAggregation(metricAggBuild.apply("X"))
                );
        Map result = new OneDimensionTableParser().parse(cardDao.aggs(queries, aggregation));
        ReferenceValues refs = new ReferenceValues();
        referenceValueHelper.refValues(refs, config.getClassifyField(), (List) result.get("xValues"));
        result.put("refs", refs);
        return result;
    }

    public Object twoDimensionTableData(TwoDimensionTable config, List<Query> queries, Object xMissingValue, Object yMissingValue) throws Exception {
        Function<String, AggregationBuilder> metricAggBuild;
        switch (config.getMetricType()) {
            case SumCardField:
                metricAggBuild = name -> AggregationBuilders.sum(name).field(config.getMetricField());
                break;
            case AvgCardField:
                metricAggBuild = name -> AggregationBuilders.avg(name).field(config.getMetricField()).missing(0);
                break;
            default:
                metricAggBuild = name -> AggregationBuilders.count(name).field(CardField.SEQ_NUM);
        }
        Consumer<SearchSourceBuilder> aggregation = builder -> builder
                .aggregation(metricAggBuild.apply("total"))
                .aggregation(AggregationBuilders.count("count").field(CardField.SEQ_NUM))
                .aggregation(AggregationBuilders
                        .terms("X").field(config.getClassifyXField()).missing(xMissingValue).size(ES_TERMS_SIZE)
                        .subAggregation(metricAggBuild.apply("X"))
                        .subAggregation(AggregationBuilders
                                .terms("Y").field(config.getClassifyYField()).missing(yMissingValue).size(ES_TERMS_SIZE)
                                .subAggregation(metricAggBuild.apply("Y"))
                        )
                )
                .aggregation(AggregationBuilders
                        .terms("Y").field(config.getClassifyYField()).missing(yMissingValue).size(ES_TERMS_SIZE)
                        .subAggregation(metricAggBuild.apply("Y"))
                );
        Map result = new TwoDimensionTableParser().parse(cardDao.aggs(queries, aggregation));
        ReferenceValues refs = new ReferenceValues();
        referenceValueHelper.refValues(refs, config.getClassifyXField(), (List) result.get("xValues"));
        referenceValueHelper.refValues(refs, config.getClassifyYField(), (List) result.get("yValues"));
        result.put("refs", refs);
        return result;
    }

    public Object cardsHistogramChartData(CardsHistogramChart config, List<Query> queries, Object yMissingValue) throws Exception {
        Consumer<SearchSourceBuilder> aggregation = builder -> builder
                .aggregation(AggregationBuilders
                        .dateHistogram("X").field(CardField.CREATE_TIME)
                        .calendarInterval(config.getDateInterval().getEsInterval()).format("yyyy-MM-dd")
                        .subAggregation(AggregationBuilders
                                .terms("Y").field(config.getClassifyYField()).missing(yMissingValue).size(ES_TERMS_SIZE)
                                .subAggregation(AggregationBuilders.count("Y").field(CardField.SEQ_NUM))
                        )
                );
        DateRange range = config.getDateRange();
        queries.add(Between.builder()
                .field(CardField.CREATE_TIME)
                .start(String.valueOf(range.start().getTime()))
                .end(String.valueOf(range.end().getTime()))
                .build());
        String agg = cardDao.agg(queries, aggregation);
        String json = JsTemplate.render("/js/CardsHistogramChart.js").render(agg);
        Map<String, Object> result = CardDao.JSON_MAPPER.readValue(json, Map.class);
        ReferenceValues refs = new ReferenceValues();
        referenceValueHelper.refValues(refs, config.getClassifyYField(), (List) result.get("yValues"));
        result.put("refs", refs);
        return result;
    }

    public Object cardsProgressBurnUp(CardsProgressBurnUp config, List<Query> queries) throws IOException {
        DateRange range = config.getDateRange();
        List<DateRange.TimeSpan> timeSpans = range.timeSpans(config.getDateInterval());
        Function<String, AggregationBuilder> metricAggBuild;
        switch (config.getMetricType()) {
            case SumCardField:
                metricAggBuild = name -> AggregationBuilders.sum(name).field(CardEvent.cardProp(config.getMetricField()));
                break;
            case AvgCardField:
                metricAggBuild = name -> AggregationBuilders.avg(name).field(CardEvent.cardProp(config.getMetricField())).missing(0);
                break;
            default:
                metricAggBuild = name -> AggregationBuilders.count(name).field(CardEvent.cardProp(CardField.SEQ_NUM));
        }
        Consumer<SearchSourceBuilder> sourceBuilderConsumer = builder -> buildCardEventDateBuckets(
                builder,
                timeSpans,
                AggregationBuilders
                        .terms("statuses").field(CardEvent.cardProp(CardField.STATUS)).size(ES_TERMS_SIZE)
                        .subAggregation(metricAggBuild.apply("metric"))

        );
        Aggregations aggs = cardEventDao.aggs(queries, sourceBuilderConsumer);

        Map<String, Object> result = new HashMap<>();
        result.put("x", timeSpans);
        result.put("y", new ProgressBurnUpParser().parse(aggs));
        return result;
    }

    public Object cardsTrend(Long projectId, CardsTrend config, List<Query> queries) throws Exception {
        DateRange range = config.getDateRange();
        List<DateRange.TimeSpan> timeSpans = range.timeSpans(DateInterval.DAY, -1, 0);
        Consumer<SearchSourceBuilder> sourceBuilderConsumer = builder -> buildCardEventDateBuckets(
                builder,
                timeSpans,
                AggregationBuilders
                        .filter("end", QueryBuilders.termQuery(CardEvent.cardProp(CardField.CALC_IS_END), true))

        );
        Aggregations aggs = cardEventDao.aggs(queries, sourceBuilderConsumer);
        Map<String, Object> result = new HashMap<>();
        result.put("x", timeSpans);
        result.putAll(new ProjectCardTrendParser().parse(aggs));
        return result;
    }

    public Object cardsMultiTrend(Long projectId, List<Query> baseQueries, CardsMultiTrend config) throws Exception {
        DateRange range = config.getDateRange();
        List<DateRange.TimeSpan> timeSpans = range.timeSpans(config.getDateInterval());
        Consumer<SearchSourceBuilder> sourceBuilderConsumer = builder -> {
            for (int i = 0; i < config.getYConfigs().size(); i++) {
                CardsMultiTrend.YConfig yConfig = config.getYConfigs().get(i);
                AggregationBuilder filter;
                if (CollectionUtils.isNotEmpty(yConfig.getQueries())) {
                    filter = AggregationBuilders
                            .filter(String.valueOf(i), And.builder().queries(yConfig.getQueries()).build().queryBuilder(CardEvent.CARD_DETAIL));
                } else {
                    filter = AggregationBuilders
                            .filter(String.valueOf(i), QueryBuilders.boolQuery());
                }
                buildCardEventDateBuckets(filter, timeSpans);
                builder.aggregation(filter);
            }
        };
        Aggregations aggs = cardEventDao.aggs(baseQueries, sourceBuilderConsumer);
        Map<String, Object> result = new HashMap<>();
        result.put("x", timeSpans);
        result.put("yValues", new CardMultiTrendParser().parse(aggs, config.getYConfigs()));
        return result;
    }

    public Object planMeasure(Long projectId, PlanMeasure config, List<Query> queries) throws Exception {
        queries.add(In.builder().field(CardField.PLAN_ID).values(
                        config.getPlanIds().stream().map(String::valueOf).collect(Collectors.toList()))
                .build());
        List<String> cardProps = new ArrayList<>(Arrays.asList(CardField.CALC_IS_END, CardField.PLAN_ID));
        Function<List<Map<String, Object>>, Number> metric;
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        if (MetricType.SumCardField.equals(config.getMetricType())) {
            cardProps.add(config.getMetricField());
            metric = cards -> cards.stream()
                    .mapToDouble(card -> FieldUtil.toFloat(card.get(config.getMetricField())))
                    .sum();
        } else {
            metric = List::size;
        }
        List<Map<String, Object>> cardDetails = cardDao.searchAsMap(queries, cardProps);
        Map<Long, List<Map<String, Object>>> planCards = cardDetails.stream()
                .collect(Collectors.groupingBy(card -> FieldUtil.toLong(card.get(CardField.PLAN_ID))));
        List<Double> data = config.getPlanIds().stream().map(planId -> {
            List<Map<String, Object>> cards = planCards.get(planId);
            if (CollectionUtils.isEmpty(cards)) {
                return null;
            }
            double all = metric.apply(cards).doubleValue();
            if (all > 0) {
                double end = metric.apply(cards.stream().filter(CardHelper::isEnd).collect(Collectors.toList())).doubleValue();
                return ROUND_DOUBLE_DECIMAL.apply(end / all * 100);
            }
            return null;
        }).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("x", config.getPlanIds());
        result.put("y", data);
        result.put("plans", planQueryService.select(config.getPlanIds()));
        return result;
    }

    public Object teamRateChart(Long projectId, TeamRateChart config, List<Query> queries) throws Exception {
        queries.add(In.builder().field(CardField.PLAN_ID).values(
                        config.getPlanIds().stream().map(String::valueOf).collect(Collectors.toList()))
                .build());
        List<String> cardProps = new ArrayList<>(Arrays.asList(CardField.CALC_IS_END, CardField.PLAN_ID));
        Function<List<Map<String, Object>>, Number> metric;
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        if (MetricType.SumCardField.equals(config.getMetricType())) {
            cardProps.add(config.getMetricField());
            metric = cards -> cards.stream()
                    .mapToDouble(card -> FieldUtil.toFloat(card.get(config.getMetricField())))
                    .sum();
        } else {
            metric = List::size;
        }
        List<Map<String, Object>> cardDetails = cardDao.searchAsMap(queries, cardProps);
        Map<Long, List<Map<String, Object>>> planCards = cardDetails.stream()
                .collect(Collectors.groupingBy(card -> FieldUtil.toLong(card.get(CardField.PLAN_ID))));
        List<Number> allMetrics = new ArrayList<>();
        List<Number> endMetrics = new ArrayList<>();
        config.getPlanIds().stream().forEach(planId -> {
            List<Map<String, Object>> cards = planCards.get(planId);
            if (CollectionUtils.isEmpty(cards)) {
                allMetrics.add(0);
                endMetrics.add(0);
            } else {
                allMetrics.add(metric.apply(cards).doubleValue());
                endMetrics.add(metric.apply(cards.stream().filter(CardHelper::isEnd).collect(Collectors.toList())).doubleValue());
            }
        });
        Map<String, Object> result = new HashMap<>();
        result.put("x", config.getPlanIds());
        result.put("all", allMetrics);
        result.put("end", endMetrics);
        result.put("plans", planQueryService.select(config.getPlanIds()));
        return result;
    }

    public Object cumulativeFlowDiagram(Long projectId, CumulativeFlowDiagram config, List<Query> queries) throws IOException {
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        List<String> statues = new ArrayList<>();
        int startStatusIndex = -1;
        int endStatusIndex = -1;
        for (int i = 0; i < schema.getStatuses().size(); i++) {
            CardStatus status = schema.getStatuses().get(i);
            statues.add(status.getKey());
            if (status.getKey().equals(config.getStartStatus())) {
                startStatusIndex = i;
            }
            if (status.getKey().equals(config.getEndStatus())) {
                endStatusIndex = i;
            }
        }
        Assert.isTrue(endStatusIndex >= startStatusIndex && startStatusIndex >= 0, "Invalid start or end status for lead time!");
        DateRange range = config.getDateRange();
        List<DateRange.TimeSpan> timeSpans = range.timeSpans(DateRange.DEFAULT_SPANS_LIMIT, DateInterval.DAY);
        int step = DateRange.step(range.start(), range.end(), DateRange.DEFAULT_SPANS_LIMIT, 1);
        Consumer<SearchSourceBuilder> sourceBuilderConsumer = builder -> buildCardEventDateBuckets(
                builder,
                timeSpans,
                AggregationBuilders
                        .terms("statuses").field(CardEvent.cardProp(CardField.STATUS)).size(ES_TERMS_SIZE)

        );
        Aggregations aggs = cardEventDao.aggs(queries, sourceBuilderConsumer);
        int[][] values = new CFDParser().parse(aggs, statues);
        CFD.CFDResult cfdResult = new CFD(values, timeSpans.size(), statues.size(), startStatusIndex, endStatusIndex, step).run();
        Map<String, Object> result = new HashMap<>();
        // todo result.put("x", timeSpans.stream().map(s -> s.getStart()).collect(Collectors.toList()));
        result.put("x", timeSpans);
        result.put("count", values);
        result.put("cfd", cfdResult);
        return result;
    }

    public Object burnUp(Long projectId, BurnUp config, List<Query> queries) throws IOException {
        DateRange range = config.getDateRange();
        List<DateRange.TimeSpan> timeSpans = range.timeSpans(config.getDateInterval());
        Function<String, AggregationBuilder> metricAggBuild;
        switch (config.getMetricType()) {
            case SumCardField:
                metricAggBuild = name -> AggregationBuilders.sum(name).field(CardEvent.cardProp(config.getMetricField()));
                break;
            case AvgCardField:
                metricAggBuild = name -> AggregationBuilders.avg(name).field(CardEvent.cardProp(config.getMetricField())).missing(0);
                break;
            default:
                metricAggBuild = name -> AggregationBuilders.count(name).field(CardEvent.cardProp(CardField.SEQ_NUM));
        }
        Consumer<SearchSourceBuilder> sourceBuilderConsumer = builder -> buildCardEventDateBuckets(
                builder,
                timeSpans,
                AggregationBuilders
                        .filter("end", QueryBuilders.termQuery(CardEvent.cardProp(CardField.CALC_IS_END), true))
                        .subAggregation(metricAggBuild.apply("metric"))

        );
        Aggregations aggs = cardEventDao.aggs(queries, sourceBuilderConsumer);

        Map<String, Object> result = new HashMap<>();
        result.put("x", timeSpans);
        result.putAll(new BurnUpParser().parse(aggs));
        return result;
    }

    public Object burnDown(Long projectId, BurnDown config, List<Query> queries) throws IOException {
        DateRange range = config.getDateRange();
        List<DateRange.TimeSpan> timeSpans = range.timeSpans(config.getDateInterval());
        Function<String, AggregationBuilder> metricAggBuild;
        switch (config.getMetricType()) {
            case SumCardField:
                metricAggBuild = name -> AggregationBuilders.sum(name).field(CardEvent.cardProp(config.getMetricField()));
                break;
            case AvgCardField:
                metricAggBuild = name -> AggregationBuilders.avg(name).field(CardEvent.cardProp(config.getMetricField())).missing(0);
                break;
            default:
                metricAggBuild = name -> AggregationBuilders.count(name).field(CardEvent.cardProp(CardField.SEQ_NUM));
        }
        Consumer<SearchSourceBuilder> sourceBuilderConsumer = builder -> buildCardEventDateBuckets(
                builder,
                timeSpans,
                AggregationBuilders
                        .filter("notEnd", QueryBuilders.termQuery(CardEvent.cardProp(CardField.CALC_IS_END), false))
                        .subAggregation(metricAggBuild.apply("metric"))

        );
        Aggregations aggs = cardEventDao.aggs(queries, sourceBuilderConsumer);

        Map<String, Object> result = new HashMap<>();
        result.put("x", timeSpans);
        result.putAll(new BurnDownParser().parse(aggs));
        return result;
    }

    public Object userNewBug(String user, Long company, DateRange range) throws Exception {
        List<Query> queries = Arrays.asList(
                Eq.builder().field(CardField.COMPANY_ID).value(String.valueOf(company)).build(),
                Eq.builder().field(CardField.OWNER_USERS).value(user).build(),
                Eq.builder().field(CardField.INNER_TYPE).value("bug").build(),
                Between.builder().field(CardField.CREATE_TIME).start(String.valueOf(range.start().getTime())).end(String.valueOf(range.end().getTime())).build(),
                Eq.builder().field(CardField.DELETED).value(String.valueOf(false)).build());
        return oneDimensionTableData(OneDimensionTable.builder()
                .classifyField(CardField.IMPORTANCE)
                .metricType(MetricType.CountCard)
                .metricField(CardField.SEQ_NUM)
                .build(), queries, "");
    }

    public Map<String, Long> userEndCard(String user, Long company, DateRange range) throws Exception {
        Map<String, Long> result = new HashMap<>();
        List<Query> queries = Arrays.asList(
                Eq.builder().field(CardField.COMPANY_ID).value(String.valueOf(company)).build(),
                Eq.builder().field(CardField.OWNER_USERS).value(user).build(),
                Between.builder().field(CardField.LAST_MODIFY_TIME).start(String.valueOf(range.start().getTime())).end(String.valueOf(range.end().getTime())).build(),
                Eq.builder().field(CardField.DELETED).value(String.valueOf(false)).build());
        List<Map<String, Object>> cards = cardDao.searchAsMap(queries, CardField.PROJECT_ID, CardField.TYPE, CardField.STATUS,
                CardField.CALC_IS_END);
        if (CollectionUtils.isEmpty(cards)) {
            return result;
        }
        List<Long> projectIds = cards.stream().map(card -> FieldUtil.toLong(card.get(CardField.PROJECT_ID))).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(projectIds)) {
            return result;
        }

        return cards.stream()
                .filter(FieldUtil::getCalcIsEnd).collect(Collectors
                        .groupingBy(card -> FieldUtil.toString(card.get(CardField.TYPE)), Collectors.counting()));
    }

    private Object missingValue(ProjectCardSchema schema, String fieldKey) {
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

    private void buildCardEventDateBuckets(
            SearchSourceBuilder builder,
            List<DateRange.TimeSpan> timeSpans,
            List<AggregationBuilder> sugAggs) {
        for (DateRange.TimeSpan timeSpan : timeSpans) {
            builder.aggregation(buildCardEventDateFilter(timeSpan, sugAggs));
        }
    }

    private void buildCardEventDateBuckets(
            SearchSourceBuilder builder,
            List<DateRange.TimeSpan> timeSpans,
            AggregationBuilder... sugAggs) {
        buildCardEventDateBuckets(builder, timeSpans, Arrays.asList(sugAggs));
    }

    private void buildCardEventDateBuckets(
            AggregationBuilder builder,
            List<DateRange.TimeSpan> timeSpans,
            List<AggregationBuilder> sugAggs) {
        for (DateRange.TimeSpan timeSpan : timeSpans) {
            builder.subAggregation(buildCardEventDateFilter(timeSpan, sugAggs));
        }
    }

    private void buildCardEventDateBuckets(
            AggregationBuilder builder,
            List<DateRange.TimeSpan> timeSpans,
            AggregationBuilder... sugAggs) {
        buildCardEventDateBuckets(builder, timeSpans, Arrays.asList(sugAggs));
    }

    private FilterAggregationBuilder buildCardEventDateFilter(
            DateRange.TimeSpan timeSpan,
            List<AggregationBuilder> sugAggs) {
        Date end = timeSpan.getEnd();
        FilterAggregationBuilder filter = AggregationBuilders.filter(timeSpan.getStartAsString(), CardEvent.timeQueryBuilder(end));
        for (AggregationBuilder sugAgg : sugAggs) {
            filter.subAggregation(sugAgg);
        }
        return filter;
    }

    /**
     * 项目卡片数据概览
     *
     * @param projectIds 项目Id
     * @param queries    查询条件
     * @return
     */
    public Map<Long, ProjectCardSummary> projectCardsSummary(@NotNull List<Long> projectIds, List<Query> queries) {
        List<Query> all = new ArrayList<>();
        all.add(In.builder().field(CardField.PROJECT_ID).values(projectIds.stream().map(String::valueOf).collect(Collectors.toList())).build());
        if (CollectionUtils.isNotEmpty(queries)) {
            all.addAll(queries);
        }

        BoolQueryBuilder processStatusQueryBuild = QueryBuilders.boolQuery();
        processStatusQueryBuild.mustNot(QueryBuilders.termQuery(CardField.STATUS, CardStatus.FIRST));
        processStatusQueryBuild.mustNot(QueryBuilders.termQuery(CardField.CALC_IS_END, true));
        Consumer<SearchSourceBuilder> cardBuilderConsumer = builder -> builder
                .aggregation(AggregationBuilders.terms("projectId").field(CardField.PROJECT_ID)
                        .subAggregation(AggregationBuilders.terms("isEnd").field(CardField.CALC_IS_END).missing(false))
                        .subAggregation(AggregationBuilders.sum("actual_workload").field(CardField.ACTUAL_WORKLOAD).missing(0))
                        .subAggregation(AggregationBuilders.count("cardCount").field(CardField.PROJECT_ID))
                        .subAggregation(AggregationBuilders.filter("newStatus", QueryBuilders.boolQuery()
                                .filter(QueryBuilders.termsQuery(CardField.STATUS, CardStatus.FIRST))))
                        .subAggregation(AggregationBuilders.filter("processStatus", QueryBuilders.boolQuery()
                                .filter(processStatusQueryBuild)))
                );

        Aggregations aggs;
        try {
            aggs = cardDao.aggs(all, cardBuilderConsumer);
        } catch (Exception e) {
            log.error("cardSummary exception!", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        return new ProjectCardSummaryParser().parse(aggs);
    }
}
