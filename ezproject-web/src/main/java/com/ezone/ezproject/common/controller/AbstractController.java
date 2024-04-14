package com.ezone.ezproject.common.controller;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.UserProjectPermissions;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.es.entity.enums.PortfolioOperationType;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.permission.CompanyPermissionService;
import com.ezone.ezproject.modules.permission.PermissionService;
import com.ezone.ezproject.modules.portfolio.service.UserPortfolioPermissionsService;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import javax.validation.constraints.NotNull;
import java.util.Base64;

@Setter(onMethod_ = {@Autowired})
@NoArgsConstructor
public class AbstractController implements IWrapResponseController {
    protected CompanyService companyService;
    protected UserService userService;

    protected ProjectQueryService projectQueryService;
    protected PermissionService permissionService;
    protected CompanyPermissionService companyPermissionService;

    protected UserPortfolioPermissionsService userPortfolioPermissionsService;

    public static final CodedException PROJECT_NOT_FOUND_EXCEPTION = new CodedException(HttpStatus.NOT_FOUND, "项目不存在!");
    public static final CodedException PROJECT_NOT_ACTIVE_EXCEPTION = new CodedException(HttpStatus.FORBIDDEN, "项目已归档!");
    public static final CodedException PLAN_NOT_ACTIVE_EXCEPTION = new CodedException(HttpStatus.FORBIDDEN, "所属计划已归档!");

    private UserProjectPermissions permissions(String user, Long projectId) throws CodedException {
        Project project = projectQueryService.select(projectId);
        if (null == project) {
            throw PROJECT_NOT_FOUND_EXCEPTION;
        }
        return permissionService.permissions(user, projectId);
    }

    protected UserProjectPermissions permissions(Long projectId) throws CodedException {
        Long companyId = companyService.currentCompany();
        if (companyId == null || !companyId.equals(projectQueryService.getProjectCompany(projectId))) {
            throw CodedException.COMPANY_OUT_FORBIDDEN;
        }
        return permissions(userService.currentUserName(), projectId);
    }

    private UserProjectPermissions permissions(String projectKey) throws CodedException {
        Project project = projectQueryService.select(projectKey);
        if (null == project) {
            throw PROJECT_NOT_FOUND_EXCEPTION;
        }
        if (!project.getCompanyId().equals(companyService.currentCompany())) {
            throw CodedException.COMPANY_OUT_FORBIDDEN;
        }
        return permissionService.permissions(userService.currentUserName(), project.getId());
    }

    protected void checkProjectActive(Project project, OperationType op) {
        if (null == project) {
            throw PROJECT_NOT_FOUND_EXCEPTION;
        }
        if (OperationType.PROJECT_ACTIVE_OPS.contains(op)) {
            return;
        }
        if (BooleanUtils.isNotTrue(project.getIsActive())) {
            throw PROJECT_NOT_ACTIVE_EXCEPTION;
        }
    }

    protected void checkProjectActive(Long projectId, OperationType op) {
        checkProjectActive(project(projectId), op);
    }

    protected void checkProjectActive(String projectKey, OperationType op) {
        checkProjectActive(project(projectKey), op);
    }

    protected void checkPermission(Long projectId, OperationType op) {
        checkProjectActive(projectId, op);
        UserProjectPermissions permissions = permissions(projectId);
        if (permissions == null || !permissions.hasPermission(op)) {
            throw CodedException.FORBIDDEN;
        }
    }

    protected boolean hasPermission(String user, Long projectId, OperationType op) {
        UserProjectPermissions permissions = permissions(user, projectId);
        return permissions != null && permissions.hasPermission(op);
    }

    protected void checkPermission(String user, Long projectId, OperationType op) {
        checkProjectActive(projectId, op);
        UserProjectPermissions permissions = permissions(user, projectId);
        if (permissions == null || !permissions.hasPermission(op)) {
            throw CodedException.FORBIDDEN;
        }
    }

    @NotNull
    protected Project project(String projectKey) throws CodedException {
        Project project = projectQueryService.select(projectKey);
        if (null == project) {
            throw PROJECT_NOT_FOUND_EXCEPTION;
        }
        return project;
    }

    @NotNull
    protected Project project(Long projectId) throws CodedException {
        Project project = projectQueryService.select(projectId);
        if (null == project) {
            throw PROJECT_NOT_FOUND_EXCEPTION;
        }
        return project;
    }

    protected void checkIsCompanyAdmin() throws CodedException {
        String user = userService.currentUserName();
        Long company = companyService.currentCompany();
        if (!userService.isCompanyAdmin(user, company)) {
            throw CodedException.FORBIDDEN;
        }
    }

    @Deprecated
    protected void checkIsProjectMember(Long projectId) throws CodedException {
        if (!permissionService.isMember(userService.currentUserName(), projectId)) {
            throw CodedException.FORBIDDEN;
        }
    }

    protected void checkIsProjectMember(String user, Long projectId) throws CodedException {
        if (!permissionService.isMember(user, projectId)) {
            throw CodedException.FORBIDDEN;
        }
    }

    protected void checkIsProjectMember(String user, String projectKey) throws CodedException {
        if (!permissionService.isMember(user, project(projectKey).getId())) {
            throw CodedException.FORBIDDEN;
        }
    }

    protected void checkHasProjectRead(Long projectId) throws CodedException {
        checkHasProjectRead(userService.currentUserName(), projectId);
    }

    protected void checkHasProjectRead(String user, Long projectId) throws CodedException {
        checkPermission(user, projectId, OperationType.PROJECT_READ);
    }

    protected void checkHasProjectRead(String projectKey) throws CodedException {
        checkHasProjectRead(project(projectKey).getId());
    }

    protected void checkPassword(String password) throws CodedException {
        if (!userService.validateUserPassword(userService.currentUserName(), password)) {
            throw new CodedException(HttpStatus.FORBIDDEN, "密码错误!");
        }
    }

    protected void checkBase64Password(String password) throws CodedException {
        checkPassword(new String(Base64.getDecoder().decode(password)));
    }

    protected void checkCreateProject() throws CodedException {
        companyPermissionService.checkCreateProject();
    }

    protected void checkCreatePortfolio() throws CodedException {
        companyPermissionService.checkCreatePortfolio();
    }

    protected void checkHasPortfolioRead(Long portfolioId) throws CodedException {
        userPortfolioPermissionsService.checkHasPortfolioRead(portfolioId);
    }

    public void checkHasPortfolioManager(Long portfolioId) throws CodedException {
        userPortfolioPermissionsService.checkPermission(userService.currentUserName(), portfolioId, PortfolioOperationType.PORTFOLIO_MANAGE_UPDATE);
    }


    protected void checkHasPortfolioPermission(Long portfolioId, PortfolioOperationType op) {
        userPortfolioPermissionsService.checkPermission(userService.currentUserName(), portfolioId, op);
    }
}
