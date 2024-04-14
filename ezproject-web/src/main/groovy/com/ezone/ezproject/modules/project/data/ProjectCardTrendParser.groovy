package com.ezone.ezproject.modules.project.data

import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter

class ProjectCardTrendParser {
    Map parse(Aggregations aggs) {
        def endCounts = []
        def incrCounts = []
        def filters = aggs.sort {it.name}
        for (int i = 0; i < filters.size(); i++) {
            def dateFilter = filters[i] as ParsedFilter
            def endFilter = dateFilter.aggregations["end"] as ParsedFilter
            endCounts.add(endFilter.docCount)
            incrCounts.add(i == 0 ? 0 : endFilter.docCount - endCounts[i - 1])
        }
        [
                end: endCounts,
                incr: incrCounts,
        ]
    }
}
