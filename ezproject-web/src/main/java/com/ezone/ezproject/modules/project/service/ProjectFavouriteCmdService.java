package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.ProjectFavourite;
import com.ezone.ezproject.dal.entity.ProjectFavouriteExample;
import com.ezone.ezproject.dal.mapper.ProjectFavouriteMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
@AllArgsConstructor
public class ProjectFavouriteCmdService {
    private ProjectFavouriteMapper projectFavouriteMapper;
    private ProjectFavouriteQueryService projectFavouriteQueryService;
    private ProjectQueryService projectQueryService;

    public void favouriteProject(String user, Long projectId) {
        Project project = projectQueryService.select(projectId);
        if (projectFavouriteQueryService.exist(user, projectId)) {
            return;
        }
        projectFavouriteMapper.insert(ProjectFavourite.builder()
                .id(IdUtil.generateId())
                .user(user)
                .time(new Date())
                .companyId(project.getCompanyId())
                .projectId(projectId)
                .build());
    }

    public void unFavouriteProject(String user, Long projectId) {
        ProjectFavouriteExample example = new ProjectFavouriteExample();
        example.createCriteria().andUserEqualTo(user).andProjectIdEqualTo(projectId);
        projectFavouriteMapper.deleteByExample(example);
    }

    public void deleteByProjectId(Long projectId) {
        ProjectFavouriteExample example = new ProjectFavouriteExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        projectFavouriteMapper.deleteByExample(example);
    }

}
