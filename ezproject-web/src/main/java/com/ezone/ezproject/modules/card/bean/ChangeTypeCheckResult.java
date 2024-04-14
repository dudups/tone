package com.ezone.ezproject.modules.card.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChangeTypeCheckResult {
    private String fromType;
    private String toType;
    @Singular
    private List<String> disabledFields;
    @Singular
    private List<String> disabledStatuses;
    private String defaultStatus;
}
