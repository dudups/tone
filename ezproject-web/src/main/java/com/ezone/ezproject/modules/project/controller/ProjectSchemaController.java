package com.ezone.ezproject.modules.project.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.validate.Uniq;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardFieldFlow;
import com.ezone.ezproject.es.entity.CardStatus;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectRole;
import com.ezone.ezproject.es.entity.ProjectWorkloadSetting;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.modules.card.bean.ChangeTypeCheckResult;
import com.ezone.ezproject.modules.company.bean.UpdateRoleRankRequest;
import com.ezone.ezproject.modules.project.bean.CardStatusesConf;
import com.ezone.ezproject.modules.project.bean.CardTypeConf;
import com.ezone.ezproject.modules.project.bean.CheckSchemaResult;
import com.ezone.ezproject.modules.project.bean.DeleteProjectRoleOptions;
import com.ezone.ezproject.modules.project.service.ProjectSchemaCmdService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
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

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ApiOperation("项目schema操作")
@RestController
@RequestMapping("/project/project/{id:[0-9]+}/schema")
@Slf4j
@AllArgsConstructor
@Validated
public class ProjectSchemaController extends AbstractController {
    private ProjectSchemaQueryService schemaQueryService;
    private ProjectSchemaCmdService schemaCmdService;

    @ApiOperation("获取项目卡片Schema")
    @GetMapping
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<ProjectCardSchema> getProjectCardSchema(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id)
            throws IOException {
        checkHasProjectRead(id);
        return success(schemaQueryService.getProjectCardSchema(id));
    }

    @ApiOperation("跨项目复制卡片校验Schema")
    @GetMapping("checkSchemaForCopy")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<CheckSchemaResult> checkSchemaForCopy(@ApiParam(value = "项目ID", example = "1") @Min(1) @PathVariable("id") Long fromProjectId,
                                                              @ApiParam(value = "项目ID", example = "1") @Min(1) @RequestParam Long toProjectId)
            throws IOException {
        if (fromProjectId.equals(toProjectId)) {
            return success(CheckSchemaResult.builder().build());
        }
        checkPermission(toProjectId, OperationType.PROJECT_MANAGE_UPDATE);
        checkHasProjectRead(fromProjectId);
        return success(schemaQueryService.checkSchemaForCopy(fromProjectId, toProjectId));
    }

    @ApiOperation("同项目转换卡片类型校验Schema")
    @PostMapping("checkSchemaForChangeType")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<List<ChangeTypeCheckResult>> checkSchemaForChangeType(@ApiParam(value = "项目ID", example = "1") @Min(1) @PathVariable Long id,
                                                                              @ApiParam(value = "类型转换映射") @Size(min = 1) @RequestBody Map<String, String> typeMap)
            throws IOException {
        checkHasProjectRead(id);
        return success(schemaQueryService.checkSchemaForChangeType(id, typeMap));
    }

