package com.ezone.ezproject.modules.chart.ezinsight.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "cardEndTrend", value = CardEndTrend.class),
        @JsonSubTypes.Type(name = "cardTrend", value = CardTrend.class),
        @JsonSubTypes.Type(name = "bugTrend", value = BugTrend.class),
        @JsonSubTypes.Type(name = "cardSummary", value = CardSummary.class),
        @JsonSubTypes.Type(name = "cardOwnerTop", value = CardOwnerTop.class),
        @JsonSubTypes.Type(name = "bugOwnerTop", value = BugOwnerTop.class),
        @JsonSubTypes.Type(name = "cardDelayList", value = CardDelayList.class),
        @JsonSubTypes.Type(name = "oneDimensionTable", value = EzInsightOneDimensionTable.class),
        @JsonSubTypes.Type(name = "twoDimensionTable", value = EzInsightTwoDimensionTable.class),
        @JsonSubTypes.Type(name = "cardsBar", value = EzInsightCardsBar.class),
        @JsonSubTypes.Type(name = "cardsPie", value = EzInsightCardsPie.class),
        @JsonSubTypes.Type(name = "cardsHistogram", value = EzInsightCardsHistogram.class),
        @JsonSubTypes.Type(name = "cardsMultiTrend", value = EzInsightCardsMultiTrend.class),
        @JsonSubTypes.Type(name = "userGantt", value = EzInsightUserGantt.class),
        @JsonSubTypes.Type(name = "projectsSummaryTable", value = EzInsightProjectsSummaryTable.class),
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface EzInsightChart {
}
