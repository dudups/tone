package com.ezone.ezproject.modules.project.data

import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms

class SummaryCardTrendParser {
    Map parse(Aggregations aggs) {
        def lastEndCount = 0
        def incrEndCounts = []
        def creates = []
        def types = []
        def filters = aggs.sort {it.name}
        for (int i = 0; i < filters.size(); i++) {
            def dateFilter = filters[i] as ParsedFilter
            def snapFilter = dateFilter.aggregations["snap"] as ParsedFilter
            def notEndFilter = snapFilter.aggregations["notEnd"] as ParsedFilter
            def typeTerms = notEndFilter.aggregations["types"] as ParsedStringTerms
            def createFilter = dateFilter.aggregations["create"] as ParsedFilter
            def endCount = snapFilter.docCount - notEndFilter.docCount
            if (i > 0) {
                incrEndCounts.add(endCount - lastEndCount)
                types.add(typeTerms.buckets.collectEntries { [it.keyAsString, it.docCount] })
                creates.add(createFilter.docCount)
            }
            lastEndCount = endCount
        }
        [
                incrEndSum: incrEndCounts,
                y: types,
                create: creates,
        ]
    }
}
