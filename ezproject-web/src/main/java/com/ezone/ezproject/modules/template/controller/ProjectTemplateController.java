package com.ezone.ezproject.modules.template.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.bean.ListRequestBody;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.validate.Uniq;
import com.ezone.ezproject.dal.entity.ProjectTemplate;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardFieldFlow;
import com.ezone.ezproject.es.entity.CardStatus;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectMenuConfig;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.es.entity.ProjectRole;
import com.ezone.ezproject.es.entity.ProjectSummaryTemplate;
import com.ezone.ezproject.es.entity.ProjectTemplateAlarm;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.project.bean.CardStatusesConf;
import com.ezone.ezproject.modules.project.bean.CardTypeConf;
import com.ezone.ezproject.modules.project.bean.ProjectMenuConfigReq;
import com.ezone.ezproject.modules.project.bean.ProjectSummaryConfRequest;
import com.ezone.ezproject.modules.template.bean.CreateProjectTemplateRequest;
import com.ezone.ezproject.modules.template.bean.ProjectTemplateBean;
import com.ezone.ezproject.modules.template.bean.UpdateProjectTemplateRequest;
import com.ezone.ezproject.modules.template.service.ProjectTemplateService;
import com.ezone.ezproject.modules.template.service.ProjectTemplateSettingService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ApiOperation("项目模版操作")
@RestController
@RequestMapping("/project/project-template")
@Slf4j
@AllArgsConstructor
@Validated
public class ProjectTemplateController extends AbstractController {
    private ProjectTemplateService projectTemplateService;
    private ProjectTemplateSettingService projectTemplateSettingService;

    @ApiOperation("新建项目模版")
    @PostMapping
    @CheckAuthType(TokenAuthType.ALL)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<ProjectTemplate> create(@Valid @RequestBody CreateProjectTemplateRequest request) throws IOException {
        checkIsCompanyAdmin();
        return success(projectTemplateService.create(request));
    }

    @ApiOperation("更新项目模版基础信息")
    @PutMapping("{id}/basic")
    @CheckAuthType(TokenAuthType.ALL)
    public BaseResponse update(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                               @Valid @RequestBody UpdateProjectTemplateRequest request) {
        checkIsCompanyAdmin();
        projectTemplateService.update(id, request);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("设置当前公司所有项目模版")
    @PutMapping("all")
    @CheckAuthType(TokenAuthType.ALL)
    public BaseResponse<List<ProjectTemplateBean>> updateAll(@Valid @RequestBody List<ProjectTemplateBean> beans)
            throws IOException {
        checkIsCompanyAdmin();
        String user = userService.currentUserName();
        Long company = companyService.currentCompany();
        projectTemplateService.setProjectTemplateBeans(user, company, beans);
        return success(projectTemplateService.getProjectTemplateBeans(company, true));
    }

    @ApiOperation("设置项目模版的卡片类型列表")
    @PutMapping("{id}/schema/types")
    @CheckAuthType(TokenAuthType.ALL)
    public BaseResponse setTypes(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                 @Valid @RequestBody CardTypeConf[] cardTypeConfs) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.setTypes(id, Arrays.asList(cardTypeConfs));
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("设置项目模版的字段列表")
    @PutMapping("{id}/schema/fields")
    @CheckAuthType(TokenAuthType.ALL)
    public BaseResponse setFields(
            @ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody CardField[] fields) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.setFields(id, Arrays.asList(fields));
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("设置项目模版的字段关联")
    @PutMapping("{id}/schema/fieldFlows")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse setFieldFlows(
            @ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
            @RequestBody @Valid @Uniq(field = CardFieldFlow.FIELD_KEY, message = "触发字段不能重复！") List<CardFieldFlow> fieldFlows) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.setFieldFlows(id, fieldFlows == null ? ListUtils.EMPTY_LIST : fieldFlows);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("设置项目模版的概要信息列表")
    @PutMapping("{id}/schema/summaryConfig")
    public BaseResponse setSummaryConfig(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                         @Valid @RequestBody ProjectSummaryConfRequest summaryConfReq) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.setTemplateSummaryConfig(id, summaryConfReq.getCharts(), summaryConfReq.getRightCharts());
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("设置项目模版的概要信息列表")
    @PutMapping("{id}/schema/projectNoticeConfig")
    public BaseResponse setProjectNoticeConfig(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                               @Valid @RequestBody ProjectNoticeConfig noticeConfig) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.setTemplateProjectNoticeConfig(id, noticeConfig);
        return SUCCESS_RESPONSE;
    }

