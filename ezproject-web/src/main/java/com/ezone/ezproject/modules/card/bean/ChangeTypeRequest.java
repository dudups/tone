package com.ezone.ezproject.modules.card.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChangeTypeRequest {
    @NotNull
    @Size(min = 1)
    private List<Long> ids;

    @ApiModelProperty(value = "转换配置，key为源卡片类型、value为转换配置")
    @NotNull
    @Size(min = 1)
    private Map<String, TypeChangeConfig> typeMap;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TypeChangeConfig {
        @ApiModelProperty("转换的目标类型")
        @NotNull
        private String toType;
        @ApiModelProperty("源类型中的状态与新类型状态对应关系")
        @NotNull
        private Map<String, String> statusMap;

    }
}

