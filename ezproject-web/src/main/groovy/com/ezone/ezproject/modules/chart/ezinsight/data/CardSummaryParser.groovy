package com.ezone.ezproject.modules.chart.ezinsight.data

import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms
import org.elasticsearch.search.aggregations.metrics.ParsedCardinality

class CardSummaryParser {
    Map parse(Aggregations aggs) {
        def endTerms = aggs['isEnd'] as ParsedLongTerms
        def blockedTerms = aggs['blocked'] as ParsedLongTerms
        def ownerFilter = aggs['ownerUsers'] as ParsedFilter
        def owners = ownerFilter.aggregations['ownerUsers'] as ParsedCardinality
        def bugFilter = aggs['bug'] as ParsedFilter
        def bugEndTerms = bugFilter.aggregations[0] as ParsedLongTerms
        [
                cardCount: endTerms.buckets.sum {it.docCount} ?: 0,
                ownerCardCount: ownerFilter.docCount,
                ownerUserCount: owners.value(),
                endCount: endTerms.buckets.find {it.key == 1}?.docCount ?: 0,
                notEndCount: endTerms.buckets.find {it.key == 0}?.docCount ?: 0,
                blockedCount: blockedTerms.buckets.find {it.key == 1}?.docCount ?: 0,
                notBlockedCount: blockedTerms.buckets.find {it.key == 0}?.docCount ?: 0,
                bugCount: bugFilter.docCount,
                endBugCount: bugEndTerms.buckets.find {it.key == 1}?.docCount ?: 0,
                notEndBugCount: bugEndTerms.buckets.find {it.key == 0}?.docCount ?: 0,
        ]
    }
}
