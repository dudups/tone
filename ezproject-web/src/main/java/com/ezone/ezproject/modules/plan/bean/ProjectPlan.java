package com.ezone.ezproject.modules.plan.bean;

import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Project;
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
public class ProjectPlan {
    private Plan plan;
    private Project project;
}
