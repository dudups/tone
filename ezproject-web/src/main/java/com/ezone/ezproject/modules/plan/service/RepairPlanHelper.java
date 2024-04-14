package com.ezone.ezproject.modules.plan.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.PlanExample;
import com.ezone.ezproject.dal.mapper.PlanMapper;
import com.ezone.ezproject.modules.card.service.CardCmdService;
import com.ezone.ezproject.modules.card.service.CardQueryService;
import com.ezone.ezproject.modules.card.service.CardSearchService;
import com.ezone.ezproject.modules.common.OperationContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class RepairPlanHelper {
    private PlanMapper planMapper;
    private CardCmdService cardCmdService;
    private CardQueryService cardQueryService;
    private CardSearchService cardSearchService;

    public void processAncestorsDeleteOrInactive() {
        PlanExample example = new PlanExample();
        List<Plan> plans = planMapper.selectByExample(example);
        Map<Long, Plan> planMap = plans.stream().collect(Collectors.toMap(Plan::getId, plan -> plan));
        Map<Long, List<Plan>> allSubPlanMap = plans.stream().filter(plan -> !plan.getParentId().equals(0L)).collect(Collectors.groupingBy(Plan::getAncestorId));
        allSubPlanMap.forEach((rootId, subPlans) -> {
            Plan root = planMap.get(rootId);
            if (root == null) {
                //根节点被删除，只能先关连到需求
                root = Plan.builder().id(0L).ancestorId(0L).build();
            }
            PlanNode planNode = new PlanNode(root, subPlans);
            Map<PlanAncestorState, List<Plan>> errorPlaneMap = findAncestorsDeleteOrInactiveMap(planNode);
            List<Plan> inactivePlans = errorPlaneMap.get(PlanAncestorState.inactive);
            if (inactivePlans != null && !inactivePlans.isEmpty()) {
                //设置卡片信息
                for (Plan plan : inactivePlans) {
                    Plan crucialPlan = findNearestInactivePlan(planNode, plan);
                    plan.setIsActive(false);
                    plan.setLastModifyTime(new Date());
                }
                inactive(inactivePlans);
            }
            List<Plan> deletePlans = errorPlaneMap.get(PlanAncestorState.delete);
            if (deletePlans != null && !deletePlans.isEmpty()) {
                for (Plan plan : deletePlans) {
                    plan.setIsActive(false);
                    plan.setParentId(root.getId());
                    plan.setAncestorId(root.getId());
                    plan.setLastModifyTime(new Date());
                }
                inactive(deletePlans);
            }
        });
    }

    public void inactive(List<Plan> plans) {
        Date now = new Date();
        plans.forEach(p -> {
            String user = p.getLastModifyUser();
            p.setIsActive(false);
            p.setLastModifyUser(user);
            p.setLastModifyTime(now);
            planMapper.updateByPrimaryKey(p);
            try {
                OperationContext opContext = OperationContext.instance(user);
                cardCmdService.onPlanInActive(opContext, p.getProjectId(), Collections.singletonList(p.getId()));
            } catch (IOException e) {
                log.error(String.format("plan %s inactive!", p.getId()), e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        });
    }

    /**
     * 查找有删除或当档时需要后续节点需要挂载的节点
     *
     * @return 如果最发现了删除了点节，刚返回根节点，如果是
     */
    protected Plan findFirstInactiveOrDeletePrePlan(PlanNode planNode) {
        List<Plan> subPlans = planNode.getSubPlans();
        Map<Long, Plan> planMap = subPlans.stream().collect(Collectors.toMap(Plan::getParentId, plan -> plan));
        Plan root = planNode.getRoot();
        Plan current = root;
        int count = 0;
        while (current != null && current.getIsActive()) {
            Plan temp = planMap.get(current.getId());
            if (temp == null && count < subPlans.size()) {
                return root;
            } else if (temp == null) {
                break;
            } else if (Boolean.FALSE.equals(temp.getIsActive())) {
                current = temp;
                break;
            } else {
                current = temp;
                count++;
            }
        }
        return current;
    }

    /**
     * 查找plan祖先节点中最近的inactive节点，不包含自己
     *
     * @return 如果最发现了最近祖先删除，刚返回根节点，否则返回最近的inactive节点。
     */
    protected Plan findNearestInactivePlan(PlanNode planNode, Plan searchPlan) {
        List<Plan> subPlans = planNode.getSubPlans();
        Map<Long, Plan> planMap = subPlans.stream().collect(Collectors.toMap(Plan::getId, plan -> plan));
        Plan current = searchPlan;
        while (current != null) {
            current = planMap.get(current.getParentId());
            if (current == null) {
                return planNode.getRoot();
            } else if (Boolean.FALSE.equals(current.getIsActive())) {
                break;
            }
        }
        return current;
    }


    /**
     * 查找祖先节点已经归档或删除，但子节点未归档的节点
     *
     * @return 有问题的节点，分两类："delete"、"inactive"
     */
    protected List<Plan> findAncestorsDeleteOrInactive(PlanNode planNode) {
        List<Plan> subPlans = planNode.getSubPlans();
        Map<Long, Plan> planMap = subPlans.stream().collect(Collectors.toMap(Plan::getId, plan -> plan));
        Plan root = planNode.getRoot();
        planMap.put(root.getId(), root);
        List<Plan> errorPlans = new ArrayList<>();
        for (Plan plan : subPlans) {
            PlanAncestorState planAncestorState = ancestorIsError(planMap, plan, root);
            if (planAncestorState == PlanAncestorState.error) {
                errorPlans.add(plan);
            }
        }
        return errorPlans;
    }

    /**
     * 查找祖先节点已经归档或删除，但子节点未归档的节点     *
     *
     * @param planNode
     * @return 有问题的节点，分两类："delete"、"inactive"     *
     */
    protected Map<PlanAncestorState, List<Plan>> findAncestorsDeleteOrInactiveMap(PlanNode planNode) {
        List<Plan> subPlans = planNode.getSubPlans();
        Map<Long, Plan> planMap = subPlans.stream().collect(Collectors.toMap(Plan::getId, plan -> plan));
        Plan root = planNode.getRoot();

        planMap.put(root.getId(), root);
        Map<PlanAncestorState, List<Plan>> result = new EnumMap<>(PlanAncestorState.class);
        for (Plan plan : subPlans) {
            PlanAncestorState planAncestorState = ancestorIsDeleteOrInactive(planMap, plan, root);
            switch (planAncestorState) {
                case delete:
                    List<Plan> deletes = result.getOrDefault(PlanAncestorState.delete, new ArrayList<>());
                    deletes.add(plan);
                    result.put(PlanAncestorState.delete, deletes);
                    break;
                case inactive:
                    List<Plan> inactivates = result.getOrDefault(PlanAncestorState.inactive, new ArrayList<>());
                    inactivates.add(plan);
                    result.put(PlanAncestorState.inactive, inactivates);
                    break;
                default:
            }
        }
        return result;
    }

    private PlanAncestorState ancestorIsError(Map<Long, Plan> plans, Plan current, Plan root) {
        if (root.getId().equals(current.getId())) {
            return PlanAncestorState.normal;
        }
        Plan parent = plans.get(current.getParentId());
        if (parent == null) {
            return PlanAncestorState.error;
        } else if (parent.getIsActive().equals(false)) {
            return PlanAncestorState.error;
        } else {
            return ancestorIsDeleteOrInactive(plans, parent, root);
        }
    }

    private PlanAncestorState ancestorIsDeleteOrInactive(Map<Long, Plan> plans, Plan current, Plan root) {
        if (!current.getIsActive()) {
            return PlanAncestorState.normal;
        }
        if (root.getId().equals(current.getId())) {
            return PlanAncestorState.normal;
        }
        Plan parent = plans.get(current.getParentId());
        if (parent == null) {
            return PlanAncestorState.delete;
        } else if (parent.getIsActive().equals(false)) {
            return PlanAncestorState.inactive;
        } else {
            return ancestorIsDeleteOrInactive(plans, parent, root);
        }
    }

    enum PlanAncestorState {
        /**
         * 正常情况
         */
        normal,


        /**
         * 如果某节点祖先节点有一个是物理删除，而自己是active
         */
        delete,
        /**
         * 如果某节点祖先节点有一个是逻辑删除，而自己是active
         */
        inactive,

        /***
         * 如果某节点祖先节点有一个是物理删除，或者祖先节点有一个是已经归档，而自己是active。则状态为error
         * 包含delete与inactive
         */
        error
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlanNode {
        private Plan root;
        private List<Plan> subPlans = new ArrayList<>();
    }
}
