package com.ezone.ezproject.modules.chart.ezinsight.config;

import com.ezone.ezproject.modules.chart.config.range.DateRange;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.annotation.Nullable;
import javax.validation.constraints.Max;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardDelayList implements EzInsightChart {
    private List<String> cardTypes;

    @JsonProperty("isEnd")
    private boolean isEnd;

    @Builder.Default
    private int pageNumber = 1;

    @Builder.Default
    @Max(500)
    private int pageSize = 10;

    @Nullable
    private DateRange dateRange;

    private boolean excludeNoPlan;
}
