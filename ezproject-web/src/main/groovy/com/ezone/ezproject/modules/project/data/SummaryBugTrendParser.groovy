package com.ezone.ezproject.modules.project.data


import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms

class SummaryBugTrendParser {
    Map parse(Aggregations aggs) {
        def lastEndCount = 0
        def incrEndCounts = []
        def creates = []
        def importances = []
        def priorities = []
        def filters = aggs.sort {it.name}
        for (int i = 0; i < filters.size(); i++) {
            def dateFilter = filters[i] as ParsedFilter
            def snapFilter = dateFilter.aggregations["snap"] as ParsedFilter
            def notEndFilter = snapFilter.aggregations["notEnd"] as ParsedFilter
            def importanceTerms = notEndFilter.aggregations["importances"] as ParsedStringTerms
            def priorityTerms = notEndFilter.aggregations["priorities"] as ParsedStringTerms
            def createFilter = dateFilter.aggregations["create"] as ParsedFilter
            def endCount = snapFilter.docCount - notEndFilter.docCount
            if (i > 0) {
                incrEndCounts.add(endCount - lastEndCount)
                importances.add(importanceTerms.buckets.collectEntries { [it.keyAsString, it.docCount] })
                priorities.add(priorityTerms.buckets.collectEntries { [it.keyAsString, it.docCount] })
                creates.add(createFilter.docCount)
            }
            lastEndCount = endCount
        }
        [
                incrEndSum: incrEndCounts,
                y: importances,
                y2: priorities,
                create: creates,
        ]
    }
}
