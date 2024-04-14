package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.dao.CardEventDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.modules.card.bean.query.Between;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.card.event.model.EventType;
import com.ezone.ezproject.modules.card.event.service.CardEventQueryService;
import com.ezone.ezproject.modules.chart.config.enums.DateInterval;
import com.ezone.ezproject.modules.chart.config.range.DateRange;
import com.ezone.ezproject.modules.project.bean.CardScatter;
import com.ezone.ezproject.modules.project.data.SummaryBugTrendParser;
import com.ezone.ezproject.modules.project.data.SummaryCardTrendParser;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class SummaryDataHelper {
    private CardEventQueryService cardEventQueryService;
    private CardDao cardDao;
    private CardEventDao cardEventDao;

    public Object chartBugTrend(Query companyOrProject, DateRange range, List<Query> queries) throws IOException {
        List<Query> finalQueries = new ArrayList<>(Arrays.asList(
                companyOrProject,
                Eq.builder().field(CardField.DELETED).value(String.valueOf(false)).build()
        ));
        if (CollectionUtils.isNotEmpty(queries)) {
            finalQueries.addAll(queries);
        }

        List<DateRange.TimeSpan> timeSpans = range.timeSpans(DateInterval.DAY, -1, 0);

        Consumer<SearchSourceBuilder> sourceBuilderConsumer = builder -> timeSpans.forEach(timeSpan -> builder
                .aggregation(AggregationBuilders
                        .filter(timeSpan.getStartAsString(), QueryBuilders.boolQuery())
                        .subAggregation(buildCardEventDateFilter("snap", timeSpan, AggregationBuilders
                                .filter("notEnd", QueryBuilders.termQuery(CardEvent.cardProp(CardField.CALC_IS_END), false))
                                .subAggregation(AggregationBuilders.terms("importances").field(CardEvent.cardProp(CardField.IMPORTANCE)).missing(StringUtils.EMPTY))
                                .subAggregation(AggregationBuilders.terms("priorities").field(CardEvent.cardProp(CardField.PRIORITY)).missing(StringUtils.EMPTY))))
                        .subAggregation(AggregationBuilders
                                .filter("create", QueryBuilders.boolQuery()
                                        .filter(QueryBuilders.termQuery(CardEvent.EVENT_TYPE, EventType.CREATE.name()))
                                        .filter(QueryBuilders.rangeQuery(CardEvent.DATE).from(timeSpan.getStart().getTime()).to(timeSpan.getEnd().getTime()))))
                ));
//        Consumer<SearchSourceBuilder> sourceBuilderConsumer = builder -> buildCardEventDateBuckets(
//                builder,
//                timeSpans,
//                timeSpan -> AggregationBuilders
//                        .filter("notEnd", QueryBuilders.termQuery(CardEvent.cardProp(CardField.CALC_IS_END), false))
//                        .subAggregation(AggregationBuilders.terms("importances").field(CardEvent.cardProp(CardField.IMPORTANCE)).missing(StringUtils.EMPTY))
//                        .subAggregation(AggregationBuilders.terms("priorities").field(CardEvent.cardProp(CardField.PRIORITY)).missing(StringUtils.EMPTY)),
//                timeSpan -> AggregationBuilders
//                        .filter("create", QueryBuilders.boolQuery()
//                                .filter(QueryBuilders.rangeQuery(CardEvent.cardProp(CardField.CREATE_TIME)).from(timeSpan.getStart().getTime()).to(timeSpan.getEnd().getTime())))
//        );
        Aggregations aggs = cardEventDao.aggs(finalQueries, sourceBuilderConsumer);

        Map<String, Object> result = new HashMap<>();
        result.put("x", timeSpans.subList(1, timeSpans.size()));
        result.putAll(new SummaryBugTrendParser().parse(aggs));
        return result;

    }

    public Object chartCardTrend(Query companyOrProject, DateRange range, List<Query> queries) throws IOException {
        List<Query> finalQueries = new ArrayList<>(Arrays.asList(
                companyOrProject,
                Eq.builder().field(CardField.DELETED).value(String.valueOf(false)).build()
        ));
        if (CollectionUtils.isNotEmpty(queries)) {
            finalQueries.addAll(queries);
        }
        List<DateRange.TimeSpan> timeSpans = range.timeSpans(DateInterval.DAY, -1, 0);
        Consumer<SearchSourceBuilder> sourceBuilderConsumer = builder -> timeSpans.forEach(timeSpan -> builder
                .aggregation(AggregationBuilders
                        .filter(timeSpan.getStartAsString(), QueryBuilders.boolQuery())
                        .subAggregation(buildCardEventDateFilter("snap", timeSpan, AggregationBuilders
                                .filter("notEnd", QueryBuilders.termQuery(CardEvent.cardProp(CardField.CALC_IS_END), false))
                                .subAggregation(AggregationBuilders.terms("types").field(CardEvent.cardProp(CardField.TYPE)).missing(StringUtils.EMPTY))))
                        .subAggregation(AggregationBuilders
                                .filter("create", QueryBuilders.boolQuery()
                                        .filter(QueryBuilders.termQuery(CardEvent.EVENT_TYPE, EventType.CREATE.name()))
                                        .filter(QueryBuilders.rangeQuery(CardEvent.DATE).from(timeSpan.getStart().getTime()).to(timeSpan.getEnd().getTime()))))
                ));
//        Consumer<SearchSourceBuilder> sourceBuilderConsumer = builder -> buildCardEventDateBuckets(
//                builder,
//                timeSpans,
//                timeSpan -> AggregationBuilders
//                        .filter("notEnd", QueryBuilders.termQuery(CardEvent.cardProp(CardField.CALC_IS_END), false))
//                        .subAggregation(AggregationBuilders.terms("types").field(CardEvent.cardProp(CardField.TYPE)).missing(StringUtils.EMPTY)),
//                timeSpan -> AggregationBuilders
//                        .filter("create", QueryBuilders.boolQuery()
//                                .filter(QueryBuilders.termQuery(CardEvent.EVENT_TYPE, EventType.CREATE.name()))
//                                .filter(QueryBuilders.rangeQuery(CardEvent.DATE).from(timeSpan.getStart().getTime()).to(timeSpan.getEnd().getTime())))
//        );
        Aggregations aggs = cardEventDao.aggs(finalQueries, sourceBuilderConsumer);

        Map<String, Object> result = new HashMap<>();
        result.put("x", timeSpans.subList(1, timeSpans.size()));
        result.putAll(new SummaryCardTrendParser().parse(aggs));
        return result;
    }

    public List<CardScatter.Data> chartCardScatter(Query companyOrProject, DateRange range, List<Query> queries) throws IOException {
        List<Query> finalQueries = new ArrayList<>(Arrays.asList(
                companyOrProject,
                Between.builder().field(CardField.LAST_MODIFY_TIME).start(String.valueOf(range.start().getTime())).end(String.valueOf(range.end().getTime())).build(),
                Eq.builder().field(CardField.DELETED).value(String.valueOf(false)).build()
        ));
        if (CollectionUtils.isNotEmpty(queries)) {
            finalQueries.addAll(queries);
        }
        finalQueries.add(Eq.builder().field(CardField.CALC_IS_END).value("true").build());
        List<Long> cardIds = cardDao.searchIds(finalQueries);

        List<String> cardProps = Arrays.asList(CardField.CALC_IS_END, CardField.STATUS, CardField.TYPE, CardField.SEQ_NUM, CardField.PROJECT_ID);

        List<CardEvent> cardEvents = cardEventQueryService.searchForChart(cardIds, cardProps);
        return cardEvents.stream()
                .collect(Collectors.groupingBy(CardEvent::getCardId)).values().stream()
                .map(events -> new CardScatter(range, events).data())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // new
    private void buildCardEventDateBuckets(
            SearchSourceBuilder builder,
            List<DateRange.TimeSpan> timeSpans,
            Function<DateRange.TimeSpan, AggregationBuilder> agg) {
        for (DateRange.TimeSpan timeSpan : timeSpans) {
            builder.aggregation(agg.apply(timeSpan));
        }
    }

    private void buildCardEventDateBuckets(
            SearchSourceBuilder builder,
            List<DateRange.TimeSpan> timeSpans,
            Function<DateRange.TimeSpan, AggregationBuilder>... sugAggs) {
        for (DateRange.TimeSpan timeSpan : timeSpans) {
            Date end = timeSpan.getEnd();
            BoolQueryBuilder bool = QueryBuilders.boolQuery();
            bool.filter(QueryBuilders.termsQuery(CardEvent.EVENT_TYPE, EventType.EVENT_STR_FOR_STAT_CHART));
            bool.filter(QueryBuilders.rangeQuery(CardEvent.DATE).lte(end));
            bool.filter(QueryBuilders.boolQuery()
                    .should(QueryBuilders.rangeQuery(CardEvent.NEXT_DATE).gte(end))
                    .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(CardEvent.NEXT_DATE)))
                    .minimumShouldMatch(1));
            FilterAggregationBuilder filter = AggregationBuilders.filter(timeSpan.getStartAsString(), bool);
            for (Function<DateRange.TimeSpan, AggregationBuilder> sugAgg : sugAggs) {
                filter.subAggregation(sugAgg.apply(timeSpan));
            }
            builder.aggregation(filter);
        }
    }

    // todo

    private FilterAggregationBuilder buildCardEventDateFilter(
            DateRange.TimeSpan timeSpan,
            List<AggregationBuilder> sugAggs) {
        Date end = timeSpan.getEnd();
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        bool.filter(QueryBuilders.termsQuery(CardEvent.EVENT_TYPE, EventType.EVENT_STR_FOR_STAT_CHART));
        bool.filter(QueryBuilders.rangeQuery(CardEvent.DATE).lte(end));
        bool.filter(QueryBuilders.boolQuery()
                .should(QueryBuilders.rangeQuery(CardEvent.NEXT_DATE).gte(end))
                .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(CardEvent.NEXT_DATE)))
                .minimumShouldMatch(1));
        FilterAggregationBuilder filter = AggregationBuilders.filter(timeSpan.getStartAsString(), bool);
        for (AggregationBuilder sugAgg : sugAggs) {
            filter.subAggregation(sugAgg);
        }
        return filter;
    }

    private FilterAggregationBuilder buildCardEventDateFilter(
            String name,
            DateRange.TimeSpan timeSpan,
            List<AggregationBuilder> sugAggs) {
        Date end = timeSpan.getEnd();
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        bool.filter(QueryBuilders.termsQuery(CardEvent.EVENT_TYPE, EventType.EVENT_STR_FOR_STAT_CHART));
        bool.filter(QueryBuilders.rangeQuery(CardEvent.DATE).lte(end));
        bool.filter(QueryBuilders.boolQuery()
                .should(QueryBuilders.rangeQuery(CardEvent.NEXT_DATE).gte(end))
                .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(CardEvent.NEXT_DATE)))
                .minimumShouldMatch(1));
        FilterAggregationBuilder filter = AggregationBuilders.filter(name, bool);
        for (AggregationBuilder sugAgg : sugAggs) {
            filter.subAggregation(sugAgg);
        }
        return filter;
    }

    private FilterAggregationBuilder buildCardEventDateFilter(
            String name,
            DateRange.TimeSpan timeSpan,
            AggregationBuilder sugAgg) {
        return buildCardEventDateFilter(name, timeSpan, Arrays.asList(sugAgg));
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

}