    @ApiOperation("设置项目卡片Schema")
    @PutMapping
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse setProjectCardSchema(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                             @RequestBody ProjectCardSchema schema)
            throws IOException {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        schemaCmdService.setProjectCardSchema(id, schema);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("设置项目schema的卡片类型列表")
    @PutMapping("types")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse setTypes(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                 @Valid @RequestBody CardTypeConf[] cardTypeConfs) {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        schemaCmdService.setTypes(id, Arrays.asList(cardTypeConfs));
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("设置项目schema的字段列表")
    @PutMapping("fields")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse setFields(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                  @Valid @RequestBody CardField[] fields) {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        schemaCmdService.setFields(id, Arrays.asList(fields), true);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("设置项目schema的字段关联")
    @PutMapping("fieldFlows")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse setFieldFlows(
            @ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
            @RequestBody @Valid @Uniq(field = CardFieldFlow.FIELD_KEY, message = "触发字段和值不能重复！") List<CardFieldFlow> fieldFlows) {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        schemaCmdService.setFieldFlows(id, fieldFlows == null ? ListUtils.EMPTY_LIST : fieldFlows);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("设置项目模版的字段列表")
    @DeleteMapping("field/{key}")
    @CheckAuthType(TokenAuthType.ALL)
    public BaseResponse deleteField(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                    @ApiParam(value = "字段标识", example = "1") @Valid @PathVariable String key) throws IOException {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        schemaCmdService.deleteField(id, key);
        return SUCCESS_RESPONSE;
    }

    // todo for status
    // 1. add
    // 2. update
    // 3. sort
    // 4. enable/disable(type,status)
    // 5. delete
    @Deprecated
    @ApiOperation("设置项目schema的状态列表")
    @PutMapping("statuses")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse setStatuses(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                    @Valid @RequestBody CardStatusesConf cardStatusesConf) {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        schemaCmdService.setStatuses(id, cardStatusesConf);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("新建项目schema状态")
    @PostMapping("statuses")
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse addStatus(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                  @Valid @RequestBody CardStatus cardStatus) {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        schemaCmdService.addStatus(id, cardStatus);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("更新项目schema的状态设置")
    @PutMapping("statuses/update")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse updateStatus(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                     @Valid @RequestBody CardStatus cardStatus) {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        schemaCmdService.updateStatus(id, cardStatus);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("移除项目schema状态; 5001错误：缺合法目标迁移状态")
    @DeleteMapping("statuses/{cardStatus}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse deleteStatus(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                  @PathVariable String cardStatus,
                                  @RequestBody(required = false) Map<String, String> toStatuses) throws Exception {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        schemaCmdService.deleteStatus(id, cardStatus, toStatuses == null ? MapUtils.EMPTY_MAP : toStatuses);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("更新项目schema的状态顺序")
    @PutMapping("statuses/sort")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse sortStatuses(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                     @RequestBody String[] statusKeys) {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        schemaCmdService.sortStatuses(id, Arrays.asList(statusKeys));
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("启用指定卡片类型的指定状态")
    @PutMapping("types/{cardType}/statuses/{cardStatus}/enable")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse enableStatus(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                     @ApiParam(value = "卡片类型") @PathVariable String cardType,
                                     @ApiParam(value = "卡片状态") @PathVariable String cardStatus) {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        schemaCmdService.enableStatus(id, cardType, cardStatus);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("关闭指定卡片类型的指定状态; 5001错误：缺合法目标迁移状态")
    @PutMapping("types/{cardType}/statuses/{cardStatus}/disable")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse disableStatus(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                      @ApiParam(value = "卡片类型", example = "1") @PathVariable String cardType,
                                      @ApiParam(value = "卡片状态") @PathVariable String cardStatus,
                                      @ApiParam(value = "目标迁移状态") @RequestParam(required = false) String toStatus) throws IOException {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        schemaCmdService.disableStatus(id, cardType, cardStatus, toStatus);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("设置项目schema下具体卡片类型的字段列表")
    @PutMapping("types/{cardType}/fields")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse setFields4Card(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                       @ApiParam(value = "卡片类型", example = "1") @PathVariable String cardType,
                                       @ApiParam(value = "字段配置") @RequestBody CardType.FieldConf[] fields) {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        schemaCmdService.setFields4Card(id, cardType, Arrays.asList(fields));
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("设置项目schema下具体卡片类型的状态列表")
    @PutMapping("types/{cardType}/statuses")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse setStatuses4Card(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                         @ApiParam(value = "卡片类型", example = "1") @PathVariable String cardType,
                                         @ApiParam(value = "状态配置") @RequestBody CardType.StatusConf[] statuses) {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        schemaCmdService.setStatuses4Card(id, cardType, Arrays.asList(statuses));
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("设置项目schema下具体卡片类型的状态自动流转")
    @PutMapping("types/{cardType}/autoStatusFlows")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse setAutoStatusFlows4Card(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                                @ApiParam(value = "卡片类型", example = "1") @PathVariable String cardType,
                                                @ApiParam(value = "自动状态流转配置") @RequestBody CardType.AutoStatusFlowConf[] autoStatusFlows) {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        schemaCmdService.setAutoStatusFlows4Card(id, cardType, Arrays.asList(autoStatusFlows));
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查看项目角色")
    @GetMapping("role")
    @CheckAuthType(TokenAuthType.READ)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<List<ProjectRole>> listRoles(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) {
        checkPermission(id, OperationType.PROJECT_MANAGE_READ);
        return success(schemaQueryService.getProjectRoleSchema(id).getRoles());
    }

    @ApiOperation("新建项目角色")
    @PostMapping("role")
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse addRole(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                @Valid @RequestBody ProjectRole role) throws IOException {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        schemaCmdService.addRole(id, role);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("更新项目的角色设置")
    @PutMapping("role/update")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse updateRole(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                   @Valid @RequestBody ProjectRole role) {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        schemaCmdService.updateRole(id, role);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("移除项目角色")
    @DeleteMapping("role/{roleKey}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse deleteRole(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                   @PathVariable String roleKey,
                                   @RequestBody(required = false) DeleteProjectRoleOptions options) throws Exception {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        schemaCmdService.deleteRole(id, roleKey, options);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("移动项目自定义角色位置")
    @PostMapping("role/customerRoleRank")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<List<ProjectRole>> customerRoleRank(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                                            @RequestBody UpdateRoleRankRequest request) {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        if (request.getSource() != RoleSource.CUSTOM) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "只支持自定义角色之间的顺序调整！");
        }
        schemaCmdService.customerRoleRank(id, request.getRoleKey(), request.getReferenceRoleKey(), request.getLocation());
        return success(schemaQueryService.getProjectRoleSchema(id).getRoles());
    }

    @ApiOperation("更新项目的工时设置")
    @PutMapping("workloadSetting")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse saveWorkloadSetting(
            @ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody ProjectWorkloadSetting workloadSetting) {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        schemaCmdService.saveWorkloadSetting(id, workloadSetting);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("获取项目的工时设置")
    @GetMapping("workloadSetting")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<ProjectWorkloadSetting> findWorkloadSetting(
            @ApiParam(value = "项目ID", example = "1") @PathVariable Long id) {
        checkHasProjectRead(id);
        return success(schemaQueryService.findProjectWorkloadSetting(id));
    }
}
