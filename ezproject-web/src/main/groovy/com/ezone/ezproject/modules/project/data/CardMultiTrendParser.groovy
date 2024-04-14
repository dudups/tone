package com.ezone.ezproject.modules.project.data

import com.ezone.ezproject.modules.chart.config.CardsMultiTrend
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter

class CardMultiTrendParser {
    List parse(Aggregations aggs, List<CardsMultiTrend.YConfig> yConfigs) {
        yConfigs.indexed().collect { index, yConfig ->
            def filters = (aggs["${index}"] as ParsedFilter).aggregations.sort {it.name}
            def counts = []
            def incrCounts = []
            for (int i = 0; i < filters.size(); i++) {
                def dateFilter = filters[i] as ParsedFilter
                counts.add(dateFilter.docCount)
                incrCounts.add(i == 0 ? 0 : dateFilter.docCount - counts[i - 1])
            }
            yConfig.isDeltaMetric() ? incrCounts : counts
        }
    }
}
