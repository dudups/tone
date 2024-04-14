package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.dal.entity.ProjectWikiSpace;
import com.ezone.ezproject.dal.entity.ProjectWikiSpaceExample;
import com.ezone.ezproject.dal.mapper.ProjectWikiSpaceMapper;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.cli.EzWikiCliService;
import com.ezone.ezproject.modules.project.bean.RelatesBean;
import com.ezone.ezproject.modules.project.bean.RelatesProjectsBean;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectWikiSpaceService {
    private ProjectQueryService projectQueryService;

    private ProjectWikiSpaceMapper projectSpaceMapper;

    private UserService userService;

    private EzWikiCliService ezWikiCliService;

    public ProjectWikiSpace bind(String user, Long projectId, Long spaceId, boolean checkSpace) {
        ProjectWikiSpace projectSpace = select(projectId, spaceId);
        if (projectSpace != null) {
            return projectSpace;
        }
        Long companyId = projectQueryService.getProjectCompany(projectId);
        if (checkSpace) {
            ezWikiCliService.checkedSpace(companyId, user, spaceId);
        }
        projectSpace = ProjectWikiSpace.builder()
                .id(IdUtil.generateId())
                .companyId(companyId)
                .projectId(projectId)
                .spaceId(spaceId)
                .createTime(new Date())
                .createUser(user)
                .build();
        projectSpaceMapper.insert(projectSpace);
        return projectSpace;
    }

    public ProjectWikiSpace bind(Long projectId, Long spaceId) {
        return bind(userService.currentUserName(), projectId, spaceId, true);
    }

    public List<ProjectWikiSpace> selectByProjectId(Long projectId) {
        ProjectWikiSpaceExample example = new ProjectWikiSpaceExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        return projectSpaceMapper.selectByExample(example);
    }

    public List<ProjectWikiSpace> selectByProjectIds(List<Long> projectIds) {
        ProjectWikiSpaceExample example = new ProjectWikiSpaceExample();
        example.createCriteria().andProjectIdIn(projectIds);
        return projectSpaceMapper.selectByExample(example);
    }

    public List<ProjectWikiSpace> selectByCompanyId(Long companyId) {
        ProjectWikiSpaceExample example = new ProjectWikiSpaceExample();
        example.createCriteria().andCompanyIdEqualTo(companyId);
        return projectSpaceMapper.selectByExample(example);
    }


    public RelatesProjectsBean<ProjectWikiSpace> selectProjectBySpaceId(Long spaceId) {
        ProjectWikiSpaceExample example = new ProjectWikiSpaceExample();
        example.createCriteria().andSpaceIdEqualTo(spaceId);
        List<ProjectWikiSpace> relates = projectSpaceMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(relates)) {
            return RelatesProjectsBean.<ProjectWikiSpace>builder()
                    .relates(ListUtils.EMPTY_LIST)
                    .build();
        }
        return RelatesProjectsBean.<ProjectWikiSpace>builder()
                .relates(relates)
                .projects(projectQueryService.select(relates.stream().map(ProjectWikiSpace::getProjectId).collect(Collectors.toList())))
                .build();
    }

    public RelatesBean<ProjectWikiSpace> selectRelatesBean(Long projectId) {
        ProjectWikiSpaceExample example = new ProjectWikiSpaceExample();
        List<ProjectWikiSpace> relates = selectByProjectId(projectId);
        if (CollectionUtils.isEmpty(relates)) {
            return RelatesBean.<ProjectWikiSpace>builder()
                    .relates(ListUtils.EMPTY_LIST)
                    .build();
        }
        return RelatesBean.<ProjectWikiSpace>builder()
                .relates(relates)
                .refs(ezWikiCliService.getSpaces(relates.stream().map(ProjectWikiSpace::getSpaceId).collect(Collectors.toList())))
                .build();
    }

    public ProjectWikiSpace select(Long projectId, Long spaceId) {
        ProjectWikiSpaceExample example = new ProjectWikiSpaceExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andSpaceIdEqualTo(spaceId);
        List<ProjectWikiSpace> rels = projectSpaceMapper.selectByExample(example);
        return CollectionUtils.isEmpty(rels) ? null : rels.get(0);
    }

    public void unBind(Long projectId, Long spaceId) {
        ProjectWikiSpaceExample example = new ProjectWikiSpaceExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andSpaceIdEqualTo(spaceId);
        projectSpaceMapper.deleteByExample(example);
    }

    public void delete(Long id) {
        projectSpaceMapper.deleteByPrimaryKey(id);
    }

    public void deleteByProjectId(Long projectId) {
        ProjectWikiSpaceExample example = new ProjectWikiSpaceExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        projectSpaceMapper.deleteByExample(example);
    }

}
