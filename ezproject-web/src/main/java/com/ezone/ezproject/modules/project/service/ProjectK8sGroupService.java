package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.ProjectK8sGroup;
import com.ezone.ezproject.dal.entity.ProjectK8sGroupExample;
import com.ezone.ezproject.dal.mapper.ProjectK8sGroupMapper;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.cli.EzK8sCliService;
import com.ezone.ezproject.modules.project.bean.RelatesBean;
import com.ezone.ezproject.modules.project.bean.RelatesProjectsBean;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.collect;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectK8sGroupService {
    private ProjectQueryService projectQueryService;

    private ProjectK8sGroupMapper projectK8sGroupMapper;

    private UserService userService;

    private EzK8sCliService ezK8sCliService;

    public ProjectK8sGroup bind(String user, Long projectId, Long k8sGroupId) {
        ProjectK8sGroup projectK8sGroup = select(projectId, k8sGroupId);
        if (projectK8sGroup != null) {
            return projectK8sGroup;
        }
        Long companyId = projectQueryService.getProjectCompany(projectId);
        projectK8sGroup = ProjectK8sGroup.builder()
                .id(IdUtil.generateId())
                .companyId(companyId)
                .projectId(projectId)
                .k8sGroupId(k8sGroupId)
                .createTime(new Date())
                .createUser(user)
                .build();
        projectK8sGroupMapper.insert(projectK8sGroup);
        return projectK8sGroup;
    }

    public ProjectK8sGroup bind(Long projectId, Long k8sGroupId) {
        return bind(userService.currentUserName(), projectId, k8sGroupId);
    }

    public List<ProjectK8sGroup> selectByProjectId(Long projectId) {
        ProjectK8sGroupExample example = new ProjectK8sGroupExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        return projectK8sGroupMapper.selectByExample(example);
    }

    public RelatesBean<ProjectK8sGroup> selectProjectByK8sGroupId(Long spaceId) {
        ProjectK8sGroupExample example = new ProjectK8sGroupExample();
        example.createCriteria().andK8sGroupIdEqualTo(spaceId);
        List<ProjectK8sGroup> relates = projectK8sGroupMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(relates)) {
            return RelatesBean.<ProjectK8sGroup>builder()
                    .relates(ListUtils.EMPTY_LIST)
                    .build();
        }
        return RelatesBean.<ProjectK8sGroup>builder()
                .relates(relates)
                .refs(projectQueryService.select(relates.stream().map(ProjectK8sGroup::getProjectId).collect(Collectors.toList())))
                .build();
    }

    public RelatesBean<ProjectK8sGroup> selectRelatesBean(Long projectId) {
        List<ProjectK8sGroup> relates = selectByProjectId(projectId);
        if (CollectionUtils.isEmpty(relates)) {
            return RelatesBean.<ProjectK8sGroup>builder()
                    .relates(ListUtils.EMPTY_LIST)
                    .build();
        }
        return RelatesBean.<ProjectK8sGroup>builder()
                .relates(relates)
                .refs(ezK8sCliService.getSpaces(relates.stream().map(ProjectK8sGroup::getK8sGroupId).collect(Collectors.toList())))
                .build();
    }

    public ProjectK8sGroup select(Long projectId, Long spaceId) {
        ProjectK8sGroupExample example = new ProjectK8sGroupExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andK8sGroupIdEqualTo(spaceId);
        List<ProjectK8sGroup> rels = projectK8sGroupMapper.selectByExample(example);
        return CollectionUtils.isEmpty(rels) ? null : rels.get(0);
    }

    public void unBind(Long projectId, Long spaceId) {
        ProjectK8sGroupExample example = new ProjectK8sGroupExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andK8sGroupIdEqualTo(spaceId);
        projectK8sGroupMapper.deleteByExample(example);
    }

    public void delete(Long id) {
        projectK8sGroupMapper.deleteByPrimaryKey(id);
    }

    public void deleteByProjectId(Long projectId) {
        ProjectK8sGroupExample example = new ProjectK8sGroupExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        projectK8sGroupMapper.deleteByExample(example);
    }

}
