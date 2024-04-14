package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.common.validate.Uniq;
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
public class CardFieldFlow {
    public static final String FIELD_KEY = "fieldKey";

    @ApiModelProperty(value = "字段标识key", example = "title")
    private String fieldKey;

    @Uniq(field = CardFieldValueFlow.FIELD_VALUE, message = "触发字段值不能重复设置！")
    private List<CardFieldValueFlow> flows;

    @ApiModelProperty(value = "当被触发字段已有值时，是否强制覆盖")
    private boolean force;
}
