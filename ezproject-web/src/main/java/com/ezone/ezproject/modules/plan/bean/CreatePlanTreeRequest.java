package com.ezone.ezproject.modules.plan.bean;

import com.ezone.ezproject.es.entity.PlanNotice;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CreatePlanTreeRequest {
    @Size(min = 1, max = 40)
    @ApiModelProperty(value = "计划名")
    private String name;

    @Min(1)
    @ApiModelProperty(value = "所属项目ID")
    private long projectId;

    @Min(0)
    @ApiModelProperty(value = "父计划ID")
    private long parentId;

    @NotNull
    @ApiModelProperty(value = "开始时间")
    private Date startTime;

    @NotNull
    @ApiModelProperty(value = "结束时间")
    private Date endTime;

    @ApiModelProperty(value = "子计划")
    private List<ChildPlan> children;

    @ApiModelProperty(value = "变更通知配置")
    private PlanNotice notice;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class ChildPlan {
        @Size(min = 1, max = 40)
        @ApiModelProperty(value = "计划名")
        private String name;

        @NotNull
        @ApiModelProperty(value = "开始时间")
        private Date startTime;

        @NotNull
        @ApiModelProperty(value = "结束时间")
        private Date endTime;

        private List<ChildPlan> children;
    }
}
