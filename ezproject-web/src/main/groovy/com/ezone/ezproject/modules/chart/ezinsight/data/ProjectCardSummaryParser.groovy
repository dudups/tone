package com.ezone.ezproject.modules.chart.ezinsight.data

import com.ezone.ezproject.modules.portfolio.bean.ProjectCardSummary
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms
import org.elasticsearch.search.aggregations.metrics.ParsedSum
import org.elasticsearch.search.aggregations.metrics.ParsedValueCount

import java.math.RoundingMode

class ProjectCardSummaryParser {
    Map parse(Aggregations aggs) {
        def Map<Long, ProjectCardSummary> result = new HashMap()
        def projects = aggs['projectId'] as ParsedLongTerms
        if (projects.buckets) {
            projects.buckets.forEach({ it ->
                ParsedLongTerms.ParsedBucket projectAggs = it as ParsedLongTerms.ParsedBucket

                def cardCount = projectAggs.aggregations["cardCount"] as ParsedValueCount
                def actualWorkload = projectAggs.aggregations["actual_workload"] as ParsedSum
                def isEnd = projectAggs.aggregations["isEnd"] as ParsedLongTerms
                def notEnd = isEnd.getBucketByKey("false")
                def end = isEnd.getBucketByKey("true")
                def newStatus = projectAggs.aggregations["newStatus"] as ParsedFilter
                def processStatus = projectAggs.aggregations["processStatus"] as ParsedFilter
                def ProjectCardSummary summary = new ProjectCardSummary()
                summary.setProjectId(Long.valueOf(projectAggs.getKey()))
                summary.setCardCount(cardCount.getValue())
                summary.setActualWorkload((long) actualWorkload.getValue())
                def endCount = end == null ? 0 : (int) end.getDocCount()
                summary.setEndCardCount(endCount)
                summary.setNotEndCardCount(notEnd == null ? 0 : (int) notEnd.getDocCount())
                summary.setNewCardCount(newStatus.getDocCount())
                summary.setProcessCardCount(processStatus.getDocCount())
                BigDecimal bd = new BigDecimal(cardCount.getValue() == 0 ? 0 : ((float) endCount / (float) cardCount.getValue()) * 100)
                bd = bd.setScale(2, RoundingMode.HALF_UP)
                summary.setCompletion(Double.valueOf(bd.toString()))
                result.put(projectAggs.getKey(), summary)
            })
        }
        return result
    }
}
