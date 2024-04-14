package com.ezone.ezproject.modules.project.controller;

import com.ezone.ezbase.iam.bean.BaseGroupUser;
import com.ezone.ezbase.iam.bean.enums.GroupUserType;
import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.ProjectArtifactRepo;
import com.ezone.ezproject.dal.entity.ProjectDocSpace;
import com.ezone.ezproject.dal.entity.ProjectHostGroup;
import com.ezone.ezproject.dal.entity.ProjectK8sGroup;
import com.ezone.ezproject.dal.entity.ProjectMember;
import com.ezone.ezproject.dal.entity.ProjectRepo;
import com.ezone.ezproject.dal.entity.ProjectWikiSpace;
import com.ezone.ezproject.es.entity.ProjectRole;
import com.ezone.ezproject.es.entity.ProjectRoleSchema;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.modules.common.InternalApiAuthentication;
import com.ezone.ezproject.modules.project.bean.ProjectExt;
import com.ezone.ezproject.modules.project.bean.RelatesBean;
import com.ezone.ezproject.modules.project.bean.RelatesProjectsBean;
import com.ezone.ezproject.modules.project.bean.SearchScope;
import com.ezone.ezproject.modules.project.service.ProjectArtifactRepoService;
import com.ezone.ezproject.modules.project.service.ProjectDocSpaceService;
import com.ezone.ezproject.modules.project.service.ProjectK8sGroupService;
import com.ezone.ezproject.modules.project.service.ProjectMemberQueryService;
import com.ezone.ezproject.modules.project.service.ProjectRepoService;
import com.ezone.ezproject.modules.project.service.ProjectResourceService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import com.ezone.ezproject.modules.project.service.ProjectWikiSpaceService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import com.ezone.galaxy.framework.common.bean.PageResult;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@ApiOperation("项目")
@RestController
@RequestMapping("/project/api/project")
@Slf4j
@AllArgsConstructor
public class ProjectApiController extends AbstractController {
    private ProjectRepoService projectRepoService;
    private ProjectWikiSpaceService projectWikiSpaceService;
    private ProjectResourceService projectResourceService;
    private ProjectArtifactRepoService projectArtifactRepoService;
    private ProjectDocSpaceService projectDocSpaceService;
    private ProjectK8sGroupService projectK8sGroupService;

    private ProjectSchemaQueryService projectSchemaQueryService;
    private ProjectMemberQueryService projectMemberQueryService;

    @ApiOperation("检查权限")
    @GetMapping("{id:[0-9]+}/checkRead")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<Boolean> checkRead(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                           @ApiParam(value = "用户名", example = "1") @RequestParam String user) {
        return success(permissionService.isRole(user, id));
    }

    @ApiOperation("检查权限")
    @GetMapping("{id:[0-9]+}/checkWrite")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<Boolean> checkWrite(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                            @ApiParam(value = "用户名", example = "1") @RequestParam String user) {
        return success(permissionService.isRole(user, id));
    }

    @ApiOperation("检查权限")
    @PostMapping("/checkRead")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<Boolean> checkReadProjects(@ApiParam(value = "项目ID", example = "1,2") @RequestBody List<Long> projectIds,
                                                   @ApiParam(value = "用户名", example = "1") @RequestParam String user) {
        return success(permissionService.isRole(user, projectIds));
    }

    @ApiOperation("检查权限")
    @PostMapping("checkWrite")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<Boolean> checkWriteProjects(@ApiParam(value = "项目ID", example = "1") @RequestBody List<Long> projectIds,
                                                    @ApiParam(value = "用户名", example = "1") @RequestParam String user) {
        return success(permissionService.isRole(user, projectIds));
    }

    @ApiOperation("去掉无读权限的项目")
    @PostMapping("/filterRead")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<Collection<Long>> filterRead(@ApiParam(value = "项目ID", example = "1,2") @RequestBody List<Long> projectIds,
                                                     @ApiParam(value = "用户名", example = "1") @RequestParam String user,
                                                     @ApiParam(value = "企业ID", example = "1") @RequestParam Long companyId) {
        return success(projectQueryService.filterRead(user, projectIds, companyId));
    }

