package com.ezone.ezproject.modules.template.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.exception.ErrorCode;
import com.ezone.ezproject.es.dao.ProjectTemplateDetailDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardFieldFlow;
import com.ezone.ezproject.es.entity.CardStatus;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.MergedProjectRoleSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectMenuConfig;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.es.entity.ProjectRole;
import com.ezone.ezproject.es.entity.ProjectRoleSchema;
import com.ezone.ezproject.es.entity.ProjectSummaryTemplate;
import com.ezone.ezproject.es.entity.ProjectTemplateAlarm;
import com.ezone.ezproject.es.entity.ProjectTemplateDetail;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.modules.alarm.bean.CardAlarmItem;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService;
import com.ezone.ezproject.modules.project.bean.CardStatusesConf;
import com.ezone.ezproject.modules.project.bean.CardTypeConf;
import com.ezone.ezproject.modules.project.bean.ProjectMenuConfigReq;
import com.ezone.ezproject.modules.project.service.ProjectCardSchemaHelper;
import com.ezone.ezproject.modules.project.service.ProjectCardSchemaSettingHelper;
import com.ezone.ezproject.modules.project.service.ProjectMenuService;
import com.ezone.ezproject.modules.project.service.ProjectRoleSchemaHelper;
import com.ezone.ezproject.modules.project.service.ProjectRoleSchemaSettingHelper;
import com.ezone.ezproject.modules.project.service.ProjectSummaryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.boot.autoconfigure.klock.lock.Lock;
import org.springframework.boot.autoconfigure.klock.lock.LockFactory;
import org.springframework.boot.autoconfigure.klock.model.LockInfo;
import org.springframework.boot.autoconfigure.klock.model.LockType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
@AllArgsConstructor
public class ProjectTemplateSettingService {
    private ProjectTemplateDetailDao projectTemplateDetailDao;

    private ProjectTemplateService projectTemplateService;

    private ProjectCardSchemaSettingHelper schemaSettingHelper;

    private ProjectCardSchemaHelper schemaHelper;

    private ProjectRoleSchemaSettingHelper roleSchemaSettingHelper;

    private ProjectRoleSchemaHelper roleSchemaHelper;

    private CompanyProjectSchemaQueryService companyProjectSchemaQueryService;

    private LockFactory lockFactory;

    private CompanyService companyService;

    private ProjectMenuService projectMenuService;

    public void setTypes(Long id, List<CardTypeConf> cardTypeConfs) {
        setTemplateSchema(id, schema -> schemaSettingHelper.setTypes(id, schema, cardTypeConfs));
    }

    public void setFields(Long id, List<CardField> fields) {
        setTemplateSchema(id, schema -> schemaHelper.reGenerateCustomFieldKey(schemaSettingHelper.setFields(schema, fields)));
    }

    public void setFieldFlows(Long id, List<CardFieldFlow> fieldFlows) {
        setTemplateSchema(id, schema -> schemaSettingHelper.setFieldFlows(schema, fieldFlows));
    }

    /**
     * @deprecated 状态操作分解成addStatus enableStatus/disableStatus deleteStatus
     * @param id
     * @param cardStatusesConf
     */
    @Deprecated
    public void setStatuses(Long id, CardStatusesConf cardStatusesConf) {
        setTemplateSchema(id, schema -> schemaHelper.reGenerateCustomStatusKey(schemaSettingHelper.setStatuses(schema, cardStatusesConf)));
    }

    public void addStatus(Long id,  CardStatus cardStatus) {
        setTemplateSchema(id, schema ->
            schemaSettingHelper.addStatus(schema, cardStatus)
        );
    }

    public void updateStatus(Long id,  CardStatus cardStatus) {
        setTemplateSchema(id, schema ->
                schemaSettingHelper.updateStatus(schema, cardStatus)
        );
    }

