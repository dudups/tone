package com.ezone.ezproject.modules.plan.bean;

import com.ezone.ezproject.es.entity.enums.PlanGoalStatus;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PlanGoalRequest {
    @NotNull
    @ApiModelProperty(value = "负责人")
    private String owner;

    @NotNull
    @ApiModelProperty(value = "状态")
    private PlanGoalStatus status;

    @Size(min = 0, max = 255)
    @ApiModelProperty(value = "目标描述")
    private String description;
}
