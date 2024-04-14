package com.ezone.ezproject.modules.chart.ezinsight.data

import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms

class CardOwnerTopParser {
    List parse(Aggregations aggs) {
        def cardTerms = aggs[0] as ParsedStringTerms
        cardTerms.buckets.collect {
            def bucket = it as ParsedStringTerms.ParsedBucket
            [owner: bucket.key, count: bucket.docCount]
        }
    }
}
