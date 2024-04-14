package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.dal.entity.ProjectFavourite;
import com.ezone.ezproject.dal.entity.ProjectFavouriteExample;
import com.ezone.ezproject.dal.mapper.ProjectFavouriteMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectFavouriteQueryService {
    private ProjectFavouriteMapper projectFavouriteMapper;

    public @NotNull List<Long> selectFavouriteProjectIds(Long company, String user) {
        ProjectFavouriteExample example = new ProjectFavouriteExample();
        example.createCriteria().andCompanyIdEqualTo(company).andUserEqualTo(user);
        List<ProjectFavourite> favourites = projectFavouriteMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(favourites)) {
            return ListUtils.EMPTY_LIST;
        }
        return favourites.stream().map(ProjectFavourite::getProjectId).collect(Collectors.toList());
    }

    public boolean exist(String user, Long projectId) {
        ProjectFavouriteExample example = new ProjectFavouriteExample();
        example.createCriteria().andUserEqualTo(user).andProjectIdEqualTo(projectId);
        return projectFavouriteMapper.countByExample(example) > 0;
    }
}
