package com.ezone.ezproject.modules.cli.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalCallback<T> {
    @NotNull
    private Long flowId;
    private boolean approved;

    private T data;
}
