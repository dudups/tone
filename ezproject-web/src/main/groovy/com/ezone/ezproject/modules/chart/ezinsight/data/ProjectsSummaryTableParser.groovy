package com.ezone.ezproject.modules.chart.ezinsight.data

import com.ezone.ezproject.modules.chart.ezinsight.config.EzInsightProjectsSummaryTable
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms
import org.elasticsearch.search.aggregations.metrics.ParsedCardinality
import org.elasticsearch.search.aggregations.metrics.ParsedSum

class ProjectsSummaryTableParser {
    Map<Long, Map<EzInsightProjectsSummaryTable.Metric, Number>> parse(Aggregations aggs) {
        def filter = aggs['filter'] as ParsedFilter
        def projects = filter.aggregations['projects'] as ParsedTerms
        projects.buckets.collectEntries {
            def bucket = it as ParsedTerms.ParsedBucket
            def metricValues = [:]
            metricValues[EzInsightProjectsSummaryTable.Metric.COUNT_CARD] = bucket.docCount
            bucket.aggregations.each {
                def metric = EzInsightProjectsSummaryTable.Metric.valueOf(it.name)
                if (metric) {
                    switch (metric) {
                        case EzInsightProjectsSummaryTable.Metric.SUM_CARD_ACTUAL_WORKLOAD:
                        case EzInsightProjectsSummaryTable.Metric.SUM_CARD_ESTIMATE_WORKLOAD:
                        case EzInsightProjectsSummaryTable.Metric.SUM_CARD_REMAIN_WORKLOAD:
                            metricValues[metric] = (it as ParsedSum).value.round(1)
                            break
                        case EzInsightProjectsSummaryTable.Metric.COUNT_CARD_END:
                            metricValues[metric] = (it as ParsedFilter).docCount
                            break
                        case EzInsightProjectsSummaryTable.Metric.COUNT_CARD_OWNER_USERS:
                            metricValues[metric] = (it as ParsedCardinality).value
                            break
                        default:
                            break
                    }
                }
            }
            [bucket.key, metricValues]
        }
    }
}
