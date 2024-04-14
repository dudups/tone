package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.dal.entity.ProjectHostGroup;
import com.ezone.ezproject.dal.entity.ProjectHostGroupExample;
import com.ezone.ezproject.dal.entity.ProjectWikiSpaceExample;
import com.ezone.ezproject.dal.mapper.ProjectHostGroupMapper;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.cli.EzDeployCliService;
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
public class ProjectResourceService {
    private ProjectQueryService projectQueryService;

    private ProjectHostGroupMapper projectHostGroupMapper;

    private UserService userService;

    private CompanyService companyService;

    private EzDeployCliService ezWikiCliService;

    public ProjectHostGroup bind(String user, Long projectId, Long groupId, boolean checkGroup) {
        ProjectHostGroup projectHostGroup = select(projectId, groupId);
        if (projectHostGroup != null) {
            return projectHostGroup;
        }
        Long companyId = projectQueryService.getProjectCompany(projectId);
        if (checkGroup) {
            ezWikiCliService.checkedHostGroup(companyId, user, groupId);
        }
        projectHostGroup = ProjectHostGroup.builder()
                .id(IdUtil.generateId())
                .companyId(projectQueryService.getProjectCompany(projectId))
                .projectId(projectId)
                .groupId(groupId)
                .createTime(new Date())
                .createUser(user)
                .build();
        projectHostGroupMapper.insert(projectHostGroup);
        return projectHostGroup;
    }

    public List<ProjectHostGroup> selectByProjectId(Long projectId) {
        ProjectHostGroupExample example = new ProjectHostGroupExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        return projectHostGroupMapper.selectByExample(example);
    }

    public RelatesProjectsBean<ProjectHostGroup> selectProjectByHostGroupId(Long groupId) {
        ProjectHostGroupExample example = new ProjectHostGroupExample();
        example.createCriteria().andGroupIdEqualTo(groupId);
        List<ProjectHostGroup> relates = projectHostGroupMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(relates)) {
            return RelatesProjectsBean.<ProjectHostGroup>builder()
                    .relates(ListUtils.EMPTY_LIST)
                    .build();
        }
        return RelatesProjectsBean.<ProjectHostGroup>builder()
                .relates(relates)
                .projects(projectQueryService.select(relates.stream().map(ProjectHostGroup::getProjectId).collect(Collectors.toList())))
                .build();
    }

    public RelatesBean<ProjectHostGroup> selectRelatesBean(Long projectId) {
        ProjectWikiSpaceExample example = new ProjectWikiSpaceExample();
        List<ProjectHostGroup> relates = selectByProjectId(projectId);
        if (CollectionUtils.isEmpty(relates)) {
            return RelatesBean.<ProjectHostGroup>builder()
                    .relates(ListUtils.EMPTY_LIST)
                    .build();
        }
        return RelatesBean.<ProjectHostGroup>builder()
                .relates(relates)
                .refs(ezWikiCliService.getHostGroups(
                        projectQueryService.getProjectCompany(projectId),
                        relates.stream().map(ProjectHostGroup::getGroupId).collect(Collectors.toList()))
                )
                .build();
    }

    public ProjectHostGroup select(Long projectId, Long groupId) {
        ProjectHostGroupExample example = new ProjectHostGroupExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andGroupIdEqualTo(groupId);
        List<ProjectHostGroup> rels = projectHostGroupMapper.selectByExample(example);
        return CollectionUtils.isEmpty(rels) ? null : rels.get(0);
    }

    public void unBind(Long projectId, Long groupId) {
        ProjectHostGroupExample example = new ProjectHostGroupExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andGroupIdEqualTo(groupId);
        projectHostGroupMapper.deleteByExample(example);
    }

    public void delete(Long id) {
        projectHostGroupMapper.deleteByPrimaryKey(id);
    }

    public void deleteByProjectId(Long projectId) {
        ProjectHostGroupExample example = new ProjectHostGroupExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        projectHostGroupMapper.deleteByExample(example);
    }

}
