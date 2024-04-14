package com.ezone.ezproject.modules.plan.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.mapper.PlanMapper;
import com.ezone.ezproject.modules.card.service.CardHelper;
import com.ezone.ezproject.modules.plan.bean.CreatePlanTreeRequest;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class CreatePlanTreeHelper {
    private String user;
    private CreatePlanTreeRequest request;
    private PlanMapper planMapper;
    private CardHelper cardHelper;

    public Plan create() {
        Long parentId = request.getParentId();
        Long ancestorId = 0L;
        if (request.getParentId() > 0L) {
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
        List<Plan> descendant = newDescendant(parent, request.getChildren());
        if (CollectionUtils.isEmpty(descendant)) {
            return ListUtils.EMPTY_LIST;
        }
        List<String> ranks = cardHelper.nextRanks(request.getProjectId(), descendant.size());
        for (int i = 0; i < descendant.size(); i++) {
            descendant.get(i).setDeliverLineRank(ranks.get(i));
        }
        return descendant;
    }

    private List<Plan> newDescendant(Plan parent, List<CreatePlanTreeRequest.ChildPlan> children) {
        if (CollectionUtils.isEmpty(children)) {
            return ListUtils.EMPTY_LIST;
        }
        List<Plan> plans = new ArrayList<>();
        children.forEach(child -> {
            Plan plan = Plan.builder()
                    .id(IdUtil.generateId())
                    .name(child.getName())
                    .projectId(request.getProjectId())
                    .parentId(parent.getId())
                    .ancestorId(parent.getParentId() > 0 ? parent.getAncestorId() : parent.getId())
                    .startTime(child.getStartTime())
                    .endTime(child.getEndTime())
                    .isActive(true)
                    .createUser(user)
                    .createTime(new Date())
                    .lastModifyUser(user)
                    .lastModifyTime(new Date())
                    .build();
            plans.add(plan);
            plans.addAll(newDescendant(plan, child.getChildren()));
        });
        return plans;
    }
}