    @ApiOperation("查询项目")
    @GetMapping("{id:[0-9]+}")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<Project> getProject(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) {
        return success(projectQueryService.select(id));
    }

    @ApiOperation("查询项目")
    @GetMapping
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<List<Project>> getProject(@ApiParam(value = "项目ID", example = "1") @RequestParam List<Long> ids) {
        return success(projectQueryService.select(ids));
    }

    @ApiOperation("查询项目总数")
    @GetMapping("count")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<Long> getProjectCount(@ApiParam(value = "公司ID", example = "1") @RequestParam(required = false) List<Long> companyId) {
        return success(projectQueryService.countByCompany(companyId));
    }

    @ApiOperation("查询公司下的项目列表")
    @GetMapping("searchAdmin")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<List<ProjectExt>> searchAdmin(@RequestParam Long companyId,
                                                   @RequestParam String user,
                                                   @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
                                                   @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                                   @ApiParam(value = "查询项目名过滤") @RequestParam(required = false) String q,
                                                   HttpServletResponse response) throws IOException {
        TotalBean<ProjectExt> totalBean = projectQueryService.search(companyId, user, SearchScope.ADMIN, q, pageNumber, pageSize);
        response.addHeader(PageResult.PAGE_TOTAL_COUNT, String.valueOf(totalBean.getTotal()));
        return success(totalBean.getList());
    }

    @ApiOperation("查询公司下的项目列表")
    @GetMapping("searchMember")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<List<ProjectExt>> searchMember(@RequestParam Long companyId,
                                                    @RequestParam String user,
                                                    @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
                                                    @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                                    @ApiParam(value = "查询项目名过滤") @RequestParam(required = false) String q,
                                                    HttpServletResponse response) throws IOException {
        TotalBean<ProjectExt> totalBean = projectQueryService.search(companyId, user, SearchScope.MEMBER, q, pageNumber, pageSize);
        response.addHeader(PageResult.PAGE_TOTAL_COUNT, String.valueOf(totalBean.getTotal()));
        return success(totalBean.getList());
    }

    @ApiOperation("查询项目关联的代码库")
    @PostMapping("projectsBindRepoIds")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<List<Long>> getRepoIdsByProjects(@RequestBody List<Long> projectIds) {
        if (CollectionUtils.isEmpty(projectIds)) {
            return success(ListUtils.EMPTY_LIST);
        }
        return success(projectRepoService.selectByProjectIds(projectIds).stream().map(ProjectRepo::getRepoId).distinct().collect(Collectors.toList()));
    }


    @ApiOperation("查询企业下所有项目关联的代码库")
    @PostMapping("companyBindRepoIds")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<List<Long>> getRepoIdsByCompany(@RequestParam Long companyId, @RequestParam() String user) {
        if (companyId == null) {
            return success(ListUtils.EMPTY_LIST);
        }
        return success(projectRepoService.selectByCompanyId(companyId).stream().map(ProjectRepo::getRepoId).distinct().collect(Collectors.toList()));
    }

    @ApiOperation("查询代码库关联的项目")
    @GetMapping("repoBindProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<List<Project>> getProjectByRepo(@ApiParam(value = "公司ID", example = "1") @RequestParam Long companyId,
                                                        @Deprecated @ApiParam(value = "代码库路径", example = "1") @RequestParam(required = false) String repo,
                                                        @RequestParam(required = false, defaultValue = "0") Long repoId) {
        if (repoId > 0) {
            return success(projectRepoService.selectProjectByRepo(companyId, repoId));
        } else {
            return success(projectRepoService.selectProjectByRepo(companyId, repo));
        }
    }

    @ApiOperation("查询代码库关联的项目")
    @GetMapping("repoCheckedBindProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<Project> getProjectByRepo(@RequestParam Long repoId, @RequestParam Long projectId) {
        return success(projectRepoService.selectBindProject(projectId, repoId));
    }

