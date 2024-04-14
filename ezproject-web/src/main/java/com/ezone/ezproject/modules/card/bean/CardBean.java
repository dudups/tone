package com.ezone.ezproject.modules.card.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardBean {
    @ApiModelProperty(value = "卡片id")
    private Long id;
    @ApiModelProperty(value = "是否因目标子卡片路过而返回的祖先节点")
    private Boolean isByPassAncestor;
    @ApiModelProperty(value = "卡片字典序序号")
    private String rank;
    @ApiModelProperty(value = "卡片字段及其值")
    private Map<String, Object> card;
}
