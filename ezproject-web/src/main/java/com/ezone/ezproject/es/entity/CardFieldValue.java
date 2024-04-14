package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.modules.card.field.FieldUtil;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardFieldValue {
    public static final String FIELD_KEY = "fieldKey";

    @ApiModelProperty(value = "字段标识key", example = "title")
    private String fieldKey;
    @ApiModelProperty(value = "字段值", example = "t1")
    @NotNull
    private Object fieldValue;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardFieldValue that = (CardFieldValue) o;
        return Objects.equals(fieldKey, that.fieldKey) && FieldUtil.equals(fieldValue, that.fieldValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldKey, fieldValue);
    }
}
