package com.ezone.ezproject.modules.project.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardTypeConf {
    @ApiModelProperty(value = "卡片类型标识key", example = "story")
    private String key;
    @ApiModelProperty(value = "卡片内置类型")
    private String type;
    @ApiModelProperty(value = "卡片类型具体描述")
    private String description;
    @ApiModelProperty(value = "卡片类型是否启用")
    private boolean enable;
    @ApiModelProperty(value = "卡片类型名称", example = "Story")
    private String name;
    @ApiModelProperty(value = "卡片颜色", example = "#FFFFFF")
    private String color;
}
