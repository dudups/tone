package com.ezone.ezproject.modules.chart.ezinsight.config;

import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.chart.config.OneDimensionTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EzInsightOneDimensionTable extends OneDimensionTable implements EzInsightChart {
    private List<Query> queries;
    private boolean excludeNoPlan;
}
