package com.ezone.ezproject.modules.company.service;

import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.enums.InnerCardType;
import com.ezone.ezproject.modules.card.bean.query.Between;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.bean.query.In;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.chart.config.range.DateRange;
import com.ezone.ezproject.modules.company.bean.CardStatData;
import com.ezone.ezproject.modules.project.bean.CardScatter;
import com.ezone.ezproject.modules.project.bean.ChartDataRequest;
import com.ezone.ezproject.modules.project.service.SummaryDataHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms;
import org.elasticsearch.search.aggregations.metrics.ParsedSum;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class CompanySummaryService {
    private CardDao cardDao;

    private SummaryDataHelper summaryDataHelper;
    private CompanyProjectSchemaQueryService companyProjectSchemaQueryService;

    public List<CardStatData> companyEndCardCount(List<Long> companyIds, Date start, Date end) throws IOException {
        if (CollectionUtils.isEmpty(companyIds)) {
            return ListUtils.EMPTY_LIST;
        }
        List<Query> queries = new ArrayList<>();
        queries.add(In.builder().field(CardField.COMPANY_ID).values(companyIds.stream().map(String::valueOf).collect(Collectors.toList())).build());
        queries.add(Between.builder().field(CardField.LAST_MODIFY_TIME).start(String.valueOf(start.getTime())).end(String.valueOf(end.getTime())).build());
        queries.add(Eq.builder().field(CardField.CALC_IS_END).value("true").build());
        queries.add(Eq.builder().field(CardField.DELETED).value(String.valueOf(false)).build());
        Consumer<SearchSourceBuilder> aggregation = builder -> builder
                .aggregation(AggregationBuilders
                        .terms(CardField.COMPANY_ID).field(CardField.COMPANY_ID).size(companyIds.size())
                        .subAggregation(AggregationBuilders.sum(CardField.ACTUAL_WORKLOAD).field(CardField.ACTUAL_WORKLOAD))
                );
        Aggregations aggs = cardDao.aggs(queries, aggregation);
        ParsedTerms terms = aggs.get(CardField.COMPANY_ID);
        return terms.getBuckets().stream()
                .map(bucket -> {
                    ParsedSum sum = bucket.getAggregations().get(CardField.ACTUAL_WORKLOAD);
                    return CardStatData.builder()
                            .companyId(bucket.getKeyAsNumber().longValue())
                            .completedCards(bucket.getDocCount())
                            .completedWorkload(sum.getValue())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public Object chartBugTrend(Long companyId, ChartDataRequest request) throws Exception {
        List<Query> queries = request.getQueries();
        // 由于部份事件的卡片快照未记录inner_type（bug），这里改用type（所有的bug类型）
        if (CollectionUtils.isEmpty(queries)) {
            List<String> bugCardTypeKeys = new ArrayList<>();
            CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(companyId);
            companyCardSchema.getTypes().stream()
                    .filter(type -> type.getInnerType().equals(InnerCardType.bug.name()))
                    .forEach(type -> {
                        bugCardTypeKeys.add(type.getKey());
                    });
            if (queries == null) {
                queries = new ArrayList<>();
            }
            queries.add(In.builder().field(CardField.TYPE).values(bugCardTypeKeys).build());
        }
        return summaryDataHelper.chartBugTrend(
                Eq.builder().field(CardField.COMPANY_ID).value(String.valueOf(companyId)).build(), request.getRange(), queries);
    }

    public Object chartCardTrend(Long companyId, DateRange range, List<Query> queries) throws Exception {
        return summaryDataHelper.chartCardTrend(
                Eq.builder().field(CardField.COMPANY_ID).value(String.valueOf(companyId)).build(),
                range,
                queries);
    }

    public List<CardScatter.Data> chartCardScatter(Long companyId, DateRange range, List<Query> queries) throws Exception {
        return summaryDataHelper.chartCardScatter(
                Eq.builder().field(CardField.COMPANY_ID).value(String.valueOf(companyId)).build(),
                range,
                queries);
    }

}
