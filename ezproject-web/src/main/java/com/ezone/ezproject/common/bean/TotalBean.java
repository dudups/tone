package com.ezone.ezproject.common.bean;

import com.ezone.ezproject.modules.card.bean.ReferenceValues;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotalBean<T> {
    @ApiModelProperty(value = "总数量")
    private long total;
    @ApiModelProperty(value = "当前页面返回对象集合")
    private List<T> list;
    @ApiModelProperty(value = "引用类型字段值对应的引用对象，如:父卡片、计划、故事地图")
    private ReferenceValues refs;
}
