package com.ezone.ezproject.modules.card.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCardsFieldsRequest {
    @NotNull
    @Size(min = 1)
    private List<Long> ids;

    @NotNull
    @Size(min = 1)
    private Map<String, List<FieldOperation>> typeMap;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldOperation {
        private String field;
        @Builder.Default
        private FieldOperationType opType = FieldOperationType.SET;
        @ApiModelProperty(value = "需要满足field定义，例如即使对于列表字段添加一个字段也必须传列表")
        private Object value;
    }

    public enum FieldOperationType {
        SET, ADD, REMOVE
    }
}
