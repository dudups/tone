package com.ezone.ezproject.modules.card.field.bean;

import com.ezone.ezproject.es.entity.CardField;
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
public class FieldChange {
    private CardField field;
    private Object fromValue;
    private Object toValue;
}
