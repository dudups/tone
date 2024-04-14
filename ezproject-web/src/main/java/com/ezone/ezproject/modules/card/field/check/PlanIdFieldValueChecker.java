package com.ezone.ezproject.modules.card.field.check;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.function.Function;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class PlanIdFieldValueChecker implements IFieldValueChecker {
    private Long projectId;
    private Function<Long, Plan> findPlanById;

    @Override
    public void check(Object planId) throws CodedException {
        Long id = FieldUtil.toLong(planId);
        if (id != null && id > 0) {
            Plan plan = findPlanById.apply(id);
            if (plan == null) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "绑定计划不存在!");
            } else {
                if (!plan.getProjectId().equals(projectId)) {
                    throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "不能跨项目绑定计划!");
                }
            }
        }
    }
}
