package com.ezone.ezproject.modules.portfolio.bean;

import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.modules.plan.bean.PlansAndProgresses;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioProjectPlan {
    @ApiModelProperty(value = "项目中包含的计划")
    private List<Plan> plans;

    @ApiModelProperty(value = "项目计划进度，key为计划ID")
    Map<Long, PlansAndProgresses.Progress> planProgressMap;

}
