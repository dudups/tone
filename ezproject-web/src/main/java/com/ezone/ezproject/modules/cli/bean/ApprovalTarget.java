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
public class ApprovalTarget {
    @NotNull
    private ApprovalType approvalType;
    @NotNull
    private Long targetId;
}
