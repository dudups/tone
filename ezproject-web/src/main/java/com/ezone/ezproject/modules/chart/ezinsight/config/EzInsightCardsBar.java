package com.ezone.ezproject.modules.chart.ezinsight.config;

import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.chart.config.OneDimensionTable;
import com.ezone.ezproject.modules.chart.config.enums.MetricType;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
public class EzInsightCardsBar extends OneDimensionTable implements EzInsightChart {
    public EzInsightCardsBar(){
        //条形图目前只支持统计卡片数。同时父类校验metricType不为空。
        super.setMetricType(MetricType.CountCard);
    }

    private List<Query> queries;
    private boolean excludeNoPlan;
}
