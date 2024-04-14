package com.ezone.ezproject.modules.project.bean;

import com.ezone.ezproject.es.entity.CardStatus;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardStatusesConf {
    @ApiModelProperty(value = "项目下状态列表，所有类型卡片可用")
    private List<CardStatus> statuses;

    @ApiModelProperty(value = "项目下针对每种卡片类型的状态设置")
    private Map<String, List<Integer>> cardStatusIndexesMap;
}
