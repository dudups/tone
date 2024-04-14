package com.ezone.ezproject.modules.project.data

import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation

class BurnUpParser {
    Map parse(Aggregations aggs) {
        def counts = []
        def endCounts = []
        def filters = aggs.sort {it.name}
        for (int i = 0; i < filters.size(); i++) {
            def dateFilter = filters[i] as ParsedFilter
            def endFilter = dateFilter.aggregations['end'] as ParsedFilter
            counts.add(dateFilter.docCount)
            endCounts.add((endFilter.aggregations['metric'] as NumericMetricsAggregation.SingleValue).value().round(1))
        }
        [
                all: counts,
                end: endCounts,
        ]
    }
}
