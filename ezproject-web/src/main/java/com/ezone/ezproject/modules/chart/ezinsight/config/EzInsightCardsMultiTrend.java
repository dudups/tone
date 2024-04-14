package com.ezone.ezproject.modules.chart.ezinsight.config;

import com.ezone.ezproject.modules.chart.config.CardsMultiTrend;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class EzInsightCardsMultiTrend extends CardsMultiTrend implements EzInsightChart {
}
