package com.ezone.ezproject.modules.chart.ezinsight.config;

import com.ezone.ezproject.modules.chart.config.enums.MetricType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EzInsightCardsBarTest {
    public static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory()
            .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    @Test
    void test() throws JsonProcessingException {
        String json = "{\n" +
                "    \"type\": \"cardsBar\",\n" +
                "    \"classifyField\": \"create_user\",\n" +
                "    \"queries\": []\n" +
                "}";
        EzInsightChart ezInsightChart = YAML_MAPPER.readValue(json, EzInsightChart.class);
        assertEquals(MetricType.CountCard, ((EzInsightCardsBar)ezInsightChart).getMetricType());
    }


}