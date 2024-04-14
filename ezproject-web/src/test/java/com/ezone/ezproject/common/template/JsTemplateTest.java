package com.ezone.ezproject.common.template;

import org.junit.Assert;
import org.junit.Test;

public class JsTemplateTest {

    @Test
    public void testRender() throws Exception {
        String json = "{\n" +
                "    \"aggregations\": {\n" +
                "        \"value_count#total\": {\n" +
                "            \"value\": 37\n" +
                "        },\n" +
                "        \"value_count#count\": {\n" +
                "            \"value\": 37\n" +
                "        },\n" +
                "        \"sterms#type\": {\n" +
                "            \"doc_count_error_upper_bound\": 0,\n" +
                "            \"sum_other_doc_count\": 0,\n" +
                "            \"buckets\": [{\n" +
                "                \"key\": \"story\",\n" +
                "                \"doc_count\": 36,\n" +
                "                \"value_count#type\": {\n" +
                "                    \"value\": 36\n" +
                "                }\n" +
                "            }, {\n" +
                "                \"key\": \"bug\",\n" +
                "                \"doc_count\": 1,\n" +
                "                \"value_count#type\": {\n" +
                "                    \"value\": 1\n" +
                "                }\n" +
                "            }]\n" +
                "        }\n" +
                "    }\n" +
                "}";
        String result = JsTemplate.render("/js/OneDimensionTable.js").render(json);
        Assert.assertEquals("{\"x\":[{\"key\":\"story\",\"value\":36},{\"key\":\"bug\",\"value\":1}],\"total\":37,\"xValues\":[\"story\",\"bug\"]}", result);
    }
}
