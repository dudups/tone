package com.ezone.ezproject.modules.project.bean;

import com.ezone.devops.ezcode.sdk.bean.model.InternalRepo;
import com.ezone.ezproject.dal.entity.ProjectRepo;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ProjectRepoBean {
    private ProjectRepo projectRepo;
    private InternalRepo repo;
}
