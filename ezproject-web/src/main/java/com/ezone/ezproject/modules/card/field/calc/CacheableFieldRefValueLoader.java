package com.ezone.ezproject.modules.card.field.calc;

import com.ezone.ezproject.common.function.CacheableFunction;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.StoryMap;
import com.ezone.ezproject.dal.entity.StoryMapNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheableFieldRefValueLoader implements FieldRefValueLoader {
    private CacheableFunction<Long, Project> loadProject;
    private CacheableFunction<Long, Plan> loadPlan;
    private CacheableFunction<Long, StoryMap> loadStoryMap;
    private CacheableFunction<Long, StoryMapNode> loadStoryMapNode;

    @Override
    public Project project(Long projectId) {
        return loadProject.apply(projectId);
    }

    @Override
    public Plan plan(Long planId) {
        return loadPlan.apply(planId);
    }

    @Override
    public StoryMap storyMap(Long storyMapId) {
        return loadStoryMap.apply(storyMapId);
    }

    @Override
    public StoryMapNode storyMapNode(Long storyMapNodeId) {
        return loadStoryMapNode.apply(storyMapNodeId);
    }

    public void cacheProject(Project project) {
        if (project == null) {
            return;
        }
        loadProject.cache(project.getId(), project);
    }

    public void cachePlan(Plan plan) {
        if (plan == null) {
            return;
        }
        loadPlan.cache(plan.getId(), plan);
    }
}
