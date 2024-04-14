package com.ezone.ezproject.modules.chart.ezinsight.data

import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter
import org.elasticsearch.search.aggregations.metrics.ParsedSum

class CardEndTrendParser {
    Map parse(Aggregations aggs) {
        aggs.collectEntries {
            def dateFilter = it as ParsedFilter
            def endFilter = dateFilter.aggregations[0] as ParsedFilter
            [dateFilter.name, [count: dateFilter.docCount, endCount: endFilter.docCount]]
        }
    }
}
