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
public class TwoDimensionTable implements Chart.Config {
    @NotNull
    private String classifyXField;
    @NotNull
    private String classifyYField;
    @NotNull
    private MetricType metricType;
    private String metricField;
}
