package com.ezone.ezproject.modules.plan.tree;

import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.mapper.PlanMapper;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class TreeDeleteAndPromote {
    private PlanMapper planMapper;
    private List<Plan> plans;
    private Function<Plan, Boolean> isExpired;

    /**
     * @return deletePlanIds
     */
    public List<Long> run() {
        if (CollectionUtils.isEmpty(plans)) {
            return ListUtils.EMPTY_LIST;
        }
        List<Long> deletePlanIds = new ArrayList<>();
        Map<Long, List<Plan>> childrenMap = plans.stream().collect(Collectors.groupingBy(Plan::getParentId));
        childrenMap.get(0L)
                .forEach(root -> runNode(root, childrenMap, deletePlanIds)
                        .forEach(plan -> {
                            plan.setParentId(0L);
                            plan.setAncestorId(0L);
                            planMapper.updateByPrimaryKey(plan);
                        }));
        return deletePlanIds;
    }

    /**
     * root: try to delete
     * children: try to reParent promote / or return
     * @return toPromotePlans
     */
    private List<Plan> runNode(Plan root, Map<Long, List<Plan>> childrenMap, List<Long> deletePlanIds) {
        boolean isRootExpired = isExpired.apply(root);
        if (isRootExpired) {
            planMapper.deleteByPrimaryKey(root.getId());
            deletePlanIds.add(root.getId());
        }
        List<Plan> children = childrenMap.get(root.getId());
        if (CollectionUtils.isEmpty(children)) {
            return ListUtils.EMPTY_LIST;
        }
        List<Plan> toPromotePlans = new ArrayList<>();
        children.forEach(child -> {
            boolean isChildExpired = isExpired.apply(child);
            if (!isChildExpired && isRootExpired) {
                toPromotePlans.add(child);
            }
            runNode(child, childrenMap, deletePlanIds)
                    .forEach(plan -> {
                        if (isRootExpired) {
                            toPromotePlans.add(plan);
                        } else {
                            plan.setParentId(root.getId());
                            plan.setAncestorId(root.getAncestorId() > 0 ? root.getAncestorId() : root.getId());
                            planMapper.updateByPrimaryKey(plan);
                        }
                    });
        });
        return toPromotePlans;
    }
}
