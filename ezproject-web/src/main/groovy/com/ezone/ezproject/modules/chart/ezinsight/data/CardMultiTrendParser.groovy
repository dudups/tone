package com.ezone.ezproject.modules.chart.ezinsight.data

import com.ezone.ezproject.modules.chart.config.CardsMultiTrend
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter

class CardMultiTrendParser {
    List parse(CardsMultiTrend chart, Aggregations aggs) {
        def dateFilters = aggs.sort {it.name}
        def index = 0
        chart.getYConfigs().collect {config ->
            def lastCount = 0
            def values = []
            dateFilters.eachWithIndex {it, i ->
                def dateFilter = it as ParsedFilter
                def count = dateFilter.docCount
                if (config.queries?.size()) {
                    def filter = dateFilter.aggregations[index] as ParsedFilter
                    count = filter.docCount
                }
                if (i > 0) {
                    values.add(config.deltaMetric ? count - lastCount : count)
                }
                lastCount = count
            }
            if (config.queries) {
                index++
            }
            values
        }
    }
}
