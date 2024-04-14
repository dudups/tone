package com.ezone.ezproject.modules.chart.ezinsight.config;

import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.SetUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EzInsightProjectsSummaryTable implements EzInsightChart {
    private List<String> cardTypes;

    private Date endDay;
    private List<Column> columns;

    public enum Column {
        SUM_CARD_ACTUAL_WORKLOAD(
                metricsValues -> metricsValues.apply(Metric.SUM_CARD_ACTUAL_WORKLOAD),
                Metric.SUM_CARD_ACTUAL_WORKLOAD
        ),
        SUM_CARD_ESTIMATE_WORKLOAD(
                metricsValues -> metricsValues.apply(Metric.SUM_CARD_ESTIMATE_WORKLOAD),
                Metric.SUM_CARD_ESTIMATE_WORKLOAD
        ),
        SUM_CARD_REMAIN_WORKLOAD(
                metricsValues -> metricsValues.apply(Metric.SUM_CARD_REMAIN_WORKLOAD),
                Metric.SUM_CARD_REMAIN_WORKLOAD
        ),
        COUNT_CARD_END(
                metricsValues -> metricsValues.apply(Metric.COUNT_CARD_END),
                Metric.COUNT_CARD_END
        ),
        COUNT_CARD_NOT_END(
                metricsValues -> {
                    Number count = metricsValues.apply(Metric.COUNT_CARD);
                    Number end = metricsValues.apply(Metric.COUNT_CARD_END);
                    if (count == null || end == null) {
                        return null;
                    }
                    return count.longValue() - end.longValue();
                },
                Metric.COUNT_CARD, Metric.COUNT_CARD_END
        ),
        CARD_END_PROGRESS(
                metricsValues -> {
                    Number count = metricsValues.apply(Metric.COUNT_CARD);
                    Number end = metricsValues.apply(Metric.COUNT_CARD_END);
                    if (count == null || end == null) {
                        return null;
                    }
                    return DefaultGroovyMethods.round(end.doubleValue() / count.doubleValue(), 3);
                },
                Metric.COUNT_CARD, Metric.COUNT_CARD_END
        ),
        COUNT_CARD(
                metricsValues -> metricsValues.apply(Metric.COUNT_CARD),
                Metric.COUNT_CARD
        ),
//        COUNT_MEMBER(
//                metricsValues -> metricsValues.apply(Metric.COUNT_MEMBER),
//                Metric.COUNT_MEMBER
//        ),
        COUNT_OWNER_USERS(
                metricsValues -> metricsValues.apply(Metric.COUNT_CARD_OWNER_USERS),
                Metric.COUNT_CARD_OWNER_USERS
        );

        // metricValues -> columnValue
        public final Function<Function<Metric, Number>, Number> calc;
        public final List<Metric> metrics;

        Column(Function<Function<Metric, Number>, Number> calc, Metric... metrics) {
            this.calc = calc;
            this.metrics = Arrays.asList(metrics);
        }

        public Number calc(Function<Metric, Number> metricsValues) {
            return this.calc.apply(metricsValues);
        }
    }

    @AllArgsConstructor
    public enum Metric {
        SUM_CARD_ACTUAL_WORKLOAD(MetricSource.CARD),
        SUM_CARD_ESTIMATE_WORKLOAD(MetricSource.CARD),
        SUM_CARD_REMAIN_WORKLOAD(MetricSource.CARD),
        COUNT_CARD_END(MetricSource.CARD),
        COUNT_CARD(MetricSource.CARD),
        // COUNT_MEMBER(MetricSource.PROJECT_MEMBER),
        COUNT_CARD_OWNER_USERS(MetricSource.CARD);

        public MetricSource metricSource;
    }

    public List<AggregationBuilder> cardMetricsAggs() {
        Set<Metric> metrics = metrics(MetricSource.CARD);
        if (CollectionUtils.isEmpty(metrics)) {
            return ListUtils.EMPTY_LIST;
        }
        List<AggregationBuilder> aggs = new ArrayList<>();
        metrics.forEach(metric -> {
            switch (metric) {
                case SUM_CARD_ACTUAL_WORKLOAD:
                    aggs.add(AggregationBuilders.sum(metric.name()).field(CardEvent.cardProp(CardField.ACTUAL_WORKLOAD)));
                    break;
                case SUM_CARD_ESTIMATE_WORKLOAD:
                    aggs.add(AggregationBuilders.sum(metric.name()).field(CardEvent.cardProp(CardField.ESTIMATE_WORKLOAD)));
                    break;
                case SUM_CARD_REMAIN_WORKLOAD:
                    aggs.add(AggregationBuilders.sum(metric.name()).field(CardEvent.cardProp(CardField.REMAIN_WORKLOAD)));
                    break;
                case COUNT_CARD_END:
                    aggs.add(AggregationBuilders.filter(metric.name(), QueryBuilders.termQuery(CardEvent.cardProp(CardField.CALC_IS_END), true)));
                    break;
                case COUNT_CARD_OWNER_USERS:
                    aggs.add(AggregationBuilders.cardinality(metric.name()).field(CardEvent.cardProp(CardField.OWNER_USERS)));
                    break;
                default:
                    break;
            }
        });
        return aggs;
    }

    public enum MetricSource {
        CARD, PROJECT_MEMBER
    }

    public Set<Metric> metrics(MetricSource source) {
        if (CollectionUtils.isEmpty(columns)) {
            return SetUtils.EMPTY_SET;
        }
        return columns.stream()
                .map(column -> column.metrics)
                .flatMap(List::stream)
                .filter(column -> column.metricSource == source)
                .collect(Collectors.toSet());
    }
}
