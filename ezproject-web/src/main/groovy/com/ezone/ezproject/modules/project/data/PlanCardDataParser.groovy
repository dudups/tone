package com.ezone.ezproject.modules.project.data

import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms

class PlanCardDataParser {
    Map parse(Aggregations aggs) {
        def endRate = [:]
        def storyEndRate = [:]
        def taskEndRate = [:]
        def agg = aggs[0] as ParsedLongTerms
        agg.buckets.forEach() {
            def bucket = it as ParsedLongTerms.ParsedBucket
            def filter = bucket.aggregations["filter"] as ParsedFilter
            endRate.put(bucket.key, ["total": bucket.docCount, "end": filter.docCount])

            def typeBuckets = bucket.aggregations["type"] as ParsedStringTerms

            def storyBucket = typeBuckets.getBucketByKey("story") as ParsedTerms.ParsedBucket
            if (storyBucket) {
                def storyFilter = storyBucket.aggregations["filter"] as ParsedFilter
                storyEndRate.put(bucket.key, ["total": storyBucket.docCount, "end": storyFilter ? storyFilter.docCount : 0])
            }

            def taskBucket = typeBuckets.getBucketByKey("task") as ParsedTerms.ParsedBucket
            if (taskBucket) {
                def taskFilter = taskBucket.aggregations["filter"] as ParsedFilter
                taskEndRate.put(bucket.key, ["total": taskBucket.docCount, "end": taskFilter ? taskFilter.docCount : 0])
            }
        }
        ["endRate": endRate, "storyEndRate": storyEndRate, "taskEndRate": taskEndRate]
    }
}
