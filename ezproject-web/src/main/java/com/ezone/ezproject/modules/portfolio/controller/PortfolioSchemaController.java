package com.ezone.ezproject.modules.portfolio.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.PortfolioRole;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.modules.company.bean.UpdateRoleRankRequest;
import com.ezone.ezproject.modules.portfolio.bean.DeletePortfolioRoleOptions;
import com.ezone.ezproject.modules.portfolio.service.PortfolioSchemaCmdService;
import com.ezone.ezproject.modules.portfolio.service.PortfolioSchemaQueryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
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

@ApiOperation("项目集schema操作")
@RestController
@RequestMapping("/portfolio/{id:[0-9]+}/schema")
@Slf4j
@AllArgsConstructor
@Validated
public class PortfolioSchemaController extends AbstractController {

    private PortfolioSchemaCmdService schemaCmdService;
    private PortfolioSchemaQueryService schemaQueryService;

    @ApiOperation("查看项目集角色")
    @GetMapping("role")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<PortfolioRole>> listRoles(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) {
        checkHasPortfolioRead(id);
        return success(schemaQueryService.getPortfolioRoleSchema(id).getRoles());
    }

    @ApiOperation("新建项目集角色")
    @PostMapping("role")
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse addRole(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                @Valid @RequestBody PortfolioRole role) throws IOException {
        checkHasPortfolioManager(id);
        schemaCmdService.addRole(id, role);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("移除项目集角色")
    @DeleteMapping("role/{roleKey}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse deleteRole(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                   @PathVariable String roleKey,
                                   @RequestBody(required = false) DeletePortfolioRoleOptions options) throws Exception {
        checkHasPortfolioManager(id);
        schemaCmdService.deleteRole(id, roleKey, options);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("更新项目集的角色设置")
    @PutMapping("role/update")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse updateRole(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                   @Valid @RequestBody PortfolioRole role) {
        checkHasPortfolioManager(id);
        schemaCmdService.updateRole(id, role);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("移动项目集自定义角色位置")
    @PostMapping("role/customerRoleRank")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<List<PortfolioRole>> customerRoleRank(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                                              @RequestBody UpdateRoleRankRequest request) {
        checkHasPortfolioManager(id);
        if (request.getSource() != RoleSource.CUSTOM) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "只支持自定义角色之间的顺序调整！");
        }
        schemaCmdService.customerRoleRank(id, request.getRoleKey(), request.getReferenceRoleKey(), request.getLocation());
        return success(schemaQueryService.getPortfolioRoleSchema(id).getRoles());
    }

}