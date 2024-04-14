package com.ezone.ezproject.modules.plan.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.mapper.PlanMapper;
import com.ezone.ezproject.modules.card.service.CardHelper;
import com.ezone.ezproject.modules.plan.bean.CreatePlanRequest;
import com.ezone.ezproject.modules.plan.bean.UpdatePlanRequest;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.springframework.http.HttpStatus;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class CreatePlanHelper {
    private String user;
    private CreatePlanRequest request;
    private PlanMapper planMapper;
    private Function<Plan, List<Plan>> findDescendantPlan;
    private CardHelper cardHelper;

    public Plan create() {
        Long parentId = request.getParentId();
        Long ancestorId = 0L;
        if (parentId > 0L) {
            Plan parent = planMapper.selectByPrimaryKey(request.getParentId());
            ancestorId = parent.getParentId() > 0 ? parent.getAncestorId() : parentId;
        }
        Plan plan = newPlan(parentId, ancestorId);
        planMapper.insert(plan);
        newDescendant(plan).forEach(planMapper::insert);
        return plan;
    }

    @NotNull
    private Plan newPlan(Long parentId, Long ancestorId) {
        return Plan.builder()
                .id(IdUtil.generateId())
                .name(request.getName())
                .projectId(request.getProjectId())
                .parentId(parentId)
                .ancestorId(ancestorId)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .isActive(true)
                .deliverLineRank(cardHelper.nextRank(request.getProjectId()))
                .createUser(user)
                .createTime(new Date())
                .lastModifyUser(user)
                .lastModifyTime(new Date())
                .build();
    }

    @NotNull
    private List<Plan> newDescendant(Plan parent) {
        if (request.getTemplatePlanId() <= 0L) {
            return ListUtils.EMPTY_LIST;
        }
        Plan template = planMapper.selectByPrimaryKey(request.getTemplatePlanId());
        if (!template.getParentId().equals(request.getParentId())) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "Invalid template plan!");
        }
        List<Plan> templateDescendant = findDescendantPlan.apply(template);
        if (CollectionUtils.isEmpty(templateDescendant)) {
            return ListUtils.EMPTY_LIST;
        }
        Map<Long, Long> templateParent = new HashMap<>();
        Map<Long, Long> copyFrom = new HashMap<>();
        Map<Long, Long> copyTo = new HashMap<>();
        List<Plan> plans = new ArrayList<>();
        List<String> ranks = cardHelper.nextRanks(request.getProjectId(), templateDescendant.size());
        for (int i = 0; i < templateDescendant.size(); i++) {
            Plan plan = templateDescendant.get(i);
            templateParent.put(plan.getId(), plan.getParentId());
            Long id = IdUtil.generateId();
            plans.add(Plan.builder()
                    .id(id)
                    .name(plan.getName())
                    .projectId(request.getProjectId())
                    // not final
                    .parentId(parent.getId())
                    .ancestorId(parent.getParentId() > 0 ? parent.getAncestorId() : parent.getId())
                    .startTime(parent.getStartTime())
                    .endTime(parent.getEndTime())
                    .isActive(true)
                    .deliverLineRank(ranks.get(i))
                    .createUser(user)
                    .createTime(new Date())
                    .lastModifyUser(user)
                    .lastModifyTime(new Date())
                    .build());
            copyFrom.put(id, plan.getId());
            copyTo.put(plan.getId(), id);
        }
        // final parent
        plans.forEach(plan -> {
            Long from = copyFrom.get(plan.getId());
            Long fromParent = templateParent.get(from);
            Long toParent = copyTo.get(fromParent);
            plan.setParentId(toParent);
        });
        return plans;
    }

    public static Map<String, Object> calculateUpdateContent(Plan sourcePlan, UpdatePlanRequest request) {
        Map<String, Object> updateContent = new HashMap<>();
        if(!sourcePlan.getName().equals(request.getName())){
            updateContent.put("name", request.getName());
        }
        if(!sourcePlan.getParentId().equals(request.getParentId())){
            updateContent.put("parentId", request.getParentId());
        }
        if(!sourcePlan.getStartTime().equals(request.getStartTime())){
            updateContent.put("startTime", request.getStartTime());
        }
        if(!sourcePlan.getEndTime().equals(request.getEndTime())){
            updateContent.put("endTime", request.getEndTime());
        }
        return updateContent;
    }

}
