package com.ezone.ezproject.modules.project.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.limit.incr.CompanyIncrLimit;
import com.ezone.ezproject.dal.entity.Portfolio;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.enums.ProjectMemberRole;
import com.ezone.ezproject.dal.mapper.ExtPortfolioMapper;
import com.ezone.ezproject.es.entity.ProjectMenuConfig;
import com.ezone.ezproject.es.entity.UserProjectPermissions;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.cli.EzTestCliService;
import com.ezone.ezproject.modules.project.bean.CreateProjectRequest;
import com.ezone.ezproject.modules.project.bean.ProjectAndRole;
import com.ezone.ezproject.modules.project.bean.ProjectExt;
import com.ezone.ezproject.modules.project.bean.ProjectMenuConfigReq;
import com.ezone.ezproject.modules.project.bean.ProjectSummaryConfRequest;
import com.ezone.ezproject.modules.project.bean.SearchScope;
import com.ezone.ezproject.modules.project.bean.UpdateProjectRequest;
import com.ezone.ezproject.modules.project.service.CompanyProjectDailyLimiter;
import com.ezone.ezproject.modules.project.service.ProjectCmdService;
import com.ezone.ezproject.modules.project.service.ProjectFavouriteCmdService;
import com.ezone.ezproject.modules.project.service.ProjectMemberQueryService;
import com.ezone.ezproject.modules.project.service.ProjectMenuService;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import com.ezone.ezproject.modules.project.service.ProjectSummaryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import com.ezone.galaxy.framework.common.bean.PageResult;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@ApiOperation("项目操作")
@RestController
@RequestMapping("/project/project")
@Slf4j
@AllArgsConstructor
public class ProjectController extends AbstractController {
    private ProjectQueryService projectQueryService;

    private ProjectCmdService projectCmdService;

    private ProjectMemberQueryService projectMemberQueryService;

    private ProjectFavouriteCmdService projectFavouriteCmdService;

    private EzTestCliService ezTestCliService;

    private ProjectSummaryService projectSummaryService;

    private ProjectMenuService projectMenuService;

    private ExtPortfolioMapper extPortfolioMapper;

    @ApiOperation("新建项目")
    @PostMapping
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    @CompanyIncrLimit(domainResourceKey = CompanyProjectDailyLimiter.DOMAIN_RESOURCE_KEY)
    public BaseResponse<Project> create(@Valid @RequestBody CreateProjectRequest createProjectRequest) throws IOException {
        checkCreateProject();
        return success(projectCmdService.create(createProjectRequest));
    }

    @ApiOperation("更新项目")
    @PutMapping("{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<ProjectExt> update(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                           @Valid @RequestBody UpdateProjectRequest updateProjectRequest) throws IOException {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        return success(projectCmdService.update(id, updateProjectRequest));
    }

