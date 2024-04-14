package com.ezone.ezproject.modules.chart.config;

import com.ezone.ezproject.modules.chart.config.enums.MetricType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OneDimensionTable implements Chart.Config {
    @NotNull
    private String classifyField;
    @NotNull
    private MetricType metricType;
    private String metricField;
}
