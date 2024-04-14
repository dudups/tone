package com.ezone.ezproject.modules.chart.config;

import com.ezone.ezproject.modules.chart.config.enums.DateInterval;
import com.ezone.ezproject.modules.chart.config.enums.HistogramType;
import com.ezone.ezproject.modules.chart.config.range.DateRange;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardsHistogramChart implements Chart.Config {
    @NotNull
    private DateInterval dateInterval = DateInterval.DAY;

    @NotNull
    private DateRange dateRange;

    @NotNull
    private String classifyYField;

    @NotNull
    private HistogramType displayType = HistogramType.STACK;
}
