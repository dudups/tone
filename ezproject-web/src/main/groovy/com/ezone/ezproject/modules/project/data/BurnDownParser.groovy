package com.ezone.ezproject.modules.project.data

import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation

class BurnDownParser {
    Map parse(Aggregations aggs) {
        def counts = []
        def notEndCounts = []
        def filters = aggs.sort {it.name}
        for (int i = 0; i < filters.size(); i++) {
            def dateFilter = filters[i] as ParsedFilter
            def notEndFilter = dateFilter.aggregations['notEnd'] as ParsedFilter
            counts.add(dateFilter.docCount)
            notEndCounts.add((notEndFilter.aggregations['metric'] as NumericMetricsAggregation.SingleValue).value().round(1))
        }
        [
                all: counts,
                not_end: notEndCounts,
        ]
    }
}
