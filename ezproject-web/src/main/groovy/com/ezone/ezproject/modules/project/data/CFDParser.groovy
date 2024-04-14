package com.ezone.ezproject.modules.project.data

import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms

class CFDParser {
    int[][] parse(Aggregations aggs, List<String> statues) {
        int[][] values = new int[aggs.size()][statues.size()]
        aggs.sort {it.name}.eachWithIndex { ParsedFilter filter, spanIndex ->
            def terms = filter.aggregations['statuses'] as ParsedTerms
            statues.eachWithIndex { status, statusIndex ->
                values[spanIndex][statusIndex] = terms.getBucketByKey(status)?.docCount ?: 0
            }
        }
        values
    }
}
