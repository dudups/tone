package com.ezone.ezproject.modules.chart.ezinsight.data


import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms

class BugTrendParser {
    Map parse(Aggregations aggs) {
        def data = [:]
        def lastCount = 0
        def lastEndCount = 0
        def filters = aggs.sort {it.name}
        for (int i = 0; i < filters.size(); i++) {
            def dateFilter = filters[i] as ParsedFilter
            def endFilter = dateFilter.aggregations[0] as ParsedFilter
            def countIncr = dateFilter.docCount - lastCount
            def endCountIncr = endFilter.docCount - lastEndCount
            lastCount = dateFilter.docCount
            lastEndCount = endFilter.docCount
            if (i == 0) {
                continue
            }
            def notEndFilter = dateFilter.aggregations[1] as ParsedFilter
            def bugImportanceTerms = notEndFilter.aggregations[0] as ParsedStringTerms
            def notEndBugImportanceCount = bugImportanceTerms.buckets.collectEntries {
                def bucket = it as ParsedStringTerms.ParsedBucket
                [bucket.keyAsString, bucket.docCount]
            }
            data[dateFilter.name] = [
                    countIncr: countIncr,
                    // endCountIncr: endCountIncr,
                    endCount: endFilter.docCount,
                    notEndBugImportanceCount: notEndBugImportanceCount,
            ]
        }
        data
    }
}