    @ApiOperation("归档")
    @PutMapping("{id:[0-9]+}/inactive")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse inactive(
            @ApiParam(value = "项目ID", example = "1") @PathVariable Long id)
            throws IOException {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        Project project = projectQueryService.select(id);
        if (project == null) {
            throw CodedException.NOT_FOUND;
        }
        projectCmdService.setIsActive(id, false);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("取消归档")
    @PutMapping("{id:[0-9]+}/active")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse active(
            @ApiParam(value = "项目ID", example = "1") @PathVariable Long id)
            throws IOException {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        Project project = projectQueryService.select(id);
        if (project == null) {
            throw CodedException.NOT_FOUND;
        }
        projectCmdService.setIsActive(id, true);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询项目")
    @GetMapping("{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<ProjectAndRole> select(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                               @ApiParam(value = "是否返回当前用户角色") @RequestParam(required = false) boolean withRole) throws IOException {
        checkHasProjectRead(id);
        return withRole(projectQueryService.select(id), withRole);
    }

    @ApiOperation("通过项目KEY查询项目")
    @GetMapping("key/{key}")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<ProjectAndRole> select(@ApiParam(value = "项目KEY", example = "project1") @PathVariable String key,
                                               @ApiParam(value = "是否返回当前用户角色") @RequestParam(required = false) boolean withRole) throws IOException {
        Project project = project(key);
        checkHasProjectRead(project.getId());
        return withRole(project, withRole);
    }

    @ApiOperation("查询项目操作权限")
    @GetMapping("{id:[0-9]+}/permissions")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<UserProjectPermissions> selectPermissions(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) throws IOException {
        return success(permissions(id));
    }

    @ApiOperation("查询当前公司下的项目列表")
    @PostMapping("search")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<ProjectExt>> search(
            @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false, defaultValue = "ALL") SearchScope scope,
            @RequestBody SearchEsRequest searchRequest,
            HttpServletResponse response) throws IOException {
        TotalBean<ProjectExt> totalBean = projectQueryService.search(scope, searchRequest, pageNumber, pageSize);
        response.addHeader(PageResult.PAGE_TOTAL_COUNT, String.valueOf(totalBean.getTotal()));
        return success(totalBean.getList());
    }

    @Deprecated
    @ApiOperation("查询当前公司下的项目列表-todo删除，因为dev环境单独加上")
    @GetMapping
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<ProjectExt>> select(@RequestParam(required = false, defaultValue = "1") Integer pageNumber,
                                                 @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                                 @ApiParam(value = "查询项目名过滤") @RequestParam(required = false) String q,
                                                 @RequestParam(required = false, defaultValue = "ALL") SearchScope scope,
                                                 HttpServletResponse response) throws IOException {
        TotalBean<ProjectExt> totalBean = projectQueryService.search(companyService.currentCompany(), userService.currentUserName(), scope, q, pageNumber, pageSize);
        response.addHeader(PageResult.PAGE_TOTAL_COUNT, String.valueOf(totalBean.getTotal()));
        return success(totalBean.getList());
    }

    @ApiOperation("删除项目")
    @DeleteMapping("{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.ALL)
    public BaseResponse delete(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) throws IOException {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        projectCmdService.delete(id);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation(value = "check")
    @GetMapping(value = "/check/key")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse checkKey(@RequestParam String key, @RequestParam(required = false, defaultValue = "0") Long projectId) {
        projectQueryService.checkKey(key, projectId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation(value = "check")
    @GetMapping(value = "/check/name")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse checkName(@RequestParam String name, @RequestParam(required = false, defaultValue = "0") Long projectId) {
        projectQueryService.checkName(name, projectId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("收藏")
    @PostMapping("{id:[0-9]+}/favourite")
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse favouriteProject(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) {
        checkHasProjectRead(id);
        projectFavouriteCmdService.favouriteProject(userService.currentUserName(), id);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("取消收藏")
    @DeleteMapping("{id:[0-9]+}/favourite")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse unFavouriteProject(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) {
        projectFavouriteCmdService.unFavouriteProject(userService.currentUserName(), id);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询项目关联的测试空间")
    @GetMapping("{id:[0-9]+}/listSpaces")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse listSpaces(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) {
        checkHasProjectRead(id);
        return success(ezTestCliService.listSpaces(id));
    }

    @ApiOperation("查询项目关联的测试空间")
    @GetMapping("{id:[0-9]+}/listBugSpaces")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse listBugSpaces(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) {
        checkHasProjectRead(id);
        return success(ezTestCliService.listBugSpaces(id));
    }

    @ApiOperation("查询项目关联的测试空间")
    @GetMapping("{id:[0-9]+}/listRequirementSpaces")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse listRequirementSpaces(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) {
        checkHasProjectRead(id);
        return success(ezTestCliService.listRequirementSpaces(id));
    }

    @ApiOperation("项目概览设置")
    @PutMapping("{id:[0-9]+}/summaryConfig")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<Project> summaryConfig(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                               @Valid @RequestBody ProjectSummaryConfRequest summaryConfReq) throws IOException {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        projectSummaryService.updateProjectSummaryConfig(id, summaryConfReq.getCharts(), summaryConfReq.getRightCharts());
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("新增项目置顶")
    @PutMapping("{id:[0-9]+}/top")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse top(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) throws IOException {
        checkIsCompanyAdmin();
        Project project = projectQueryService.select(id);
        if (project == null) {
            throw CodedException.NOT_FOUND;
        }
        projectCmdService.addTop(project.getId());
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("取消项目置顶")
    @DeleteMapping("{id:[0-9]+}/top")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse deleteTop(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) throws IOException {
        checkIsCompanyAdmin();
        Project project = projectQueryService.select(id);
        if (project == null) {
            throw CodedException.NOT_FOUND;
        }
        projectCmdService.deleteTop(id);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("获取项目菜单配置")
    @GetMapping("{key}/menus")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<ProjectMenuConfig> projectMenus(@ApiParam(value = "项目ID", example = "1") @PathVariable String key) {
        Project project = project(key);
        Long id = project.getId();
        checkPermission(id, OperationType.PROJECT_READ);
        return success(projectMenuService.getMenuConfig(id));
    }

    @ApiOperation("设置项目菜单")
    @PutMapping("{key}/menus")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse saveProjectMenus(@ApiParam(value = "项目ID", example = "1") @PathVariable String key, @RequestBody ProjectMenuConfigReq menuConfig) {
        Project project = project(key);
        checkPermission(project.getId(), OperationType.PROJECT_MANAGE_UPDATE);
        projectMenuService.saveOrUpdateOpenMenus(project.getId(), menuConfig);
        return SUCCESS_RESPONSE;
    }

    protected BaseResponse<ProjectAndRole> withRole(Project project, boolean withRole) throws IOException {
        Map<String, Object> extend = projectQueryService.selectExtend(project.getId());
        List<Portfolio> portfolios = extPortfolioMapper.selectRelPortfolioByProjectId(project.getId());
        if (withRole) {
            Long company = companyService.currentCompany();
            String user = userService.currentUserName();
            if (userService.isCompanyAdmin(user, company)) {
                return success(new ProjectAndRole(project, extend, ProjectMemberRole.ADMIN, portfolios));
            }
            return success(new ProjectAndRole(project, extend,
                    projectMemberQueryService.maxRoleInCompanyProjectMembers(company, project.getId(), user), portfolios));
        }
        return success(new ProjectAndRole(project, extend, portfolios));
    }
}
