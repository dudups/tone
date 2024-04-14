package com.ezone.ezproject.modules.plan.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.PlanGoal;
import com.ezone.ezproject.dal.entity.PlanGoalExample;
import com.ezone.ezproject.dal.mapper.PlanGoalMapper;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.plan.bean.PlanGoalRequest;
import com.ezone.ezproject.modules.plan.bean.PlanMilestone;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class PlanGoalService {
    private PlanGoalMapper planGoalMapper;
    
    private PlanQueryService planQueryService;

    private UserService userService;

    @Transactional
    public PlanGoal create(Long planId, PlanGoalRequest request) {
        String user = userService.currentUserName();
        PlanGoal goal = PlanGoal.builder()
                .id(IdUtil.generateId())
                .planId(planId)
                .owner(request.getOwner())
                .status(request.getStatus().name())
                .description(request.getDescription())
                .createTime(new Date())
                .createUser(user)
                .lastModifyTime(new Date())
                .lastModifyUser(user)
                .build();
        planGoalMapper.insert(goal);
        return goal;
    }

    @Transactional
    public PlanGoal update(Long id, PlanGoalRequest request) {
        PlanGoal goal = planGoalMapper.selectByPrimaryKey(id);
        String user = userService.currentUserName();
        goal.setOwner(request.getOwner());
        goal.setStatus(request.getStatus().name());
        goal.setDescription(request.getDescription());
        goal.setLastModifyTime(new Date());
        goal.setLastModifyUser(user);
        planGoalMapper.updateByPrimaryKey(goal);
        return goal;
    }

    @Transactional
    public void delete(Long id) {
        planGoalMapper.deleteByPrimaryKey(id);
    }

    @Transactional
    public void deleteByPlanId(List<Long> planIds) {
        if (CollectionUtils.isEmpty(planIds)) {
            return;
        }
        PlanGoalExample example = new PlanGoalExample();
        example.createCriteria().andPlanIdIn(planIds);
        planGoalMapper.deleteByExample(example);
    }

    public List<PlanGoal> selectByPlanId(Long planId) {
        PlanGoalExample example = new PlanGoalExample();
        example.createCriteria().andPlanIdEqualTo(planId);
        return planGoalMapper.selectByExample(example);
    }

    @NotNull
    public Map<Long, List<PlanGoal>> selectByPlanId(List<Long> planIds) {
        if (CollectionUtils.isEmpty(planIds)) {
            return MapUtils.EMPTY_MAP;
        }
        PlanGoalExample example = new PlanGoalExample();
        example.createCriteria().andPlanIdIn(planIds);
        List<PlanGoal> goals = planGoalMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(goals)) {
            return MapUtils.EMPTY_MAP;
        }
        return goals.stream().collect(Collectors.groupingBy(PlanGoal::getPlanId));
    }

    public TotalBean<PlanMilestone> milestone(Long projectId, Integer pageNumber, Integer pageSize) {
        TotalBean<Plan> totalPlans = planQueryService.selectWithEndTimeDesc(projectId, pageNumber, pageSize);
        List<Plan> plans = totalPlans.getList();
        if (CollectionUtils.isEmpty(plans)) {
            return TotalBean.<PlanMilestone>builder().total(0).build();
        }
        List<Long> planIds = plans.stream().map(Plan::getId).collect(Collectors.toList());
        Map<Long, List<PlanGoal>> goals = selectByPlanId(planIds);
        return TotalBean.<PlanMilestone>builder().total(totalPlans.getTotal()).list(plans.stream().map(plan -> {
            List<PlanGoal> planGoals = goals.get(plan.getId());
            return PlanMilestone.builder().plan(plan).goals(planGoals == null ? ListUtils.EMPTY_LIST : planGoals).descendantPlanGoals(ListUtils.EMPTY_LIST).build();
        }).collect(Collectors.toList())).build();
    }

    public TotalBean<PlanMilestone> aggL0Milestone(Long projectId, Integer pageNumber, Integer pageSize) {
        TotalBean<Plan> totalPlans = planQueryService.selectL0WithEndTimeDesc(projectId, pageNumber, pageSize);
        List<Plan> ancestors = totalPlans.getList();
        if (CollectionUtils.isEmpty(ancestors)) {
            return TotalBean.<PlanMilestone>builder().total(0).build();
        }
        List<Long> planIds = ancestors.stream().map(Plan::getId).collect(Collectors.toList());
        Map<Long, List<Plan>> descendants = planQueryService.selectByL0Ancestor(planIds);
        descendants.values().forEach(plans -> plans.forEach(plan -> planIds.add(plan.getId())));
        Map<Long, List<PlanGoal>> goals = selectByPlanId(planIds);
        return TotalBean.<PlanMilestone>builder().total(totalPlans.getTotal()).list(ancestors.stream().map(ancestor -> {
            List<PlanGoal> descendantGoals = new ArrayList<>();
            List<Plan> plans = descendants.get(ancestor.getId());
            if (CollectionUtils.isNotEmpty(plans)) {
                plans.forEach(plan -> {
                    List<PlanGoal> planGoals = goals.get(plan.getId());
                    if (CollectionUtils.isNotEmpty(planGoals)) {
                        descendantGoals.addAll(planGoals);
                    }
                });
            }
            List<PlanGoal> planGoals = goals.get(ancestor.getId());
            return PlanMilestone.builder().plan(ancestor).goals(planGoals == null ? ListUtils.EMPTY_LIST : planGoals).descendantPlanGoals(descendantGoals).build();
        }).collect(Collectors.toList())).build();
    }

}
