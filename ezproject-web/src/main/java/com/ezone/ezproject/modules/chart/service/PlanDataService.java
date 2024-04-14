package com.ezone.ezproject.modules.chart.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.plan.bean.PlansAndProgresses;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import com.ezone.ezproject.modules.project.data.PlanProgressParser;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregator;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Slf4j
@AllArgsConstructor
public class PlanDataService {
    private CardDao cardDao;
    private PlanQueryService planQueryService;

    public static final int ES_TERMS_SIZE = 30;

    public Map<Long, PlansAndProgresses.Progress> planProgress(List<Long> planIds) throws IOException {
        if (CollectionUtils.isEmpty(planIds)) {
            return MapUtils.EMPTY_MAP;
        }
        List<Query> queries = Arrays.asList(Eq.builder().field(CardField.DELETED).value(String.valueOf(false)).build());
        Consumer<SearchSourceBuilder> aggregation = builder -> builder
                .aggregation(AggregationBuilders
                        .filters("plans", planIds.stream()
                                .map(planId -> new FiltersAggregator.KeyedFilter(String.valueOf(planId), QueryBuilders.termQuery(CardField.PLAN_ID, planId)))
                                .toArray(FiltersAggregator.KeyedFilter[]::new)
                        )
                        .subAggregation(AggregationBuilders
                                .filter("end", QueryBuilders.termQuery(CardField.CALC_IS_END, true))
                        )
                );
        Aggregations aggs = cardDao.aggs(queries, aggregation);
        return new PlanProgressParser().parse(aggs);
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
}
