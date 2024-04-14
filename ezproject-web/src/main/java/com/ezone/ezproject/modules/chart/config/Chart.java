package com.ezone.ezproject.modules.chart.config;

import com.ezone.ezproject.modules.card.bean.query.Query;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Chart {
    @ApiModelProperty(value = "查询条件", example = "参考这个api的返回值：card/searchQueryExamples")
    private List<Query> queries;

    private boolean excludeNoPlan;

    private Config config;

    @ApiModelProperty(value = "字段ID-仅自定义字段产生字段ID", example = "{\"custom_1_keyword\": 1}")
    private Map<String, Long> fieldIds;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(name = "oneDimensionTable", value = OneDimensionTable.class),
            @JsonSubTypes.Type(name = "twoDimensionTable", value = TwoDimensionTable.class),
            @JsonSubTypes.Type(name = "cardsBar", value = CardsBar.class),
            @JsonSubTypes.Type(name = "cardsPie", value = CardsPie.class),
            @JsonSubTypes.Type(name = "cardsHistogramChart", value = CardsHistogramChart.class),
            @JsonSubTypes.Type(name = "cardsProgressBurnUp", value = CardsProgressBurnUp.class),
            @JsonSubTypes.Type(name = "cardsTrend", value = CardsTrend.class),
            @JsonSubTypes.Type(name = "cardsMultiTrend", value = CardsMultiTrend.class),
            @JsonSubTypes.Type(name = "planMeasure", value = PlanMeasure.class),
            @JsonSubTypes.Type(name = "teamRateChart", value = TeamRateChart.class),
            @JsonSubTypes.Type(name = "cumulativeFlowDiagram", value = CumulativeFlowDiagram.class),
            @JsonSubTypes.Type(name = "burnUp", value = BurnUp.class),
            @JsonSubTypes.Type(name = "burnDown", value = BurnDown.class),
            @JsonSubTypes.Type(name = "cardsSummary", value = CardsSummary.class),
            @JsonSubTypes.Type(name = "userGantt", value = UserGantt.class),
    })
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public interface Config {
    }

    public static final Class[] CONFIG_CLASSES = {
            OneDimensionTable.class,
            TwoDimensionTable.class,
            CardsBar.class,
            CardsPie.class,
            CardsHistogramChart.class,
            CardsProgressBurnUp.class,
            CardsTrend.class,
            CardsMultiTrend.class,
            PlanMeasure.class,
            TeamRateChart.class,
            CumulativeFlowDiagram.class,
            BurnUp.class,
            BurnDown.class,
            CardsSummary.class
    };

    public static String chartType(Config chartConfig) {
        if (null == chartConfig) {
            return null;
        }
        return StringUtils.uncapitalize(chartConfig.getClass().getSimpleName());
    }

//    @AllArgsConstructor
//    public enum ChartType {
//        OneDimensionTable("统计表（一维筛选表）"),
//        TwoDimensionTable("统计表（二维筛选表）"),
//        CardsBar("需求池条形图"),
//        CardsPie("需求池饼图"),
//
//        CardsHistogramChart("需求池柱状图"),
//        CardsProgressBurnUp("堆积图"),
//        CardsTrend("需求池趋势图"),
//
//        PlanMeasure("计划进度度量数据"),
//        TeamRateChart("团队速率图"),
//        CumulativeFlowDiagram("卡片状态累积流图(CFD)"),
//        BurnUp("燃起图"),
//        BurnDown("燃尽图");
//
//        private String description;
//        // private Class<? extends Chart> clazz;
//    }
}
