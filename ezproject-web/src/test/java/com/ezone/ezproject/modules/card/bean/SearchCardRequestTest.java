package com.ezone.ezproject.modules.card.bean;

import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class SearchCardRequestTest {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    public void testDeserializer() throws Exception {

        String json = "{\n" +
                "    \"queries\":[\n" +
                "        {\n" +
                "            \"type\": \"eq\",\n" +
                "            \"field\": \"f1\",\n" +
                "            \"value\": \"1\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        SearchEsRequest request = MAPPER.readValue(json, SearchEsRequest.class);
        Query query = request.getQueries().get(0);
        Assert.assertTrue(query instanceof Eq);
        Eq eq = (Eq) query;
        Assert.assertEquals("f1", eq.fields());
        Assert.assertEquals("1", eq.getValue());
        Assert.assertTrue(MAPPER.writeValueAsString(request).contains("\"eq\""));
    }
}
