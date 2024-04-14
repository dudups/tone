package com.ezone.ezproject.modules.project.service;

import com.ezone.devops.ezcode.sdk.bean.model.ProjectIterStatData;
import com.ezone.devops.ezcode.sdk.bean.model.TimeInterval;
import com.ezone.devops.ezcode.sdk.service.InternalStatService;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.service.TaskExecuteErrorPolicy;
import com.ezone.ezproject.common.service.TasksExecute;
import com.ezone.ezproject.common.service.ThreadPool;
import com.ezone.ezproject.common.template.JsTemplate;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.dao.CardEventDao;
import com.ezone.ezproject.es.dao.ProjectSummaryDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectSummary;
import com.ezone.ezproject.es.entity.enums.InnerCardType;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bean.query.Between;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.bean.query.Gt;
import com.ezone.ezproject.modules.card.bean.query.In;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.chart.config.range.DateRange;
import com.ezone.ezproject.modules.chart.ezinsight.data.CardSummaryParser;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import com.ezone.ezproject.modules.project.bean.CardScatter;
import com.ezone.ezproject.modules.project.bean.ChartDataRequest;
import com.ezone.ezproject.modules.project.bean.ProjectRepoBean;
import com.ezone.ezproject.modules.project.data.PlanChartDataParser;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectSummaryService {
    public static final int ES_TERMS_SIZE = 30;
    public static final List<String> CHARTS = Arrays.asList("milestone", "bugTrend", "activeCards", "endCards",
            "codeData", "buildData", "deployData", "cardsScatter", "cardTrend", "planData");
    public static final List<String> RIGHT_CHARTS = Arrays.asList("notify", "cardCountByType", "delay", "blocked", "wiki", "docSpace", "codeRepo",
            "artifactRepo", "hostGroup", "k8sGroup", "testSpace");
    private ProjectSummaryDao projectSummaryDao;
    private UserService userService;
    private CardDao cardDao;
    private SummaryDataHelper summaryDataHelper;
    private ProjectSchemaQueryService projectSchemaQueryService;
    private PlanQueryService planQueryService;
    private CardEventDao cardEventDao;

    private ProjectRepoService projectRepoService;
    private InternalStatService repoStatService;

    /**
     * 按配置的数据与个数，与新的配置位置进行合并。
     *
     * @param typeConfigs      配置类型（以此参数的内容个数为准）
     * @param orderTypeConfigs 位置调整排序类型（以此参数的顺序为准）
     * @return
     */
    protected static List<String> mergerTypes(List<String> typeConfigs, List<String> orderTypeConfigs) {
        if (typeConfigs == null) {
            return orderTypeConfigs;
        }
        if (orderTypeConfigs == null) {
            return typeConfigs;
        }

        List<String> filterNoInOldTypes = orderTypeConfigs.stream()
                .filter(typeConfigs::contains)
                .collect(Collectors.toList());
        List<String> filterInPositionTypes = typeConfigs.stream()
                .filter(oldTypeConfig -> !orderTypeConfigs.contains(oldTypeConfig))
                .collect(Collectors.toList());
        filterNoInOldTypes.addAll(filterInPositionTypes);
        return filterNoInOldTypes;
    }

    public void reOrderProjectSummary(Long projectId, List<String> charts, List<String> rightCharts) throws IOException {
        ProjectSummary oldProjectSummary = find(projectId);
        if (charts != null && !charts.isEmpty()) {
            List<String> oldCharts = oldProjectSummary.getCharts();
            charts = mergerTypes(oldCharts, charts);
        }
        if (rightCharts != null && !rightCharts.isEmpty()) {
            List<String> oldRightCharts = oldProjectSummary.getRightCharts();
            rightCharts = mergerTypes(oldRightCharts, rightCharts);
        }
        saveOrUpdate(projectId, charts, rightCharts);
    }

    public void updateProjectSummaryConfig(Long projectId, List<String> charts, List<String> rightCharts) throws IOException {
        ProjectSummary oldProjectSummary = find(projectId);
        if (oldProjectSummary != null) {
            if (charts != null) {
                List<String> oldCharts = oldProjectSummary.getCharts();
                charts = mergerTypes(charts, oldCharts);
            }
            if (rightCharts != null) {
                List<String> oldRightCharts = oldProjectSummary.getRightCharts();
                rightCharts = mergerTypes(rightCharts, oldRightCharts);
            }
        }
        saveOrUpdate(projectId, charts, rightCharts);
    }

    private void saveOrUpdate(Long projectId, List<String> charts, List<String> rightCharts) throws IOException {
        String user = userService.currentUserName();
        //chart与rightCharts至少需要保留一个，如果全空则取默认值（全开）。
        ProjectSummary projectSummary = ProjectSummary.builder()
                .lastModifyTime(new Date())
                .lastModifyUser(user)
                .charts(CollectionUtils.isEmpty(charts) ? CHARTS : charts)
                .rightCharts(CollectionUtils.isEmpty(rightCharts) ? RIGHT_CHARTS : rightCharts)
                .build();
        projectSummaryDao.saveOrUpdate(projectId, projectSummary);
    }

    public ProjectSummary find(Long projectId) throws IOException {
        ProjectSummary summary = projectSummaryDao.find(projectId);
        if (summary == null) {
            summary = ProjectSummary.builder().charts(CHARTS).rightCharts(RIGHT_CHARTS).build();
            return summary;
        }

        if (summary.getCharts() == null) {
            summary.setCharts(CHARTS);
        } else {
            List<String> charts = new ArrayList<>();
            summary.getCharts().stream().filter(CHARTS::contains).forEach(charts::add);
            summary.setCharts(charts);
        }

        if (summary.getRightCharts() == null) {
            summary.setRightCharts(RIGHT_CHARTS);
        } else {
            List<String> rightCharts = new ArrayList<>();
            summary.getRightCharts().stream().filter(RIGHT_CHARTS::contains).forEach(rightCharts::add);
            summary.setRightCharts(rightCharts);
        }
        return summary;
    }

    public void delete(Long projectId) throws IOException {
        projectSummaryDao.delete(projectId);
    }

    public void delete(List<Long> projectIds) throws IOException {
        projectSummaryDao.delete(projectIds);
    }

    public Object chartBugTrend(Long projectId, ChartDataRequest request) throws IOException {
        DateRange range = request.getRange();
        List<Query> queries = request.getQueries();
        // 若开启了缺陷类型的卡片，默认将第一个缺陷类型的卡片类型选中。用户可以更改并且支持选择多个。
        if (CollectionUtils.isEmpty(queries)) {
            List<String> bugCardTypeKeys = new ArrayList<>();
            ProjectCardSchema projectCardSchema = projectSchemaQueryService.getProjectCardSchema(projectId);
            projectCardSchema.getTypes().stream()
                    .filter(type -> type.isEnable() && type.getInnerType().equals(InnerCardType.bug))
                    .forEach(type -> bugCardTypeKeys.add(type.getKey()));
            if (queries == null) {
                queries = new ArrayList<>();
            }
            queries.add(In.builder().field(CardField.TYPE).values(bugCardTypeKeys).build());
        }

        return summaryDataHelper.chartBugTrend(
                Eq.builder().field(CardField.PROJECT_ID).value(String.valueOf(projectId)).build(), range, queries);
    }

    public Object chartCardTrend(Long projectId, DateRange range, List<Query> queries) throws IOException {
        //只统计本项目开启的卡片类型
        ProjectCardSchema projectCardSchema = projectSchemaQueryService.getProjectCardSchema(projectId);
        queries.add(In.builder().field(CardField.TYPE).values(projectCardSchema.enabledCardTypeKeys()).build());
        return summaryDataHelper.chartCardTrend(
                Eq.builder().field(CardField.PROJECT_ID).value(String.valueOf(projectId)).build(),
                range,
                queries);
    }

    public Object chartActiveCards(Long projectId, boolean containsNoPlan, List<Query> queries) throws Exception {
        List<Query> finalQueries = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(queries)) {
            finalQueries.addAll(queries);
        }
        finalQueries.add(Eq.builder().field(CardField.PROJECT_ID).value(String.valueOf(projectId)).build());
        finalQueries.add(Eq.builder().field(CardField.PLAN_IS_ACTIVE).value(String.valueOf(true)).build());
        if (!containsNoPlan) {
            finalQueries.add(Gt.builder().field(CardField.PLAN_ID).value("0").build());
        }
        //只统计本项目开启的卡片类型
        ProjectCardSchema projectCardSchema = projectSchemaQueryService.getProjectCardSchema(projectId);
        finalQueries.add(In.builder().field(CardField.TYPE).values(projectCardSchema.enabledCardTypeKeys()).build());
        //只统计未删除的
        finalQueries.add(Eq.builder().field(CardField.DELETED).value(String.valueOf(false)).build());
        Consumer<SearchSourceBuilder> aggregation = builder -> builder
                .aggregation(AggregationBuilders.count("count").field(CardField.SEQ_NUM))
                .aggregation(AggregationBuilders
                        .terms("X").field(CardField.OWNER_USERS).size(ES_TERMS_SIZE)
                        .subAggregation(AggregationBuilders
                                .terms("Y").field(CardField.TYPE).size(ES_TERMS_SIZE)
                        )
                        .subAggregation(AggregationBuilders.sum(CardField.ESTIMATE_WORKLOAD).field(CardField.ESTIMATE_WORKLOAD))
                );
        String agg = cardDao.agg(finalQueries, aggregation);
        String json = JsTemplate.render("/js/ActiveCards.js").render(agg);
        return CardDao.JSON_MAPPER.readValue(json, Map.class);
    }

    public Object chartEndCards(Long projectId, boolean containsNotActive, DateRange range, List<Query> queries) throws Exception {
        List<Query> finalQueries = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(queries)) {
            finalQueries.addAll(queries);
        }
        finalQueries.add(Eq.builder().field(CardField.PROJECT_ID).value(String.valueOf(projectId)).build());
        if (!containsNotActive) {
            finalQueries.add(Eq.builder().field(CardField.PLAN_IS_ACTIVE).value(String.valueOf(true)).build());
        }
        finalQueries.add(Between.builder().field(CardField.LAST_MODIFY_TIME).start(String.valueOf(range.start().getTime())).end(String.valueOf(range.end().getTime())).build());
        finalQueries.add(Eq.builder().field(CardField.CALC_IS_END).value("true").build());
        //只统计本项目开启的卡片类型
        ProjectCardSchema projectCardSchema = projectSchemaQueryService.getProjectCardSchema(projectId);
        finalQueries.add(In.builder().field(CardField.TYPE).values(projectCardSchema.enabledCardTypeKeys()).build());

        finalQueries.add(Eq.builder().field(CardField.DELETED).value(String.valueOf(false)).build());
        Consumer<SearchSourceBuilder> aggregation = builder -> builder
                .aggregation(AggregationBuilders.count("count").field(CardField.SEQ_NUM))
                .aggregation(AggregationBuilders
                        .terms("X").field(CardField.OWNER_USERS).size(ES_TERMS_SIZE)
                        .subAggregation(AggregationBuilders
                                .terms("Y").field(CardField.TYPE).size(ES_TERMS_SIZE)
                        )
                        .subAggregation(AggregationBuilders
                                .sum("SUM").field(CardField.ESTIMATE_WORKLOAD))
                );
        String agg = cardDao.agg(finalQueries, aggregation);
        String json = JsTemplate.render("/js/EndCards.js").render(agg);
        return CardDao.JSON_MAPPER.readValue(json, Map.class);
    }

    public List<CardScatter.Data> chartCardScatter(Long projectId, DateRange range, List<Query> queries) throws IOException {
        //只统计本项目开启的卡片类型
        ProjectCardSchema projectCardSchema = projectSchemaQueryService.getProjectCardSchema(projectId);
        queries.add(In.builder().field(CardField.TYPE).values(projectCardSchema.enabledCardTypeKeys()).build());
        return summaryDataHelper.chartCardScatter(
                Eq.builder().field(CardField.PROJECT_ID).value(String.valueOf(projectId)).build(),
                range,
                queries);
    }

    public Object chartCardCountByType(Long projectId) throws Exception {
        List<Query> finalQueries = new ArrayList<>();

        finalQueries.add(Eq.builder().field(CardField.PROJECT_ID).value(String.valueOf(projectId)).build());

        List<String> enableCardTypeNames = new ArrayList<>();
        ProjectCardSchema projectCardSchema = projectSchemaQueryService.getProjectCardSchema(projectId);
        projectCardSchema.getTypes().stream().filter(CardType::isEnable).forEach(cardType -> enableCardTypeNames.add(cardType.getKey().toLowerCase()));

        finalQueries.add(In.builder().field(CardField.TYPE).values(enableCardTypeNames).build());
        finalQueries.add(Eq.builder().field(CardField.DELETED).value(String.valueOf(false)).build());

        Consumer<SearchSourceBuilder> aggregation = builder -> builder
                .aggregation(AggregationBuilders
                        .terms("X").field(CardField.TYPE).size(ES_TERMS_SIZE)
                        .subAggregation(AggregationBuilders
                                .filter("Y", QueryBuilders.termsQuery(CardField.CALC_IS_END, true))
                        )
                );
        String agg = cardDao.agg(finalQueries, aggregation);
        String json = JsTemplate.render("/js/CountGroupByAndSubFilter.js").render(agg);
        return CardDao.JSON_MAPPER.readValue(json, Map.class);
    }

    private List<ProjectIterStatData> getCodeIterStat(Long companyId, List<Plan> plans, Long projectId) {
        List<TimeInterval> timeIntervals = new ArrayList<>();
        plans.forEach(plan -> timeIntervals.add(new TimeInterval(plan.getStartTime(), plan.getEndTime())));
        List<ProjectRepoBean> projectRepoBeans = projectRepoService.selectBeanByProjectId(projectId);
        Set<Long> repoIds = projectRepoBeans.stream().map(repoBean -> repoBean.getProjectRepo().getRepoId()).collect(Collectors.toSet());
        List<ProjectIterStatData> projectIterStatData = repoStatService.projectIterStat(
                companyId,
                repoIds,
                timeIntervals
        );
        return projectIterStatData;
    }

    public Object planData(Long companyId, Long projectId, DateRange range) {
        Map<String, Object> result = new HashMap<>();
        List<Plan> plans = planQueryService.selectByEndTime(projectId, range.start(), range.end()).stream()
                .sorted((p1, p2) -> p1.getEndTime().before(p2.getEndTime()) ? -1 : 1)
                .collect(Collectors.toList());
        result.put("plans", plans);
        if (CollectionUtils.isEmpty(plans)) {
            return result;
        }

        List<Supplier<?>> suppliers = new ArrayList<>();
        suppliers.add(() -> {
            List<ProjectIterStatData> codeIterStat = getCodeIterStat(companyId, plans, projectId);
            result.put("iterStat", codeIterStat);
            return "";
        });
        suppliers.add(() -> {
            List<String> enableCardTypeKeys = new ArrayList<>();
            ProjectCardSchema projectCardSchema = projectSchemaQueryService.getProjectCardSchema(projectId);
            projectCardSchema.getTypes().stream().filter(CardType::isEnable).forEach(cardType -> enableCardTypeKeys.add(cardType.getKey().toLowerCase()));
            Map<String, Object> planData = planCardData(plans, enableCardTypeKeys);
            result.put("endRate", planData.get("endRate"));
            result.put("countChange", planData.get("countChange"));
            enableCardTypeKeys.forEach(typeKey -> {
                        String key = typeKey + "EndRate";
                        result.put(key, planData.get(key));
                    }
            );
            return "";
        });
        TasksExecute.builder().executorService(ThreadPool.ioExecutorService)
                .errorPolicy(TaskExecuteErrorPolicy.RETURN_ON_SUBMIT_ERROR)
                .tasks(suppliers)
                .timeoutSeconds(5)
                .build()
                .submit();

        return result;
    }

    private Map<String, Object> planCardData(List<Plan> plans, List<String> enableCardTypeKeys) {

        List<DateRange.TimeSpan> planTimeSpans = new ArrayList<>();
        List<String> planIds = new ArrayList<>(plans.size());
        for (Plan plan : plans) {
            planIds.add(plan.getId().toString());
            DateRange.TimeSpan timeSpan = DateRange.TimeSpan.builder().start(plan.getStartTime())
                    .end(DateUtils.addDays(plan.getEndTime(), 1)).name(plan.getId().toString()).build();
            planTimeSpans.add(timeSpan);
        }

        List<Query> queries = new ArrayList<>();
        queries.add(In.builder().field(CardField.PLAN_ID).values(planIds).build());
        queries.add(Eq.builder().field(CardField.DELETED).value(String.valueOf(false)).build());

        AggregationBuilder[] planEndTimeSubAggs = new AggregationBuilder[enableCardTypeKeys.size() + 1];
        for (int i = 0; i < enableCardTypeKeys.size(); i++) {
            String cardKey = enableCardTypeKeys.get(i);
            FilterAggregationBuilder filter = AggregationBuilders
                    .filter(cardKey, QueryBuilders.termQuery(CardEvent.cardProp(CardField.TYPE), cardKey));
            filter.subAggregation(AggregationBuilders.filter(cardKey + "_end", QueryBuilders.termQuery(CardEvent.cardProp(CardField.CALC_IS_END), true)));
            planEndTimeSubAggs[i] = filter;
        }
        planEndTimeSubAggs[enableCardTypeKeys.size()] = AggregationBuilders
                .filter("end", QueryBuilders.termQuery(CardEvent.cardProp(CardField.CALC_IS_END), true));

        AggregationBuilder[] planStartTimeSubAggs = new AggregationBuilder[enableCardTypeKeys.size()];
        for (int i = 0; i < enableCardTypeKeys.size(); i++) {
            String cardKey = enableCardTypeKeys.get(i);
            planStartTimeSubAggs[i] = AggregationBuilders
                    .filter(cardKey, QueryBuilders.termQuery(CardEvent.cardProp(CardField.TYPE), cardKey));
        }

        Consumer<SearchSourceBuilder> sourceBuilderConsumer = builder -> buildCardEventDateBuckets(
                builder,
                planTimeSpans,
                planStartTimeSubAggs,
                planEndTimeSubAggs
        );
        Aggregations aggs;
        try {
            aggs = cardEventDao.aggs(queries, sourceBuilderConsumer);
        } catch (IOException e) {
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        return new PlanChartDataParser().parse(aggs, plans, enableCardTypeKeys);
    }

    private void buildCardEventDateBuckets(
            SearchSourceBuilder builder,
            List<DateRange.TimeSpan> timeSpans,
            AggregationBuilder[] planStartSubAggs,
            AggregationBuilder[] planEndSubAggs) {
        //plan start
        for (DateRange.TimeSpan timeSpan : timeSpans) {
            Date start = timeSpan.getStart();
            BoolQueryBuilder bool = QueryBuilders.boolQuery();
            bool.filter(QueryBuilders.rangeQuery(CardEvent.DATE).lte(start));
            bool.filter(QueryBuilders.termQuery(CardEvent.cardProp(CardField.PLAN_ID), timeSpan.getName()));
            bool.filter(QueryBuilders.boolQuery()
                    .should(QueryBuilders.rangeQuery(CardEvent.NEXT_DATE).gte(start))
                    .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(CardEvent.NEXT_DATE)))
                    .minimumShouldMatch(1));
            FilterAggregationBuilder filter = AggregationBuilders.filter(timeSpan.getName() + "_start", bool);
            for (AggregationBuilder sugAgg : planStartSubAggs) {
                filter.subAggregation(sugAgg);
            }
            builder.aggregation(filter);
        }

        //plan end
        for (DateRange.TimeSpan timeSpan : timeSpans) {
            Date end = timeSpan.getEnd();
            BoolQueryBuilder bool = QueryBuilders.boolQuery();
            bool.filter(QueryBuilders.rangeQuery(CardEvent.DATE).lte(end));
            String name = timeSpan.getName();
            bool.filter(QueryBuilders.termQuery(CardEvent.cardProp(CardField.PLAN_ID), name));
            bool.filter(QueryBuilders.boolQuery()
                    .should(QueryBuilders.rangeQuery(CardEvent.NEXT_DATE).gte(end))
                    .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(CardEvent.NEXT_DATE)))
                    .minimumShouldMatch(1));
            FilterAggregationBuilder filter = AggregationBuilders.filter(timeSpan.getName() + "_end", bool);
            for (AggregationBuilder sugAgg : planEndSubAggs) {
                filter.subAggregation(sugAgg);
            }
            builder.aggregation(filter);
        }
    }

}
