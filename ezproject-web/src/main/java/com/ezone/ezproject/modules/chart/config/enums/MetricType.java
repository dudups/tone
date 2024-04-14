package com.ezone.ezproject.modules.chart.config.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum MetricType {
    CountCard("卡片个数"),
    SumCardField("卡片字段值的求和"),
    AvgCardField("卡片字段值的平均值");

    private String description;
}
