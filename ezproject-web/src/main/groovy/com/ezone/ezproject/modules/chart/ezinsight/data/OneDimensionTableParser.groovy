package com.ezone.ezproject.modules.chart.ezinsight.data

import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation

class OneDimensionTableParser {
    Map parse(Aggregations aggs) {
        def count = aggs['count'] as NumericMetricsAggregation.SingleValue
        def total = aggs['total'] as NumericMetricsAggregation.SingleValue
        def xTerms = aggs['X'] as ParsedTerms
        [
                count: count.value(),
                total: total.value().round(1),
                xValues: xTerms.buckets.collect {it.key},
                x: xTerms.buckets.collect {[
                        key: it.key,
                        value: (it.aggregations['X'] as NumericMetricsAggregation.SingleValue).value().round(1)
                ]}
        ]
    }
}
