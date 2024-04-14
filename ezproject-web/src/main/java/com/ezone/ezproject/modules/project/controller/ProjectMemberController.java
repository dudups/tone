package com.ezone.ezproject.modules.project.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.enums.ProjectMemberRole;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.modules.project.bean.MemberBean;
import com.ezone.ezproject.modules.project.bean.RoleKeySource;
import com.ezone.ezproject.modules.project.service.ProjectMemberCmdService;
import com.ezone.ezproject.modules.project.service.ProjectMemberQueryService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApiOperation("项目成员操作")
@RestController
@RequestMapping("/project/project/{id:[0-9]+}/member")
@Slf4j
@AllArgsConstructor
@Validated
public class ProjectMemberController extends AbstractController {
    private ProjectMemberQueryService projectMemberQueryService;

    private ProjectMemberCmdService projectMemberCmdService;

    @ApiOperation("查询项目成员")
    @GetMapping
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<MemberBean>> select(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) {
        checkHasProjectRead(id);
        return success(projectMemberQueryService.select(id).stream().map(MemberBean::from).collect(Collectors.toList()));
    }

    @Deprecated
    @ApiOperation("查询当前用户在项目中最大角色")
    @GetMapping("role")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<ProjectMemberRole> maxRole(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) {
        Long company = companyService.currentCompany();
        String user = userService.currentUserName();
        if (userService.isCompanyAdmin(user, company)) {
            return success(ProjectMemberRole.ADMIN);
        }
        return success(projectMemberQueryService.maxRoleInCompanyProjectMembers(company, id, user));
    }

    @ApiOperation("设置项目成员")
    @PutMapping
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse setMembers(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                   @RequestBody @Valid List<MemberBean> members) throws IOException {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        projectMemberCmdService.setProjectMembers(id, members);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询指定角色的用户")
    @PostMapping("rolesUsers")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<Map<String, Set<String>>> rolesUsers(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id, @ApiParam(value = "bpm中配置的角色", example = "8-COMPANY,ADMIN-SYS") @RequestBody List<String> bpmRoles) {
        checkHasProjectRead(id);
        //结果格式（key与传入保持一致） key:role + '-' + role_source(project_member表中role与role_source值）, value:user集合
        Map<String, Set<String>> result = new HashMap<>();
        if (bpmRoles.size() < 1) {
            return success(result);
        }

        String bpmRoleRoleResourceSeparator = "-";
        List<RoleKeySource> projectMembers = new ArrayList<>();
        for (String roleRoleResource : bpmRoles) {
            String[] split = roleRoleResource.split(bpmRoleRoleResourceSeparator);
            if (split.length != 2) {
                log.error("bpm中角色配置异常！配置角色为：" + roleRoleResource);
                throw new CodedException(HttpStatus.BAD_REQUEST, "请求角色格式错误，" + roleRoleResource + "格式应为role-roleSource");
            }
            projectMembers.add(RoleKeySource.builder().role(split[0]).roleSource(RoleSource.valueOf(split[1])).build());
        }

        Map<RoleKeySource, Set<String>> roleMembers = projectMemberQueryService.selectProjectRoleUsers(id, projectMembers);

        roleMembers.forEach((roleKeySource, usernames) -> {
            String key = roleKeySource.getRole() + bpmRoleRoleResourceSeparator + roleKeySource.getRoleSource();
            Set<String> roleUsers = result.getOrDefault(key, new HashSet<>());
            result.put(key, roleUsers);
            roleUsers.addAll(usernames);
        });
        return success(result);
    }
}
