package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.common.validate.Uniq;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardFieldValueFlow {
    public static final String FIELD_VALUE = "fieldValue";

    @ApiModelProperty(value = "字段值", example = "t1")
    @NotNull
    private Object fieldValue;

    @Size(min = 1)
    @Uniq(field = CardFieldValue.FIELD_KEY, message = "被触发字段不能重复设置！")
    private List<CardFieldValue> targetFieldValues;
}
