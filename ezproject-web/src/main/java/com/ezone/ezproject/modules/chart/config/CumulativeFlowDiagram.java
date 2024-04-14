package com.ezone.ezproject.modules.chart.config;

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
public class CumulativeFlowDiagram implements Chart.Config {
    @NotNull
    private DateRange dateRange;

    @NotNull
    private String startStatus;
    @NotNull
    private String endStatus;
}
