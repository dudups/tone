package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.dal.entity.ProjectArtifactRepo;
import com.ezone.ezproject.dal.entity.ProjectArtifactRepoExample;
import com.ezone.ezproject.dal.mapper.ProjectArtifactRepoMapper;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.cli.EzPkgCliService;
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
public class ProjectArtifactRepoService {
    private ProjectQueryService projectQueryService;

    private ProjectArtifactRepoMapper artifactRepoMapper;

    private UserService userService;

    private CompanyService companyService;

    private EzPkgCliService ezPkgCliService;

    public ProjectArtifactRepo bind(String user, Long projectId, Long repoId, boolean checkRepo) {
        ProjectArtifactRepo artifactRepo = select(projectId, repoId);
        if (artifactRepo != null) {
            return artifactRepo;
        }
        Long companyId = projectQueryService.getProjectCompany(projectId);
        if (checkRepo) {
            ezPkgCliService.checkRepo(companyId, user, repoId);
        }
        artifactRepo = ProjectArtifactRepo.builder()
                .id(IdUtil.generateId())
                .companyId(projectQueryService.getProjectCompany(projectId))
                .projectId(projectId)
                .repoId(repoId)
                .createTime(new Date())
                .createUser(user)
                .build();
        artifactRepoMapper.insert(artifactRepo);
        return artifactRepo;
    }

    public List<ProjectArtifactRepo> selectByProjectId(Long projectId) {
        ProjectArtifactRepoExample example = new ProjectArtifactRepoExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        return artifactRepoMapper.selectByExample(example);
    }

    public RelatesProjectsBean<ProjectArtifactRepo> selectProjectByRepoId(Long repoId) {
        ProjectArtifactRepoExample example = new ProjectArtifactRepoExample();
        example.createCriteria().andRepoIdEqualTo(repoId);
        List<ProjectArtifactRepo> relates = artifactRepoMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(relates)) {
            return RelatesProjectsBean.<ProjectArtifactRepo>builder()
                    .relates(ListUtils.EMPTY_LIST)
                    .build();
        }
        return RelatesProjectsBean.<ProjectArtifactRepo>builder()
                .relates(relates)
                .projects(projectQueryService.select(relates.stream().map(ProjectArtifactRepo::getProjectId).collect(Collectors.toList())))
                .build();
    }

    public RelatesBean<ProjectArtifactRepo> selectRelatesBean(Long projectId) {
        ProjectArtifactRepoExample example = new ProjectArtifactRepoExample();
        List<ProjectArtifactRepo> relates = selectByProjectId(projectId);
        if (CollectionUtils.isEmpty(relates)) {
            return RelatesBean.<ProjectArtifactRepo>builder()
                    .relates(ListUtils.EMPTY_LIST)
                    .build();
        }
        return RelatesBean.<ProjectArtifactRepo>builder()
                .relates(relates)
                .refs(ezPkgCliService.getRepos(relates.stream().map(ProjectArtifactRepo::getRepoId).collect(Collectors.toList())))
                .build();
    }

    public ProjectArtifactRepo select(Long projectId, Long repoId) {
        ProjectArtifactRepoExample example = new ProjectArtifactRepoExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andRepoIdEqualTo(repoId);
        List<ProjectArtifactRepo> rels = artifactRepoMapper.selectByExample(example);
        return CollectionUtils.isEmpty(rels) ? null : rels.get(0);
    }

    public void unBind(Long projectId, Long repoId) {
        ProjectArtifactRepoExample example = new ProjectArtifactRepoExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andRepoIdEqualTo(repoId);
        artifactRepoMapper.deleteByExample(example);
    }

    public void delete(Long id) {
        artifactRepoMapper.deleteByPrimaryKey(id);
    }

    public void deleteByProjectId(Long projectId) {
        ProjectArtifactRepoExample example = new ProjectArtifactRepoExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        artifactRepoMapper.deleteByExample(example);
    }

}
