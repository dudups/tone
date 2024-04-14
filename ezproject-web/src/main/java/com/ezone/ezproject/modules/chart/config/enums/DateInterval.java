package com.ezone.ezproject.modules.chart.config.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;

@AllArgsConstructor
@Getter
public enum DateInterval {
    DAY(DateHistogramInterval.DAY),
    WEEK(DateHistogramInterval.WEEK),
    MONTH(DateHistogramInterval.MONTH),
    QUARTER(DateHistogramInterval.QUARTER),
    YEAR(DateHistogramInterval.YEAR);
    private DateHistogramInterval esInterval;

}
