package com.ezone.ezproject.modules.card.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ExportRequest {
    @NotNull
    @Size(min = 1)
    private List<Long> ids;
    private List<String> exportFields;
    private boolean exportAllFields;
    private boolean exportContent;
    private boolean exportOperations;
}
