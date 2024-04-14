package com.ezone.ezproject.modules.project.service;

import com.ezone.devops.ezcode.base.enums.ResourceType;
import com.ezone.devops.ezcode.sdk.bean.model.InternalRepo;
import com.ezone.devops.ezcode.sdk.service.InternalRepoService;
import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.ProjectRepo;
import com.ezone.ezproject.dal.entity.ProjectRepoExample;
import com.ezone.ezproject.dal.mapper.ProjectRepoMapper;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.project.bean.ProjectRepoBean;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectRepoService {
    private ProjectQueryService projectQueryService;

    private ProjectRepoMapper projectRepoMapper;

    private UserService userService;

    private CompanyService companyService;

    private InternalRepoService repoService;

    public ProjectRepo bind(String user, Long projectId, Long repoId) {
        ProjectRepo projectRepo = select(projectId, repoId);
        if (projectRepo != null) {
            return projectRepo;
        }
        projectRepo = ProjectRepo.builder()
                .id(IdUtil.generateId())
                .companyId(projectQueryService.getProjectCompany(projectId))
                .projectId(projectId)
                .repoId(repoId)
                .createTime(new Date())
                .createUser(user)
                .build();
        projectRepoMapper.insert(projectRepo);
        return projectRepo;
    }

    public List<ProjectRepoBean> bindWithCheck(Long projectId, Long resourceId, ResourceType resourceType, boolean recursion) {
        Long companyId = companyService.currentCompany();
        String user = userService.currentUserName();
        List<InternalRepo> repos = repoService.filterProjectBindableRepos(companyId, resourceType, resourceId, user, recursion);
        if (CollectionUtils.isEmpty(repos)) {
            return ListUtils.EMPTY_LIST;
        }
        List<Long> fromRepos = selectByProjectId(projectId).stream().map(ProjectRepo::getRepoId).collect(Collectors.toList());
        Date now = new Date();
        return repos.stream()
                .filter(repo -> !fromRepos.contains(repo.getId()))
                .map(repo -> {
                    ProjectRepo projectRepo = ProjectRepo.builder()
                            .id(IdUtil.generateId())
                            .companyId(companyId)
                            .projectId(projectId)
                            .repoId(repo.getId())
                            .createTime(now)
                            .createUser(user)
                            .build();
                    projectRepoMapper.insert(projectRepo);
                    return ProjectRepoBean.builder().projectRepo(projectRepo).repo(repo).build();
                })
                .collect(Collectors.toList());
    }

    public List<ProjectRepo> selectByProjectId(Long projectId) {
        ProjectRepoExample example = new ProjectRepoExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        return projectRepoMapper.selectByExample(example);
    }

    public List<ProjectRepo> selectByProjectIds(List<Long> projectIds) {
        ProjectRepoExample example = new ProjectRepoExample();
        example.createCriteria().andProjectIdIn(projectIds);
        return projectRepoMapper.selectByExample(example);
    }

    public List<ProjectRepo> selectByCompanyId(Long companyId) {
        ProjectRepoExample example = new ProjectRepoExample();
        example.createCriteria().andCompanyIdEqualTo(companyId);
        return projectRepoMapper.selectByExample(example);
    }

    public List<ProjectRepoBean> selectBeanByProjectId(Long projectId) {
        List<ProjectRepo> projectRepos = selectByProjectId(projectId);
        if (CollectionUtils.isEmpty(projectRepos)) {
            return ListUtils.EMPTY_LIST;
        }
        Map<Long, InternalRepo> repos = repoService
                .listReposByIds(
                        projectQueryService.getProjectCompany(projectId),
                        projectRepos.stream().map(ProjectRepo::getRepoId).collect(Collectors.toList())
                )
                .stream()
                .collect(Collectors.toMap(InternalRepo::getId, r -> r));
        return projectRepos.stream()
                .map(projectRepo -> ProjectRepoBean.builder()
                        .projectRepo(projectRepo)
                        .repo(repos.get(projectRepo.getRepoId())).build())
                .collect(Collectors.toList());
    }

    public List<ProjectRepo> selectByRepo(Long companyId, Long repoId) {
        ProjectRepoExample example = new ProjectRepoExample();
        example.createCriteria().andCompanyIdEqualTo(companyId).andRepoIdEqualTo(repoId);
        return projectRepoMapper.selectByExample(example);
    }

    public List<Project> selectProjectByRepo(Long companyId, Long repoId) {
        List<ProjectRepo> rels = selectByRepo(companyId, repoId);
        if (CollectionUtils.isEmpty(rels)) {
            return ListUtils.EMPTY_LIST;
        }
        return projectQueryService.select(rels.stream().map(ProjectRepo::getProjectId).collect(Collectors.toList()));
    }

    public Project selectBindProject(Long projectId, Long repoId) {
        ProjectRepoExample example = new ProjectRepoExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andRepoIdEqualTo(repoId);
        List<ProjectRepo> rels = projectRepoMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(rels)) {
            throw CodedException.NOT_FOUND;
        }
        return projectQueryService.select(projectId);
    }

    @Deprecated
    public List<Project> selectProjectByRepo(Long companyId, String repo) {
        InternalRepo internalRepo = repoService.getRepoByFullName(companyId, repo);
        if (null == internalRepo) {
            throw new CodedException(HttpStatus.NOT_FOUND, "repo not exist!");
        }
        List<ProjectRepo> rels = selectByRepo(companyId, internalRepo.getId());
        if (CollectionUtils.isEmpty(rels)) {
            return ListUtils.EMPTY_LIST;
        }
        return projectQueryService.select(rels.stream().map(ProjectRepo::getProjectId).collect(Collectors.toList()));
    }

    public ProjectRepo select(Long projectId, Long repoId) {
        ProjectRepoExample example = new ProjectRepoExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andRepoIdEqualTo(repoId);
        List<ProjectRepo> rels = projectRepoMapper.selectByExample(example);
        return CollectionUtils.isEmpty(rels) ? null : rels.get(0);
    }

    public void checkIsBindRepo(Long projectId, Long repoId) {
        if (select(projectId, repoId) == null) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "卡片所在项目未关联该代码库！");
        }
    }

    @Deprecated
    public void checkIsBindRepo(Long projectId, String repo) {
        InternalRepo internalRepo = repoService.getRepoByFullName(projectQueryService.getProjectCompany(projectId), repo);
        if (null == internalRepo) {
            throw new CodedException(HttpStatus.NOT_FOUND, "repo not exist!");
        }
        if (select(projectId, internalRepo.getId()) == null) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "卡片所在项目未关联该代码库！");
        }
    }

    public void delete(Long id) {
        projectRepoMapper.deleteByPrimaryKey(id);
    }

    public void unBind(Long projectId, Long repoId) {
        ProjectRepoExample example = new ProjectRepoExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andRepoIdEqualTo(repoId);
        projectRepoMapper.deleteByExample(example);
    }

    public void unBindByRepo(Long repoId, Long companyId) {
        ProjectRepoExample example = new ProjectRepoExample();
        example.createCriteria().andCompanyIdEqualTo(companyId).andRepoIdEqualTo(repoId);
        projectRepoMapper.deleteByExample(example);
    }

    public void deleteByRepoId(Long repoId) {
        ProjectRepoExample example = new ProjectRepoExample();
        example.createCriteria().andRepoIdEqualTo(repoId);
        projectRepoMapper.deleteByExample(example);
    }

    public void deleteByProjectId(Long projectId) {
        ProjectRepoExample example = new ProjectRepoExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        projectRepoMapper.deleteByExample(example);
    }
}
