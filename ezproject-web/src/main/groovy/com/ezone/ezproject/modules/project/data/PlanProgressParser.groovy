package com.ezone.ezproject.modules.project.data

import com.ezone.ezproject.modules.plan.bean.PlansAndProgresses
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilters

class PlanProgressParser {
    Map<Long, PlansAndProgresses.Progress> parse(Aggregations aggs) {
        def filters = aggs['plans'] as ParsedFilters
        filters.buckets.collectEntries {
            def bucket = it as ParsedFilters.ParsedBucket
            def end = bucket.aggregations['end'] as ParsedFilter
            [
                    bucket.keyAsString.toLong(),
                    PlansAndProgresses.Progress.builder()
                            .total(bucket.docCount)
                            .end(end.docCount)
                            .build()
            ]
        }
    }
}
