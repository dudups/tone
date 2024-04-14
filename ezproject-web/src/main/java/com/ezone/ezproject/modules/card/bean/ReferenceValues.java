package com.ezone.ezproject.modules.card.bean;

import com.ezone.devops.ezcode.sdk.bean.model.InternalRepo;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.StoryMapNode;
import com.ezone.ezproject.es.entity.ProjectAlarmExt;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceValues {
    private Map<Long, Plan> plans;
    private Map<Long, CardBean> parents;
    private Map<Long, StoryMapNode> storyMapNodes;
    private Map<Long, Project> projects;
    private Map<Long, ProjectCardSchema> projectSchemas;
    private Map<Long, InternalRepo> repos;
    private List<ProjectAlarmExt> alarms;
}