    // 转换为以下几个操作
    // 1. add
    // 2. update
    // 3. sort
    // 4. enable/disable(type,status)
    // 5. delete
    @ApiOperation("设置项目模版的状态列表")
    @PutMapping("{id}/schema/statuses")
    @CheckAuthType(TokenAuthType.ALL)
    @Deprecated
    public BaseResponse setStatuses(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                    @Valid @RequestBody CardStatusesConf cardStatusesConf) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.setStatuses(id, cardStatusesConf);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("启用指定卡片类型的指定状态")
    @PutMapping("{id}/schema/types/{cardType}/statuses/{cardStatus}/enable")
    @CheckAuthType(TokenAuthType.ALL)
    @Deprecated
    public BaseResponse enableStatus(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                     @ApiParam(value = "卡片类型") @PathVariable String cardType,
                                     @ApiParam(value = "卡片状态") @PathVariable String cardStatus) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.enableStatus(id, cardType, cardStatus);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("关闭指定卡片类型的指定状态")
    @PutMapping("{id}/schema/types/{cardType}/statuses/{cardStatus}/disable")
    @CheckAuthType(TokenAuthType.ALL)
    @Deprecated
    public BaseResponse disableStatus(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                      @ApiParam(value = "卡片类型") @PathVariable String cardType,
                                      @ApiParam(value = "卡片状态") @PathVariable String cardStatus) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.disableStatus(id, cardType, cardStatus);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("新建模板schema状态")
    @PostMapping("{id}/schema/statuses/add")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse addStatus(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                  @Valid @RequestBody CardStatus cardStatus) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.addStatus(id, cardStatus);
        return SUCCESS_RESPONSE;
    }

    @DeleteMapping("{id}/schema/statuses/{cardStatus}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse deleteStatus(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                     @PathVariable String cardStatus) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.deleteStatus(id, cardStatus);
        return SUCCESS_RESPONSE;
    }

    @PutMapping("{id}/schema/statuses/update")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse updateStatus(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                     @Valid @RequestBody CardStatus cardStatus) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.updateStatus(id, cardStatus);
        return SUCCESS_RESPONSE;
    }

    @PutMapping("{id}/schema/statuses/sort")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse sortStatuses(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                     @RequestBody String[] statusKeys) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.sortStatuses(id, Arrays.asList(statusKeys));
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("设置项目模版下具体卡片类型的字段列表")
    @PutMapping("{id}/schema/types/{cardType}/fields")
    @CheckAuthType(TokenAuthType.ALL)
    public BaseResponse setFields4Card(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                       @ApiParam(value = "卡片类型", example = "story") @PathVariable String cardType,
                                       @ApiParam(value = "字段配置") @RequestBody CardType.FieldConf[] fields) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.setFields4Card(id, cardType, Arrays.asList(fields));
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("设置项目模版下具体卡片类型的状态列表")
    @PutMapping("{id}/schema/types/{cardType}/statuses")
    @CheckAuthType(TokenAuthType.ALL)
    public BaseResponse setStatuses4Card(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                         @ApiParam(value = "卡片类型", example = "story") @PathVariable String cardType,
                                         @ApiParam(value = "状态配置") @RequestBody CardType.StatusConf[] statuses) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.setStatuses4Card(id, cardType, Arrays.asList(statuses));
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("设置项目模版下具体卡片类型的自动状态流转")
    @PutMapping("{id}/schema/types/{cardType}/autoStatusFlows")
    @CheckAuthType(TokenAuthType.ALL)
    public BaseResponse setAutoStatusFlows4Card(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                                @ApiParam(value = "卡片类型", example = "story") @PathVariable String cardType,
                                                @ApiParam(value = "自动状态流转配置") @RequestBody CardType.AutoStatusFlowConf[] autoStatusFlows) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.setAutoStatusFlows4Card(id, cardType, Arrays.asList(autoStatusFlows));
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("设置项目模版下具体卡片类型的模版")
    @PutMapping("{id}/schema/types/{cardType}/template")
    @CheckAuthType(TokenAuthType.ALL)
    public BaseResponse setStatuses4Card(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                         @ApiParam(value = "卡片类型", example = "story") @PathVariable String cardType,
                                         @ApiParam(value = "卡片模版json") @RequestBody Map<String, Object> json) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.setTemplate4Card(id, cardType, json);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("获取项目模版的角色列表")
    @GetMapping("{id}/schema/roles")
    @CheckAuthType(TokenAuthType.ALL)
    public BaseResponse<List<ProjectRole>> getRoles(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id) {
        checkWriteProjectTemplate(id);
        return success(projectTemplateService.getRoleSchema(id).getRoles());
    }

    @ApiOperation("设置项目模版的角色列表")
    @PutMapping("{id}/schema/roles")
    @CheckAuthType(TokenAuthType.ALL)
    public BaseResponse setRoles(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                 @Valid @RequestBody ProjectRole[] roles) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.setRoles(id, Arrays.asList(roles));
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("更新项目模版的角色")
    @PutMapping("{id}/schema/role/update")
    @CheckAuthType(TokenAuthType.ALL)
    public BaseResponse updateRole(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                   @Valid @RequestBody ProjectRole role) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.updateRole(id, role);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("新增项目模版的角色")
    @PostMapping("{id}/schema/role")
    @CheckAuthType(TokenAuthType.ALL)
    public BaseResponse addRole(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                @Valid @RequestBody ProjectRole role) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.addRole(id, role);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("删除项目模版的角色")
    @DeleteMapping("{id}/schema/role/{roleKey}")
    @CheckAuthType(TokenAuthType.ALL)
    public BaseResponse deleteRole(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                   @PathVariable String roleKey) throws Exception {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.deleteRole(id, roleKey);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询当前公司所有项目模版详细信息")
    @GetMapping("all")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<ProjectTemplateBean>> selectAll()
            throws IOException {
        checkIsCompanyAdmin();
        return selectAllPublic();
    }

    @ApiOperation("查询当前公司所有项目模版详细信息")
    @GetMapping("all/public")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<ProjectTemplateBean>> selectAllPublic()
            throws IOException {
        String user = userService.currentUserName();
        Long company = companyService.currentCompany();
        List<ProjectTemplateBean> beans = projectTemplateService.getProjectTemplateBeans(company, true);
        if (CollectionUtils.isEmpty(beans)) {
            projectTemplateService.initSysProjectTemplates(user, company);
            beans = projectTemplateService.getProjectTemplateBeans(company, true);
        }
        return success(beans);
    }

    @ApiOperation("查询当前公司可用的项目模版列表")
    @GetMapping
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<ProjectTemplate>> select() {
        checkCreateProject();
        return selectPublic();
    }

    @ApiOperation("查询当前公司可用的项目模版列表")
    @GetMapping("public")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<ProjectTemplate>> selectPublic() {
        String user = userService.currentUserName();
        Long company = companyService.currentCompany();
        List<ProjectTemplate> templates = projectTemplateService.getProjectTemplates(company);
        if (CollectionUtils.isEmpty(templates)) {
            return success(projectTemplateService.initSysProjectTemplates(user, company));
        }
        return success(templates);
    }

    @ApiOperation("获取项目模版卡片Schema")
    @GetMapping("{id}/schema")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<ProjectCardSchema> getCardSchema(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id) {
        checkReadProjectTemplate(id);
        return success(projectTemplateService.getSchema(id));
    }

    @ApiOperation("获取项目模版概要信息配置")
    @GetMapping("{id}/projectSummaryConfig")
    public BaseResponse<ProjectSummaryTemplate> getProjectSummaryConfig(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id) {
        checkReadProjectTemplate(id);
        ProjectSummaryTemplate template = projectTemplateService.getProjectSummaryTemplate(id);
        return success(template == null ? ProjectSummaryTemplate.DEFAULT : template);
    }

    @ApiOperation("获取项目模版通知配置")
    @GetMapping("{id}/projectNoticeConfig")
    public BaseResponse<ProjectNoticeConfig> getProjectNoticeConfig(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id) {
        checkReadProjectTemplate(id);
        ProjectNoticeConfig projectNoticeConfig = projectTemplateService.getProjectNoticeConfig(id);
        return success(projectNoticeConfig);
    }

    @ApiOperation("获取项目模版下具体卡片类型的模版")
    @GetMapping("{id}/schema/types/{cardType}/template")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<Map<String, Object>> getCardTemplate(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                                             @ApiParam(value = "卡片类型", example = "story") @PathVariable String cardType) {
        checkReadProjectTemplate(id);
        Map<String, Map<String, Object>> templates = projectTemplateService.getProjectTemplateDetail(id).getProjectCardTemplates();
        if (templates == null) {
            return success(null);
        }
        return success(projectTemplateService.getTemplate4Card(id, cardType));
    }

    @ApiOperation("设置项目模版的菜单列表")
    @GetMapping("{id}/menus")
    public BaseResponse<ProjectMenuConfig> getMenus(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id) {
        checkReadProjectTemplate(id);
        return success(projectTemplateService.getMenus(id));
    }

    @ApiOperation("设置项目模版的菜单列表")
    @PutMapping("{id}/menus")
    public BaseResponse setMenus(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id,
                                 @RequestBody ProjectMenuConfigReq openMenus) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.setTemplateMenu(id, openMenus);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("删除项目模版")
    @DeleteMapping("{id}")
    @CheckAuthType(TokenAuthType.ALL)
    public BaseResponse delete(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id) {
        ProjectTemplate template = projectTemplateService.select(id);
        if (null == template) {
            return SUCCESS_RESPONSE;
        }
        checkIsCompanyAdmin();
        projectTemplateService.deleteTemplate(template);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("获取项目模版预警设置")
    @GetMapping("{id}/projectAlarmConfig")
    public BaseResponse<List<ProjectTemplateAlarm>> getProjectAlarmConfig(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id) {
        checkReadProjectTemplate(id);
        return success(projectTemplateService.getAlarmConfig(id));
    }

    @ApiOperation("更新项目模版预警设置")
    @PutMapping("{id}/projectAlarmConfig")
    public BaseResponse updateProjectAlarmConfig(@ApiParam(value = "项目模版ID", example = "1") @PathVariable Long id, @Valid @RequestBody ListRequestBody<ProjectTemplateAlarm> alarms) {
        checkWriteProjectTemplate(id);
        projectTemplateSettingService.setAlarms(id, alarms.getList());
        return SUCCESS_RESPONSE;
    }

    private BaseResponse checkWriteProjectTemplate(Long id) {
        ProjectTemplate template = projectTemplateService.select(id);
        if (null == template) {
            throw CodedException.NOT_FOUND;
        }
        checkIsCompanyAdmin();
        return SUCCESS_RESPONSE;
    }

    private BaseResponse checkReadProjectTemplate(Long id) {
        ProjectTemplate template = projectTemplateService.select(id);
        if (null == template) {
            throw CodedException.NOT_FOUND;
        }
        checkCreateProject();
        return SUCCESS_RESPONSE;
    }
}
