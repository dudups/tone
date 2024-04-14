package com.ezone.ezproject.modules.project.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CompanyCardTypeConf {
    @ApiModelProperty(value = "卡片类型标识key", example = "story")
    private String key;
    @Size(max = 100)
    @ApiModelProperty(value = "卡片类型具体描述")
    private String description;
    @NotNull
    @Size(min = 1, max = 10)
    @ApiModelProperty(value = "卡片类型名称", example = "Story")
    private String name;
    @NotNull
    @ApiModelProperty(value = "卡片颜色", example = "#FFFFFF")
    private String color;
    @NotNull
    @ApiModelProperty(value = "卡片内置类型", example = "#FFFFFF")
    private String innerType;
}
