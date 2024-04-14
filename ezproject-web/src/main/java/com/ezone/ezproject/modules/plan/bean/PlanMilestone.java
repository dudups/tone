package com.ezone.ezproject.modules.plan.bean;

import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.PlanGoal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PlanMilestone {
    private Plan plan;
    private List<PlanGoal> goals;
    private List<PlanGoal> descendantPlanGoals;
}
