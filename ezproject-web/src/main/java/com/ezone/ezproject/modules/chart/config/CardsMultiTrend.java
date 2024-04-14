package com.ezone.ezproject.modules.chart.config;

import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.chart.config.enums.DateInterval;
import com.ezone.ezproject.modules.chart.config.range.DateRange;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CardsMultiTrend implements Chart.Config {
    @NotNull
    private DateInterval dateInterval = DateInterval.DAY;

    @NotNull
    private DateRange dateRange;

    @NotNull
    @Size(min = 1, max = 10)
    @JsonDeserialize(contentAs = YConfig.class)
    @JsonProperty("yConfigs")
    private List<YConfig> yConfigs;

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YConfig {
        private String name;

        @ApiModelProperty(value = "查询条件", example = "参考这个api的返回值：card/searchQueryExamples")
        private List<Query> queries;

        private boolean excludeNoPlan;

        private boolean deltaMetric;
    }
}
