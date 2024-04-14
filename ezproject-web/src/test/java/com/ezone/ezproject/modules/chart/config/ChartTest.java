package com.ezone.ezproject.modules.chart.config;

import com.ezone.ezproject.common.serialize.TypedSerializerBuilder;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.bean.query.Query;
import org.junit.Assert;
import org.junit.Test;

public class ChartTest {
    @Test
    public void testDeserializer() throws Exception {
        String json = "{\n" +
                "  \"queries\":[\n" +
                "      {\n" +
                "          \"type\": \"eq\",\n" +
                "          \"field\": \"f1\",\n" +
                "          \"value\": \"1\"\n" +
                "      }\n" +
                "  ],\n" +
                "  \"config\": {\n" +
                "    \"type\": \"oneDimensionTable\",\n" +
                "    \"classifyField\": \"type\"\n" +
                "  }\n" +
                "}";
        Chart chart = TypedSerializerBuilder.MAPPER.readValue(json, Chart.class);
        Assert.assertTrue(chart.getConfig() instanceof OneDimensionTable);
        Query query = chart.getQueries().get(0);
        Assert.assertTrue(query instanceof Eq);
        Assert.assertTrue(TypedSerializerBuilder.MAPPER.writeValueAsString(chart).contains("\"oneDimensionTable\""));
        Assert.assertTrue(TypedSerializerBuilder.MAPPER.writeValueAsString(chart).contains("\"eq\""));
    }
}
