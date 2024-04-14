package com.ezone.ezproject.modules.card.field.calc;

import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.StoryMap;
import com.ezone.ezproject.dal.entity.StoryMapNode;

public interface CardFieldValueRef {
    Project project(Long projectId);
    Plan plan(Long planId);
    StoryMap storyMap(Long storyMapId);
    StoryMapNode storyMapNode(Long storyMapNodeId);
}
