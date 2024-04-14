package com.ezone.ezproject.modules.project.data

import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation

class ProgressBurnUpParser {
    List parse(Aggregations aggs) {
        aggs.sort {it.name}.collect {ParsedFilter filter ->
            def terms = filter.aggregations['statuses'] as ParsedTerms
            terms.buckets.collectEntries { bucket ->
                [bucket.keyAsString, (bucket.aggregations['metric'] as NumericMetricsAggregation.SingleValue).value().round(1)]
            }
        }
    }
}