    public void deleteStatus(Long id, String statusKey) {
        if (CardStatus.FIRST.equals(statusKey)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "不能删除系统内置初始状态！");
        }
        setTemplateSchema(id, schema -> {
            schema = schemaSettingHelper.deleteStatus(schema, statusKey);
            return schema;
        });
    }

    public void enableStatus(Long id, String cardTypeKey, String statusKey) {
        setTemplateSchema(id, schema -> {
            schema = schemaSettingHelper.enableStatus(schema, cardTypeKey, statusKey);
            return schema;
        });
    }

    public void disableStatus(Long id, String cardTypeKey, String statusKey) {
        if (CardStatus.FIRST.equals(statusKey)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "不能关闭系统内置初始状态！");
        }
        setTemplateSchema(id, schema -> {
            schema = schemaSettingHelper.disableStatus(schema, cardTypeKey, statusKey);
            return schema;
        });
    }

    public void sortStatuses(Long id, List<String> statusKeys) {
        setTemplateSchema(id, schema -> schemaSettingHelper.sortStatuses(schema, statusKeys));
    }


    public void setFields4Card(Long id, String cardType, List<CardType.FieldConf> fields) {
        setTemplateSchema(id, schema -> schemaSettingHelper.setFields4Card(schema, cardType, fields));
    }

    public void setStatuses4Card(Long id, String cardType, List<CardType.StatusConf> statuses) {
        setTemplateSchema(id, schema -> schemaSettingHelper.setStatuses4Card(id, schema, cardType, statuses));
    }

    public void setAutoStatusFlows4Card(Long id, String cardType, List<CardType.AutoStatusFlowConf> autoStatusFlowConfs) {
        setTemplateSchema(id, schema -> schemaSettingHelper.setAutoStatusFlows4Card(id, companyService.currentCompany(), schema, cardType, autoStatusFlowConfs));
    }

    public void setTemplateSummaryConfig(Long id, List<String> charts, List<String> rightCharts) {
        setTemplate(id, detail -> {
            ProjectSummaryTemplate projectSummaryTemplate = detail.getProjectSummaryConfigTemplates();
            if (null == projectSummaryTemplate) {
                projectSummaryTemplate = new ProjectSummaryTemplate();
                detail.setProjectSummaryConfigTemplates(new ProjectSummaryTemplate());
            }
            projectSummaryTemplate.setCharts(CollectionUtils.isEmpty(charts) ? ProjectSummaryService.CHARTS : charts);
            projectSummaryTemplate.setRightCharts(CollectionUtils.isEmpty(rightCharts) ? ProjectSummaryService.RIGHT_CHARTS : rightCharts);
            return detail;
        });
    }

    public void setTemplateMenu(Long id, ProjectMenuConfigReq req) {
        setTemplate(id, detail -> {
            ProjectMenuConfig projectMenu = detail.getProjectMenu();
            if (null == projectMenu) {
                projectMenu = projectMenuService.getDefaultMenuConfig();
            }
            projectMenu.setMenus(req.getMenus());
            projectMenu.setDefaultMenu(req.getDefaultMenu());
            projectMenu.setLastModifyTime(new Date());
            detail.setProjectMenu(projectMenu);
            return detail;
        });
    }

    public void setTemplate4Card(Long id, String cardType, Map<String, Object> json) {
        setTemplate(id, detail -> {
            Map<String, Map<String, Object>> projectCardTemplates = detail.getProjectCardTemplates();
            if (null == projectCardTemplates) {
                projectCardTemplates = new HashMap<>();
                detail.setProjectCardTemplates(projectCardTemplates);
            }
            projectCardTemplates.put(cardType, json);
            return detail;
        });
    }

    public void setRoles(Long id, List<ProjectRole> roles) {
        setTemplate(id, detail -> {
            ProjectRoleSchema schema = detail.getProjectRoleSchema();
            ProjectRoleSchema companyRoleSchema = companyProjectSchemaQueryService.getCompanyProjectRoleSchema(companyService.currentCompany());
            schema = roleSchemaHelper.mergeDefaultSchema(schema, RoleSource.CUSTOM, companyRoleSchema);
            schema = roleSchemaSettingHelper.saveRoles(schema, RoleSource.CUSTOM, roles);
            detail.setProjectRoleSchema(schema);
            return detail;
        });
    }

    public void updateRole(Long id, ProjectRole role) {
        setTemplate(id, detail -> {
            ProjectRoleSchema schema = detail.getProjectRoleSchema();
            ProjectRoleSchema companyRoleSchema = companyProjectSchemaQueryService.getCompanyProjectRoleSchema(companyService.currentCompany());
            MergedProjectRoleSchema mergedProjectRoleSchema = roleSchemaHelper.mergeDefaultSchema(schema, RoleSource.CUSTOM, companyRoleSchema);
            schema = roleSchemaSettingHelper.updateRole(mergedProjectRoleSchema, RoleSource.CUSTOM, role);
            mergedProjectRoleSchema.getRefCompanyRoles().remove(role);
            schema.getRoles().removeAll(mergedProjectRoleSchema.getRefCompanyRoles());
            ((MergedProjectRoleSchema) schema).setRefCompanyRoles(null);
            detail.setProjectRoleSchema(schema);
            return detail;
        });
    }

    public void deleteRole(Long id, String roleKey) {
        setTemplate(id, detail -> {
            ProjectRoleSchema schema = detail.getProjectRoleSchema();
            schema = roleSchemaSettingHelper.deleteRole(schema, RoleSource.CUSTOM, roleKey);
            detail.setProjectRoleSchema(schema);
            return detail;
        });
    }

    public void addRole(Long id, ProjectRole role) {
        role.setKey(null);
        setTemplate(id, detail -> {
            ProjectRoleSchema schema = detail.getProjectRoleSchema();
            if (schema == null) {
                schema = ProjectRoleSchema.builder().build();
            }
            schema = roleSchemaSettingHelper.addRole(schema, RoleSource.CUSTOM, role);
            detail.setProjectRoleSchema(schema);
            return detail;
        });
    }

    private void setTemplateSchema(Long id, Function<ProjectCardSchema, ProjectCardSchema> set) {
        setTemplate(id, detail -> {
            ProjectCardSchema schema = set.apply(schemaHelper.fillSysSchema(detail.getProjectCardSchema()));
            detail.setProjectCardSchema(schemaHelper.tripSysSchema(schema));
            return detail;
        });
    }

    private void setTemplate(Long id, Function<ProjectTemplateDetail, ProjectTemplateDetail> set) {
        Lock lock = lockTemplate(id);
        if (lock.acquire()) {
            try {
                ProjectTemplateDetail detail = projectTemplateService.getProjectTemplateDetail(id);
                set.apply(detail);
                projectTemplateDetailDao.saveOrUpdate(id, detail);
            } catch (CodedException e) {
                throw e;
            } catch (Exception e) {
                log.error(String.format("Update schema for project-template:[%s] exception!", id), e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            } finally {
                lock.release();
            }
        } else {
            throw new CodedException(HttpStatus.CONFLICT, "锁定模版冲突!");
        }
    }

    private Lock lockTemplate(Long id) {
        return lockFactory.getLock(new LockInfo(LockType.Reentrant,
                String.format("lock:project-template:setting:%s", id), 2, 2));
    }

    public void setTemplateProjectNoticeConfig(Long id, ProjectNoticeConfig noticeConfig) {
        setTemplate(id, detail -> {
            detail.setProjectNoticeConfig(noticeConfig);
            return detail;
        });
    }


    public void setAlarms(Long id, List<ProjectTemplateAlarm> alarms) {
        if (CollectionUtils.isNotEmpty(alarms)) {
            long count = alarms.stream().map(item -> item.getAlarmItem().getName()).distinct().count();
            if (count != alarms.size()) {
                throw new CodedException(ErrorCode.KEY_CONFLICT, "名称冲突!");
            }
        }
        setTemplate(id, detail -> {
            detail.setAlarms(alarms);
            return detail;
        });
    }
}
