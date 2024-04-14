package com.ezone.ezproject.modules.plan.bean;

import com.ezone.ezproject.dal.entity.Plan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlansAndProgresses {
    private List<Plan> plans;
    private List<Progress> progresses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Progress {
        private Number total;
        private Number end;

        public void include(Progress progress) {
            this.total = this.total.doubleValue() + progress.total.doubleValue();
            this.end = this.end.doubleValue() + progress.end.doubleValue();
        }
    }

    public PlansAndProgresses includeDescendantProgress() {
        if (CollectionUtils.isEmpty(this.plans)) {
            return this;
        }
        Map<Long, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < this.plans.size(); i++) {
            indexMap.put(this.plans.get(i).getId(), i);
        }
        Map<Long, List<Plan>> childrenMap = plans.stream().collect(Collectors.groupingBy(Plan::getParentId));
        include(childrenMap.get(0L), indexMap, childrenMap);
        return this;
    }

    private void include(List<Plan> roots, Map<Long, Integer> indexMap, Map<Long, List<Plan>> childrenMap) {
        if (CollectionUtils.isEmpty(roots)) {
            return;
        }
        roots.forEach(root -> {
            List<Plan> children = childrenMap.get(root.getId());
            if (CollectionUtils.isNotEmpty(children)) {
                include(children, indexMap, childrenMap);
                Progress progress = this.progresses.get(indexMap.get(root.getId()));
                children.forEach(child -> {
                    progress.include(this.progresses.get(indexMap.get(child.getId())));
                });
            }
        });
    }
}
