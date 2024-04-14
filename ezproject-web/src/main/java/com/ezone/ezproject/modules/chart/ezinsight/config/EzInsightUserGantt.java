package com.ezone.ezproject.modules.chart.ezinsight.config;

import com.ezone.ezproject.modules.chart.config.UserGantt;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class EzInsightUserGantt extends UserGantt implements EzInsightChart {
}
