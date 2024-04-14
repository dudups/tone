package com.ezone.ezproject.modules.portfolio.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.es.entity.OperationConfig;
import com.ezone.ezproject.es.entity.PortfolioOperationSchema;
import com.ezone.ezproject.es.entity.PortfolioRole;
import com.ezone.ezproject.es.entity.Role;
import com.ezone.ezproject.es.entity.enums.PortfolioOperationType;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.es.entity.enums.RoleType;
import com.ezone.ezproject.modules.portfolio.service.PortfolioOperationSchemaHelper;
import com.ezone.ezproject.modules.portfolio.service.PortfolioRoleSchemaHelper;
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
@RequestMapping("/portfolio/schema")
@Slf4j
@AllArgsConstructor
public class SystemPortfolioSchemaController extends AbstractController {
    private PortfolioRoleSchemaHelper roleSchemaHelper;
    private PortfolioOperationSchemaHelper operationSchemaHelper;

    @ApiOperation("查询项目集操作权限定义")
    @GetMapping("operation")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<PortfolioOperationSchema> protoOperationSchema() {
        return success(operationSchemaHelper.getSysPortfolioOperationSchema());
    }

    @ApiOperation("查询项目集默认角色")
    @GetMapping("operation/init")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<Map<PortfolioOperationType, OperationConfig>> roleDefaultOperations(@RequestParam RoleType roleType) {
        Role role = roleSchemaHelper.getSysCustomRoleSchema().findRole(RoleSource.SYS, roleType.name());
        return success(((PortfolioRole) role).getOperations());
    }
}
