package com.ezone.ezproject.modules.card.bean;

import com.ezone.ezproject.modules.card.bean.query.Query;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.elasticsearch.search.sort.SortOrder;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchEsRequest {

    @ApiModelProperty(value = "查询条件", example = "参考这个api的返回值：card/searchQueryExamples")
    private List<Query> queries;

    @ApiModelProperty(value = "返回卡片字段")
    private String[] fields;

    @ApiModelProperty(value = "排序规则")
    private List<Sort> sorts;

    @ApiModelProperty(value = "字段ID-仅自定义字段产生字段ID,请求时不用传，后端自动处理", example = "{\"custom_1_keyword\": 1}")
    private Map<String, Long> fieldIds;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Sort {
        @ApiModelProperty(value = "排序字段")
        @NotNull
        @Size(min = 1)
        private String field;

        @ApiModelProperty(value = "正序/倒序")
        @NotNull
        @JsonDeserialize(using = SortOrderDeserializer.class)
        private SortOrder order;
    }

    public static class SortOrderDeserializer extends JsonDeserializer<SortOrder> {
        @Override
        public SortOrder deserialize(JsonParser p, DeserializationContext context) throws IOException {
            return SortOrder.fromString(p.getValueAsString());
        }
    }
}
