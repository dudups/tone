package com.ezone.ezproject.modules.company.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.ProjectMember;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.CompanyProjectSchema;
import com.ezone.ezproject.es.entity.CompanyWorkloadSetting;
import com.ezone.ezproject.es.entity.ProjectField;
import com.ezone.ezproject.es.entity.ProjectRole;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.modules.company.bean.UpdateRoleRankRequest;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaCmdService;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService;
import com.ezone.ezproject.modules.company.service.RoleRankCmdService;
import com.ezone.ezproject.modules.project.bean.CompanyCardTypeConf;
import com.ezone.ezproject.modules.project.service.ProjectMemberQueryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@ApiOperation("项目模版操作")
@RestController
@RequestMapping("/project/company-project-schema")
@Slf4j
@AllArgsConstructor
public class CompanyProjectSchemaController extends AbstractController {
    private CompanyProjectSchemaCmdService companyProjectSchemaCmdService;
    private CompanyProjectSchemaQueryService companyProjectSchemaQueryService;

    private ProjectMemberQueryService projectMemberQueryService;

    private RoleRankCmdService companyRoleRankCmdService;

    @ApiOperation("更新公司下的项目自定义扩展字段")
    @PutMapping("fields")
    @CheckAuthType(TokenAuthType.ALL)
    public BaseResponse updateFields(@Valid @RequestBody List<ProjectField> fields) {
        checkIsCompanyAdmin();
        if (CollectionUtils.isNotEmpty(fields)) {
            companyProjectSchemaCmdService.setFields(companyService.currentCompany(), fields);
        }
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询当前公司项目定义信息")
    @CheckAuthType(TokenAuthType.READ)
    @GetMapping
    public BaseResponse<CompanyProjectSchema> getCompanyProjectSchema() {
        checkIsCompanyAdmin();
        return getCompanyProjectSchemaPublic();
    }

    @ApiOperation("查询当前公司项目定义信息")
    @CheckAuthType(TokenAuthType.READ)
    @GetMapping("public")
    public BaseResponse<CompanyProjectSchema> getCompanyProjectSchemaPublic() {
        return success(companyProjectSchemaQueryService.getCompanyProjectSchema(companyService.currentCompany()));
    }

    @ApiOperation("查询当前公司级别卡片定义信息")
    @GetMapping("card")
    public BaseResponse<CompanyCardSchema> getCompanyCardSchema() {
        checkIsCompanyAdmin();
        return getCompanyCardSchemaPublic();
    }

    @ApiOperation("查询当前公司级别卡片定义信息")
    @GetMapping("card/public")
    public BaseResponse<CompanyCardSchema> getCompanyCardSchemaPublic() {
        return success(companyProjectSchemaQueryService.getCompanyCardSchema(companyService.currentCompany()));
    }

    @ApiOperation("更新公司级别卡片类型定义信息")
    @PutMapping("card/types")
    public BaseResponse updateCardTypes(@Valid @RequestBody List<CompanyCardTypeConf> types) {
        checkIsCompanyAdmin();
        if (CollectionUtils.isNotEmpty(types)) {
            companyProjectSchemaCmdService.setCardTypes(companyService.currentCompany(), types);
        }
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查看企业角色")
    @GetMapping("role")
    @CheckAuthType(TokenAuthType.READ)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<List<ProjectRole>> listRoles() {
        checkIsCompanyAdmin();
        Long companyId = companyService.currentCompany();
        return success(companyProjectSchemaQueryService.getCompanyProjectRoleSchema(companyId).getRoles());
    }

    @ApiOperation("查看企业角色")
    @GetMapping("role/public")
    @CheckAuthType(TokenAuthType.READ)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<List<ProjectRole>> listRolesPublic() {
        Long companyId = companyService.currentCompany();
        return success(companyProjectSchemaQueryService.getCompanyProjectRoleSchema(companyId).getRoles());
    }

    @ApiOperation("新建企业角色")
    @PostMapping("role")
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse addRole(@Valid @RequestBody ProjectRole role) throws IOException {
        checkIsCompanyAdmin();
        Long companyId = companyService.currentCompany();
        companyProjectSchemaCmdService.addRole(companyId, role);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("更新企业的角色设置")
    @PutMapping("role/update")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse updateRole(@Valid @RequestBody ProjectRole role) {
        checkIsCompanyAdmin();
        Long companyId = companyService.currentCompany();
        companyProjectSchemaCmdService.updateRole(companyId, role);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("移除企业角色")
    @DeleteMapping("role/{roleKey}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse deleteRole(@PathVariable String roleKey) throws IOException {
        checkIsCompanyAdmin();
        Long companyId = companyService.currentCompany();
        checkRoleHasUsed(roleKey, companyId);
        companyProjectSchemaCmdService.deleteRole(companyId, roleKey);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("更新企业的工时设置")
    @PutMapping("workloadSetting")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse saveWorkloadSetting(@Valid @RequestBody CompanyWorkloadSetting workloadSetting) {
        checkIsCompanyAdmin();
        Long companyId = companyService.currentCompany();
        companyProjectSchemaCmdService.saveWorkloadSetting(companyId, workloadSetting);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查看企业工时设置")
    @GetMapping("workloadSetting/public")
    @CheckAuthType(TokenAuthType.READ)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<CompanyWorkloadSetting> findWorkloadSetting() {
        Long companyId = companyService.currentCompany();
        return success(companyProjectSchemaQueryService.getCompanyWorkloadSetting(companyId));
    }

    @ApiOperation("查看企业工时设置")
    @GetMapping("workloadSetting")
    @CheckAuthType(TokenAuthType.READ)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<CompanyWorkloadSetting> findWorkloadSettingPublic() {
        checkIsCompanyAdmin();
        Long companyId = companyService.currentCompany();
        return success(companyProjectSchemaQueryService.getCompanyWorkloadSetting(companyId));
    }

    private void checkRoleHasUsed(String roleKey, Long companyId) {
        List<ProjectMember> projectMembers = projectMemberQueryService.listCompanyRoleMember(companyId, roleKey);
        if (CollectionUtils.isNotEmpty(projectMembers)) {
            List<Long> projectIds = projectMembers.stream().map(ProjectMember::getProjectId).collect(Collectors.toList());
            int size = projectIds.size();
            int limit = size > 3 ? 3 : size;
            List<Project> projects = projectQueryService.select(projectIds.subList(0, limit));
            final StringBuilder buf = new StringBuilder();
            for (int i = 0; i < projects.size(); i++) {
                if (i > 0) {
                    buf.append("、");
                }
                buf.append(projects.get(i).getName());
            }
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("[%s]项目下，此角色已经有用户，无法删除此角色", buf.toString()));
        }
    }

    @ApiOperation("更新角色排序位置")
    @PutMapping("rankByProjectRoleRank")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<List<ProjectRole>> rankByProjectRoleRank(@Valid @RequestBody UpdateRoleRankRequest request) {
        checkIsCompanyAdmin();
        if (request.getSource() != RoleSource.COMPANY) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "只支持企业角色之间的顺序调整！");
        }
        Long companyId = companyService.currentCompany();
        companyProjectSchemaCmdService.rankByProjectRoleRank(companyId, request.getRoleKey(), request.getReferenceRoleKey(), request.getLocation());
        return success(companyProjectSchemaQueryService.getCompanyProjectRoleSchema(companyId).getRoles());
    }
}
