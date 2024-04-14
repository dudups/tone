package com.ezone.ezproject.modules.chart.ezinsight.config;

import com.ezone.ezproject.modules.chart.config.enums.DateInterval;
import com.ezone.ezproject.modules.chart.config.range.DateRange;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardTrend implements EzInsightChart {
    @NotNull
    private DateInterval dateInterval = DateInterval.DAY;

    @NotNull
    private DateRange dateRange;

    private List<String> cardTypes;

    private boolean excludeNoPlan;
}
