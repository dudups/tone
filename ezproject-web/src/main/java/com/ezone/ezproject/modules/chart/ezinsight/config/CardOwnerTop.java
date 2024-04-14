package com.ezone.ezproject.modules.chart.ezinsight.config;

import com.ezone.ezproject.modules.chart.config.range.DateRange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CardOwnerTop implements EzInsightChart {
    private DateRange dateRange;
    @Builder.Default
    private FilterType filterType = FilterType.CREATE;
    private List<String> cardTypes;
    private boolean excludeNoPlan;

    public enum FilterType {
        CREATE, END
    }
}
