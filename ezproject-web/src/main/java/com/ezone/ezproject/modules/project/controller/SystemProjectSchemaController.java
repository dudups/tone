package com.ezone.ezproject.modules.project.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.es.entity.OperationConfig;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectOperationSchema;
import com.ezone.ezproject.es.entity.ProjectRole;
import com.ezone.ezproject.es.entity.Role;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.es.entity.enums.RoleType;
import com.ezone.ezproject.modules.project.service.ProjectCardSchemaHelper;
import com.ezone.ezproject.modules.project.service.ProjectOperationSchemaHelper;
import com.ezone.ezproject.modules.project.service.ProjectRoleSchemaHelper;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@ApiOperation("系统定义/项目")
@RestController
@RequestMapping("/project/project/schema")
@Slf4j
@AllArgsConstructor
public class SystemProjectSchemaController extends AbstractController {
    private ProjectRoleSchemaHelper roleSchemaHelper;
    private ProjectOperationSchemaHelper operationSchemaHelper;
    private ProjectCardSchemaHelper cardSchemaHelper;

    @ApiOperation("查询项目操作权限定义")
    @GetMapping("operation")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<ProjectOperationSchema> projectOperationSchema() {
        return success(operationSchemaHelper.getSysProjectOperationSchema());
    }

    @ApiOperation("查询项目默认角色")
    @GetMapping("operation/init")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<Map<OperationType, OperationConfig>> roleDefaultOperations(@RequestParam RoleType roleType) {
        Role role = roleSchemaHelper.getSysCustomRoleSchema().findRole(RoleSource.SYS, roleType.name());
        return success(((ProjectRole)role).getOperations());
    }

    @ApiOperation("查询项目操作权限定义")
    @GetMapping("defaultProjectCardSchema")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<ProjectCardSchema> projectCardSchema() {
        return success(cardSchemaHelper.getSysProjectCardSchema());
    }

}
