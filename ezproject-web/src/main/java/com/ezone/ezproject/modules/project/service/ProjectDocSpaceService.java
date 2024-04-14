package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.dal.entity.ProjectDocSpace;
import com.ezone.ezproject.dal.entity.ProjectDocSpaceExample;
import com.ezone.ezproject.dal.mapper.ProjectDocSpaceMapper;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.cli.EzDocCliService;
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
public class ProjectDocSpaceService {
    private ProjectQueryService projectQueryService;

    private ProjectDocSpaceMapper projectSpaceMapper;

    private UserService userService;

    private CompanyService companyService;

    private EzDocCliService ezDocCliService;

    public ProjectDocSpace bind(String user, Long projectId, Long spaceId, boolean checkSpace) {
        ProjectDocSpace projectSpace = select(projectId, spaceId);
        if (projectSpace != null) {
            return projectSpace;
        }
        Long companyId = projectQueryService.getProjectCompany(projectId);
        if (checkSpace) {
            ezDocCliService.checkSpace(companyId, user, spaceId);
        }
        projectSpace = ProjectDocSpace.builder()
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

    public ProjectDocSpace bind(Long projectId, Long spaceId) {
        return bind(userService.currentUserName(), projectId, spaceId, true);
    }

    public List<ProjectDocSpace> selectByProjectId(Long projectId) {
        ProjectDocSpaceExample example = new ProjectDocSpaceExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        return projectSpaceMapper.selectByExample(example);
    }

    public RelatesProjectsBean<ProjectDocSpace> selectProjectBySpaceId(Long spaceId) {
        ProjectDocSpaceExample example = new ProjectDocSpaceExample();
        example.createCriteria().andSpaceIdEqualTo(spaceId);
        List<ProjectDocSpace> relates = projectSpaceMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(relates)) {
            return RelatesProjectsBean.<ProjectDocSpace>builder()
                    .relates(ListUtils.EMPTY_LIST)
                    .build();
        }
        return RelatesProjectsBean.<ProjectDocSpace>builder()
                .relates(relates)
                .projects(projectQueryService.select(relates.stream().map(ProjectDocSpace::getProjectId).collect(Collectors.toList())))
                .build();
    }

    public RelatesBean<ProjectDocSpace> selectRelatesBean(Long projectId) {
        ProjectDocSpaceExample example = new ProjectDocSpaceExample();
        List<ProjectDocSpace> relates = selectByProjectId(projectId);
        if (CollectionUtils.isEmpty(relates)) {
            return RelatesBean.<ProjectDocSpace>builder()
                    .relates(ListUtils.EMPTY_LIST)
                    .build();
        }
        return RelatesBean.<ProjectDocSpace>builder()
                .relates(relates)
                .refs(ezDocCliService.getSpaces(relates.get(0).getCompanyId(), relates.stream().map(ProjectDocSpace::getSpaceId).collect(Collectors.toList())))
                .build();
    }

    public ProjectDocSpace select(Long projectId, Long spaceId) {
        ProjectDocSpaceExample example = new ProjectDocSpaceExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andSpaceIdEqualTo(spaceId);
        List<ProjectDocSpace> rels = projectSpaceMapper.selectByExample(example);
        return CollectionUtils.isEmpty(rels) ? null : rels.get(0);
    }

    public void unBind(Long projectId, Long spaceId) {
        ProjectDocSpaceExample example = new ProjectDocSpaceExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andSpaceIdEqualTo(spaceId);
        projectSpaceMapper.deleteByExample(example);
    }

    public void delete(Long id) {
        projectSpaceMapper.deleteByPrimaryKey(id);
    }

    public void deleteByProjectId(Long projectId) {
        ProjectDocSpaceExample example = new ProjectDocSpaceExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        projectSpaceMapper.deleteByExample(example);
    }

}
