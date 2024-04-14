package com.ezone.ezproject.modules.card.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class QuickCreateCardRequest {
    @ApiModelProperty(value = "卡片类型")
    @NotNull
    @Size(min = 1)
    private String type;
    @ApiModelProperty(value = "卡片title")
    @NotNull
    @Size(min = 1)
    private String title;
    @ApiModelProperty(value = "计划id")
    @Min(0)
    private long planId;
    @ApiModelProperty(value = "父卡片id")
    @Min(0)
    private long parentId;
    @ApiModelProperty(value = "故事地图二级分类id")
    @Min(0)
    private long storyMapNodeId;
}
