package com.ezone.ezproject.modules.chart.ezinsight.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CardSummary implements EzInsightChart {
    private List<String> cardTypes;
    private boolean excludeNoPlan;
}
