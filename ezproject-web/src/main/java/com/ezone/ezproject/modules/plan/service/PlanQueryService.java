package com.ezone.ezproject.modules.plan.service;

import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.PlanExample;
import com.ezone.ezproject.dal.mapper.ExtPlanMapper;
import com.ezone.ezproject.dal.mapper.ProjectMapper;
import com.ezone.ezproject.modules.plan.bean.ProjectPlan;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class PlanQueryService {
    private ExtPlanMapper planMapper;
    private ProjectMapper projectMapper;

    public Plan select(Long id) {
        if (null == id || id <= 0) {
            return null;
        }
        return planMapper.selectByPrimaryKey(id);
    }

    public ProjectPlan selectProjectPlan(Long id) {
        if (null == id || id <= 0) {
            return null;
        }
        Plan plan = planMapper.selectByPrimaryKey(id);
        if (null == plan) {
            return null;
        }
        return ProjectPlan.builder()
                .plan(plan)
                .project(projectMapper.selectByPrimaryKey(plan.getProjectId()))
                .build();
    }

    public List<Plan> select(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ListUtils.EMPTY_LIST;
        }
        PlanExample example = new PlanExample();
        example.createCriteria().andIdIn(ids);
        return planMapper.selectByExample(example);
    }

    public List<Plan> select(Long projectId, List<Long> planIds) {
        if (CollectionUtils.isEmpty(planIds)) {
            return ListUtils.EMPTY_LIST;
        }
        PlanExample example = new PlanExample();
        example.createCriteria().andIdIn(planIds).andProjectIdEqualTo(projectId);
        return planMapper.selectByExample(example);
    }

    /**
     * 孩子节点
     *
     * @param parentId
     * @param isActive
     * @return
     */
    public List<Plan> selectChildren(Long parentId, Boolean isActive) {
        PlanExample example = new PlanExample();
        if (null == isActive) {
            example.createCriteria().andParentIdEqualTo(parentId);
        } else {
            example.createCriteria().andParentIdEqualTo(parentId).andIsActiveEqualTo(isActive);
        }
        return planMapper.selectByExample(example);
    }

    /**
     * select by L0 ancestor
     *
     * @param ancestorIds must be L0
     * @return group by ancestor
     */
    @NotNull
    public Map<Long, List<Plan>> selectByL0Ancestor(List<Long> ancestorIds) {
        PlanExample example = new PlanExample();
        example.createCriteria().andAncestorIdIn(ancestorIds);
        List<Plan> descendants = planMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(descendants)) {
            return MapUtils.EMPTY_MAP;
        }
        return descendants.stream().collect(Collectors.groupingBy(Plan::getAncestorId));
    }

    /**
     * 子孙节点
     *
     * @param ancestorId 任意一级祖先计划
     * @param isActive
     * @return
     */
    public List<Plan> selectDescendant(Long ancestorId, Boolean isActive) {
        if (ancestorId == null || ancestorId <= 0) {
            return ListUtils.EMPTY_LIST;
        }
        return selectDescendant(select(ancestorId), isActive);
    }

    public List<Plan> selectTree(Long rootId, Boolean isActive) {
        if (rootId == null || rootId <= 0) {
            return ListUtils.EMPTY_LIST;
        }
        return selectTree(select(rootId), isActive);
    }

    public List<Plan> selectTree(Plan plan, Boolean isActive) {
        if (plan == null) {
            return ListUtils.EMPTY_LIST;
        }
        List<Plan> tree = new ArrayList<>();
        tree.add(plan);
        tree.addAll(selectDescendant(plan, isActive));
        return tree;
    }

    /**
     * 子孙节点
     *
     * @param plan     任意一级祖先计划
     * @param isActive
     * @return
     */
    public List<Plan> selectDescendant(Plan plan, Boolean isActive) {
        if (plan == null) {
            return ListUtils.EMPTY_LIST;
        }
        Long ancestorId;
        if (plan.getParentId() > 0) {
            ancestorId = plan.getAncestorId();
        } else {
            ancestorId = plan.getId();
        }
        PlanExample example = new PlanExample();
        if (null == isActive) {
            example.createCriteria().andAncestorIdEqualTo(ancestorId);
        } else {
            example.createCriteria().andAncestorIdEqualTo(ancestorId).andIsActiveEqualTo(isActive);
        }
        List<Plan> plans = planMapper.selectByExample(example);
        if (plan.getParentId() > 0 && CollectionUtils.isNotEmpty(plans)) {
            Map<Long, List<Plan>> childrenMap = plans.stream().collect(Collectors.groupingBy(Plan::getParentId));
            List<Plan> descendant = new ArrayList<>();
            descendant(plan.getId(), childrenMap, descendant);
            return descendant;
        }
        return plans;
    }

    private void descendant(Long id, Map<Long, List<Plan>> childrenMap, List<Plan> descendant) {
        List<Plan> children = childrenMap.get(id);
        if (CollectionUtils.isNotEmpty(children)) {
            descendant.addAll(children);
            children.forEach(child -> descendant(child.getId(), childrenMap, descendant));
        }
    }

    @Nullable
    public List<Plan> selectByProjectId(Long projectId, Boolean isActive) {
        PlanExample example = new PlanExample();
        if (null == isActive) {
            example.createCriteria().andProjectIdEqualTo(projectId);
        } else {
            example.createCriteria().andProjectIdEqualTo(projectId).andIsActiveEqualTo(isActive);
        }
        return planMapper.selectByExample(example);
    }

    public TotalBean<Plan> selectL0WithEndTimeDesc(Long projectId, Integer pageNumber, Integer pageSize) {
        PlanExample example = new PlanExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andParentIdEqualTo(0L);
        example.setOrderByClause("`end_time` desc");
        Page page = PageHelper.startPage(pageNumber, pageSize, true);
        List<Plan> plans = planMapper.selectByExample(example);
        return TotalBean.<Plan>builder().total(page.getTotal()).list(plans).build();
    }

    public TotalBean<Plan> selectWithEndTimeDesc(Long projectId, Integer pageNumber, Integer pageSize) {
        PlanExample example = new PlanExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        example.setOrderByClause("`end_time` desc");
        Page page = PageHelper.startPage(pageNumber, pageSize, true);
        List<Plan> plans = planMapper.selectByExample(example);
        return TotalBean.<Plan>builder().total(page.getTotal()).list(plans).build();
    }

    /**
     * last_modify_time desc
     *
     * @param projectId
     * @param q
     * @param pageNumber
     * @param pageSize
     * @return
     */
    public TotalBean<Plan> searchInactive(Long projectId, String q, Integer pageNumber, Integer pageSize) {
        PlanExample example = new PlanExample();
        PlanExample.Criteria criteria = example.createCriteria();
        criteria.andProjectIdEqualTo(projectId).andIsActiveEqualTo(false);
        if (StringUtils.isNotEmpty(q)) {
            criteria.andNameLike(String.format("%s%%", q));
        }
        example.setOrderByClause("`last_modify_time` desc");
        Page page = PageHelper.startPage(pageNumber, pageSize, true);
        List<Plan> plans = planMapper.selectByExample(example);
        return TotalBean.<Plan>builder().total(page.getTotal()).list(plans).build();
    }

    /**
     * 按(活跃，起始时间最近)排序返回
     *
     * @param projectId
     * @param q
     * @param pageNumber
     * @param pageSize
     * @return
     */
    public TotalBean<Plan> searchAll(Long projectId, String q, Integer pageNumber, Integer pageSize) {
        PlanExample example = new PlanExample();
        PlanExample.Criteria criteria = example.createCriteria();
        criteria.andProjectIdEqualTo(projectId);
        if (StringUtils.isNotEmpty(q)) {
            criteria.andNameLike(String.format("%s%%", q));
        }
        example.setOrderByClause("`is_active` desc, `start_time` desc");
        Page page = PageHelper.startPage(pageNumber, pageSize, true);
        List<Plan> plans = planMapper.selectByExample(example);
        return TotalBean.<Plan>builder().total(page.getTotal()).list(plans).build();
    }

    public Plan selectHigherDeliverLineRankPlan(Long projectId, String rank) {
        PlanExample example = new PlanExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andDeliverLineRankGreaterThan(rank);
        example.setOrderByClause("`deliver_line_rank` asc");
        PageHelper.startPage(1, 1, false);
        List<Plan> plans = planMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(plans)) {
            return null;
        }
        return plans.get(0);
    }

    public Plan selectLowerDeliverLineRankPlan(Long projectId, String rank) {
        PlanExample example = new PlanExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andDeliverLineRankLessThan(rank);
        example.setOrderByClause("`deliver_line_rank` desc");
        PageHelper.startPage(1, 1, false);
        List<Plan> plans = planMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(plans)) {
            return null;
        }
        return plans.get(0);
    }

    public List<Plan> selectInactivePlanForClean(Long projectId, Long expireDays) {
        PlanExample example = new PlanExample();
        Date expireDate = DateUtils.addDays(new Date(), -expireDays.intValue());
        example.createCriteria()
                .andProjectIdEqualTo(projectId)
                .andIsActiveEqualTo(false)
                .andLastModifyTimeLessThan(expireDate);
        return planMapper.selectByExample(example);
    }

    public List<Plan> selectByEndTime(Long projectId, Date start, Date end) {
        PlanExample example = new PlanExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andEndTimeBetween(start, end);
        return planMapper.selectByExample(example);
    }

    public @NotNull Map<Long, Long> plansCount(List<Long> projectIds) {
        Map<Long, Long> result = new HashMap<>();
        if (CollectionUtils.isEmpty(projectIds)) {
            return result;
        }
        List<Map> plansCount = planMapper.planCountGroupByProject(projectIds);
        if (CollectionUtils.isNotEmpty(plansCount)) {
            plansCount.forEach(row -> result.put(Long.valueOf(row.get("project_id").toString()),
                    Long.valueOf(row.get("count").toString())));
        }
        return result;
    }

    public @NotNull Map<Long, List<Plan>> projectPlans(List<Long> projectIds, Boolean isActive) {
        Map<Long, List<Plan>> result = new HashMap<>();
        if (CollectionUtils.isEmpty(projectIds)) {
            return result;
        }
        PlanExample example = new PlanExample();
        if (null == isActive) {
            example.createCriteria().andProjectIdIn(projectIds);
        } else {
            example.createCriteria().andProjectIdIn(projectIds).andIsActiveEqualTo(isActive);
        }
        List<Plan> plans = planMapper.selectByExample(example);
        if (CollectionUtils.isNotEmpty(plans)) {
            result = plans.stream().collect(Collectors.groupingBy(Plan::getProjectId));
        }
        return result;
    }

    public Map<Long, List<Plan>> projectPlans(List<Long> projectIds) {
        return projectPlans(projectIds, null);
    }
}