    @ApiOperation("代码库关联项目")
    @PutMapping("repoBindProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse repoBindProject(@RequestParam Long projectId,
                                        @Deprecated @RequestParam(required = false) String repo,
                                        @RequestParam(required = false, defaultValue = "0") Long repoId,
                                        @ApiParam(value = "用户名", example = "1") @RequestParam String user) {
        checkIsProjectMember(user, projectId);
        if (repoId > 0) {
            projectRepoService.bind(user, projectId, repoId);
        } else {
            throw new CodedException(HttpStatus.BAD_REQUEST, "repo不能作为唯一标识");
        }
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("解除代码库关联项目")
    @DeleteMapping("repoUnbindProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse repoUnbindProject(@RequestParam Long companyId,
                                          @Deprecated @RequestParam(required = false) String repo,
                                          @RequestParam(required = false, defaultValue = "0") Long repoId,
                                          @RequestParam(required = false) Long projectId) {
        if (repoId > 0) {
            if (projectId != null && projectId > 0) {
                projectRepoService.unBind(projectId, repoId);
            } else {
                projectRepoService.unBindByRepo(repoId, companyId);
            }
        } else {
            throw new CodedException(HttpStatus.BAD_REQUEST, "不再支持根据repo");
        }
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询Wiki空间关联的项目")
    @GetMapping("wikiSpaceBindProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<RelatesProjectsBean<ProjectWikiSpace>> getProjectByWikiSpace(@RequestParam Long spaceId) {
        return success(projectWikiSpaceService.selectProjectBySpaceId(spaceId));
    }

    @ApiOperation("查询project关联的wiki空间")
    @PostMapping("projectsBindWikiSpaces")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<List<Long>> getWikiSpaceIdsByProjects(@ApiParam(value = "要查询的项目ID", example = "1") @RequestBody List<Long> projectIds, @ApiParam(value = "用户名", example = "admin") @RequestParam String user) {
        return success(projectWikiSpaceService.selectByProjectIds(projectIds).stream().map(ProjectWikiSpace::getSpaceId).distinct().collect(Collectors.toList()));
    }

    @ApiOperation("查询企业下所有项目关联的wiki空间")
    @PostMapping("companyBindWikiSpaces")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")})
    public BaseResponse<List<Long>> getWikiSpaceIdsByCompany(@ApiParam(value = "要查询的企业ID", example = "1") @RequestParam Long companyId, @ApiParam(value = "用户名", example = "admin") @RequestParam String user) {
        return success(projectWikiSpaceService.selectByCompanyId(companyId).stream().map(ProjectWikiSpace::getSpaceId).distinct().collect(Collectors.toList()));
    }

    @ApiOperation("Wiki空间关联项目")
    @PutMapping("wikiSpaceBindProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse wikiSpaceProject(@RequestParam Long projectId,
                                         @RequestParam Long spaceId,
                                         @ApiParam(value = "用户名", example = "1") @RequestParam String user) {
        checkIsProjectMember(user, projectId);
        projectWikiSpaceService.bind(user, projectId, spaceId, false);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("解除Wiki空间关联项目")
    @DeleteMapping("wikiSpaceUnbindProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse wikiSpaceUnbindProject(@RequestParam Long spaceId,
                                               @RequestParam Long projectId) {
        projectWikiSpaceService.unBind(projectId, spaceId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询主机组关联的项目")
    @GetMapping("hostGroupBindProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<RelatesProjectsBean<ProjectHostGroup>> getProjectByHostGroup(@RequestParam Long groupId) {
        return success(projectResourceService.selectProjectByHostGroupId(groupId));
    }

    @ApiOperation("主机组关联项目")
    @PutMapping("hostGroupBindProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse hostGroupProject(@RequestParam Long projectId,
                                         @RequestParam Long groupId,
                                         @ApiParam(value = "用户名", example = "1") @RequestParam String user) {
        checkIsProjectMember(user, projectId);
        projectResourceService.bind(user, projectId, groupId, false);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("解除主机组关联项目")
    @DeleteMapping("hostGroupUnbindProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse hostGroupUnbindProject(@RequestParam Long groupId,
                                               @RequestParam Long projectId) {
        projectResourceService.unBind(projectId, groupId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询制品库关联的项目")
    @GetMapping("artifactRepoBindProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<RelatesProjectsBean<ProjectArtifactRepo>> getProjectByArtifactRepo(@RequestParam Long repoId) {
        return success(projectArtifactRepoService.selectProjectByRepoId(repoId));
    }

    @ApiOperation("制品库关联项目")
    @PutMapping("artifactRepoBindProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse artifactRepoProject(@RequestParam Long projectId,
                                            @RequestParam Long repoId,
                                            @ApiParam(value = "用户名", example = "u1") @RequestParam String user) {
        checkIsProjectMember(user, projectId);
        projectArtifactRepoService.bind(user, projectId, repoId, false);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("解除制品库关联项目")
    @DeleteMapping("artifactRepoUnbindProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse artifactRepoUnbindProject(@RequestParam Long repoId,
                                                  @RequestParam Long projectId) {
        projectArtifactRepoService.unBind(projectId, repoId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询Doc空间关联的项目")
    @GetMapping("docSpaceBindProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<RelatesProjectsBean<ProjectDocSpace>> getProjectByDocSpace(@RequestParam Long spaceId) {
        return success(projectDocSpaceService.selectProjectBySpaceId(spaceId));
    }

    @ApiOperation("Doc空间关联项目")
    @PutMapping("docSpaceBindProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse docSpaceProject(@RequestParam Long projectId,
                                        @RequestParam Long spaceId,
                                        @ApiParam(value = "用户名", example = "1") @RequestParam String user) {
        checkIsProjectMember(user, projectId);
        projectDocSpaceService.bind(user, projectId, spaceId, false);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("解除Doc空间关联项目")
    @DeleteMapping("docSpaceUnbindProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse docSpaceUnbindProject(@RequestParam Long spaceId,
                                              @RequestParam Long projectId) {
        projectDocSpaceService.unBind(projectId, spaceId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询k8s集群关联的项目")
    @GetMapping("k8sGroupBindProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<RelatesBean<ProjectK8sGroup>> getProjectByK8s(@ApiParam(value = "公司ID", example = "1") @RequestParam Long companyId,
                                                                      @Deprecated @ApiParam(value = "k8s环境Id", example = "1") @RequestParam(required = false) Long k8sGroupId
    ) {
        return success(projectK8sGroupService.selectProjectByK8sGroupId(k8sGroupId));
    }

    @ApiOperation("k8s集群关联项目")
    @PutMapping("k8sGroupBindProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse k8sGroupBindProject(@RequestParam Long projectId,
                                            @RequestParam(required = false, defaultValue = "0") Long k8sGroupId,
                                            @ApiParam(value = "用户名", example = "1") @RequestParam String user) {
        checkIsProjectMember(user, projectId);
        projectK8sGroupService.bind(user, projectId, k8sGroupId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("解除代码库关联项目")
    @DeleteMapping("k8sGroupUnbindProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse k8sGroupUnbindProject(@RequestParam Long companyId,
                                              @Deprecated @RequestParam(required = false) String k8sGroupKey,
                                              @RequestParam(required = false, defaultValue = "0") Long k8sGroupId,
                                              @RequestParam(required = false) Long projectId) {
        projectK8sGroupService.unBind(projectId, k8sGroupId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("获取项目角色成员")
    @GetMapping("{id:[0-9]+}/roleMembers")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<List<BaseGroupUser>> roleMembers(
            @ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
            @ApiParam(value = "角色来源", example = "1") @RequestParam RoleSource roleSource,
            @ApiParam(value = "角色key", example = "1") @RequestParam String roleKey) {
        ProjectRoleSchema schema = projectSchemaQueryService.getProjectRoleSchema(id);
        if (schema == null) {
            throw CodedException.NOT_FOUND;
        }
        ProjectRole role = (ProjectRole)schema.findRole(roleSource, roleKey);
        if (role == null) {
            throw CodedException.NOT_FOUND;
        }
        List<ProjectMember> members = projectMemberQueryService.select(id, roleSource, roleKey);
        if (CollectionUtils.isEmpty(members)) {
            return success(ListUtils.EMPTY_LIST);
        }
        return success(members.stream()
                .map(member -> new BaseGroupUser()
                        .setType(GroupUserType.valueOf(member.getUserType()))
                        .setName(member.getUser()))
                .collect(Collectors.toList()));
    }
}
