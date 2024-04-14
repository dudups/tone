package com.ezone.ezproject.modules.card.bpm.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class StatusFlowResult {
        @NotNull
        @ApiModelProperty(value = "卡片ID", example = "1")
        private Long cardId;

        @NotNull
        @ApiModelProperty(value = "审批流ID", example = "1")
        private Long flowId;

        @NotNull
        @ApiModelProperty(value = "审批流结果是否通过", example = "1")
        private boolean approved;
}
