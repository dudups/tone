package com.ezone.ezproject.modules.log.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.transactional.AfterCommit;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.ProjectMember;
import com.ezone.ezproject.dal.mapper.PlanMapper;
import com.ezone.ezproject.es.dao.OperationLogDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardStatus;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.OperationLog;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectRole;
import com.ezone.ezproject.es.entity.ProjectRoleSchema;
import com.ezone.ezproject.es.entity.Role;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.service.CardHelper;
import com.ezone.ezproject.modules.common.EndpointHelper;
import com.ezone.ezproject.modules.common.OperationContext;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService;
import com.ezone.ezproject.modules.log.bean.LogOperationType;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
@Async
public class OperationLogCmdService {

    ProjectSchemaQueryService projectSchemaQueryService;
    CompanyProjectSchemaQueryService companyProjectSchemaQueryService;
    private PlanMapper planMapper;

    public static final String RESOURCE_ID = "_resourceId";
    public static final String CONTENT = "_content";

    private OperationLogDao operationLogDao;
    private EndpointHelper endpointHelper;

    @AfterCommit
    public void deleteCards(OperationContext opContext, List<Card> cards, Map<Long, Map<String, Object>> deleteCardDetails) {
        if (cards == null) {
            return;
        }
        for (Card card : cards) {
            deleteCard(opContext, card, deleteCardDetails.get(card.getId()));
        }
    }

    @AfterCommit
    public void deleteCard(OperationContext opContext, @Nonnull Card card, @Nonnull Map<String, Object> cardDetails) {
        addOperationLog(opContext, card.getProjectId(), card.getId().toString(),
                LogOperationType.CARD_UPDATE, String.format("删除%s卡片", CardHelper.cardPathHtml(card, endpointHelper, FieldUtil.getTitle(cardDetails))));
    }

    /**
     * 更新某具体卡片的类型。如将计划中某卡片由前端task转为后端task
     */
    @AfterCommit
    public void updateCardDetailType(OperationContext opContext, Project project, Long cardId, Map<String, Object> cardDetail, String source, String target) {
        addOperationLog(opContext, project.getId(), cardId.toString(), LogOperationType.CARD_UPDATE, String.format("将卡片%s类型从%s变更为%s", CardHelper.cardPathHtml(project, cardDetail, endpointHelper),
                source, target));
    }

    @AfterCommit
    public void updateCardTypeEnable(OperationContext opContext, Long companyId, Long projectId, CardType cardType, boolean enable) {
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(companyId);
        addOperationLog(opContext, projectId, cardType.getKey(), LogOperationType.CARD_TYPE_UPDATE, String.format("将卡片%s%s", companyCardSchema.findCardTypeName(cardType.getKey()), enable ? "开启" : "关闭"));
    }

    @AfterCommit
    public void deletePlan(OperationContext opContext, @Nonnull Plan plan) {
        addOperationLog(opContext, plan.getProjectId(), plan.getId().toString(), LogOperationType.PLAN_UPDATE, String.format("删除%s计划", plan.getName()));
    }

    @AfterCommit
    public void createSubPlan(OperationContext opContext, @Nonnull Plan plan) {
        if (plan.getParentId() > 0) {
            Plan parent = planMapper.selectByPrimaryKey(plan.getParentId());
            addOperationLog(opContext, plan.getProjectId(), plan.getId().toString(), LogOperationType.PLAN_UPDATE, String.format("对%s计划，增加了子计划%s", parent.getName(), plan.getName()));
        }
    }

    @AfterCommit
    public void updatePlan(OperationContext opContext, @Nonnull Plan plan, Map<String, Object> updateContent) {
        Map<String, String> fieldNames = new HashMap<>();
        fieldNames.put("name", "计划名");
        fieldNames.put("parentId", "父计划ID");
        fieldNames.put("startTime", "开始时间");
        fieldNames.put("endTime", "结束时间");
        if (updateContent != null && updateContent.size() > 0) {
            StringBuilder content = new StringBuilder();
            content.append("对").append(plan.getName()).append("计划的");
            List<String> names = updateContent.keySet().stream().map(fieldNames::get).collect(Collectors.toList());
            content.append(StringUtils.join(names, "、")).append("进行了修改。");
            addOperationLog(opContext, plan.getProjectId(), plan.getId().toString(), LogOperationType.PLAN_UPDATE, content.toString());
        }
    }

    @AfterCommit
    public void updateProjectMember(OperationContext opContext, Long projectId, @NotNull List<ProjectMember> deletedMembers, @NotNull List<ProjectMember> addMembers) {
        ProjectRoleSchema projectRoleSchema = projectSchemaQueryService.getProjectRoleSchema(projectId);
        for (ProjectMember member : addMembers) {
            Role role = projectRoleSchema.findRole(member.getRoleSource(), member.getRole());
            addOperationLog(opContext, projectId, projectId.toString(), LogOperationType.ROLE_MEMBER_UPDATE,
                    "添加 " + member.getUser() + " 到" + role.getName() + "中;");
        }
        for (ProjectMember member : deletedMembers) {
            Role role = projectRoleSchema.findRole(member.getRoleSource(), member.getRole());
            addOperationLog(opContext, projectId, projectId.toString(), LogOperationType.ROLE_MEMBER_UPDATE,
                    "从角色" + role.getName() + "中删除" + member.getUser());
        }
    }

    @AfterCommit
    public void addRole(OperationContext opContext, Long projectId, ProjectRole role) {
        addOperationLog(opContext, projectId, role.getKey(), LogOperationType.ROLE_UPDATE, "添加了" + role.getName() + "角色");
    }

    @AfterCommit
    public void deleteRole(OperationContext opContext, Long projectId, ProjectRole role) {
        addOperationLog(opContext, projectId, role.getKey(), LogOperationType.ROLE_UPDATE, "删除了" + role.getName() + "角色");
    }

    @AfterCommit
    public void updateProjectBaseInfo(OperationContext opContext, Long projectId, @Nonnull Map<String, Object> updateContent) {
        for (String key : updateContent.keySet()) {
            StringBuilder content = new StringBuilder();
            content.append("更新了项目管理空间的").append(key);
            if ("回收站清理周期".equals(key) || "归档计划清理周期".equals(key)) {
                addOperationLog(opContext, projectId, projectId.toString(), LogOperationType.PROJECT_CLEAN_UPDATE, content.toString());
            } else {
                addOperationLog(opContext, projectId, projectId.toString(), LogOperationType.PROJECT_BASE_UPDATE, content.toString());
            }
        }
    }

    @AfterCommit
    public void updateProjectSchemaStatus(OperationContext opContext, Long companyId, Long projectId, @Nonnull String cardTypeKey, String statusName, boolean isDelete) {
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(companyId);
        addOperationLog(opContext, projectId, statusName, LogOperationType.CARD_TYPE_STATUS_UPDATE,
                "为" + companyCardSchema.findCardTypeName(cardTypeKey) + (isDelete ? "删除" : "添加") + statusName + "状态");
    }

    @AfterCommit
    public void updateCardTypeWorkFlowEnd(OperationContext opContext, Long companyId, Long projectId, String typeKey, @Nonnull List<String> changeToEndStatuses,
                                          @Nonnull List<String> changeToNoEndStatuses) {
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(companyId);
        ProjectCardSchema projectCardSchema = projectSchemaQueryService.getProjectCardSchema(projectId);
        Map<String, CardStatus> statusMap = projectCardSchema.getStatuses().stream().collect(Collectors.toMap(CardStatus::getKey, cardStatus -> cardStatus));
        projectCardSchema.getTypes().stream().filter(cardType -> cardType.getKey().equals(typeKey)).findFirst().ifPresent(type -> {
            changeToEndStatuses.forEach(status ->
                    addOperationLog(opContext, projectId, type.getKey() + "-" + statusMap.get(status).getKey(), LogOperationType.CARD_TYPE_FLOW_UPDATE,
                            "将" + companyCardSchema.findCardTypeName(type.getKey()) + "的" + statusMap.get(status).getName() + "设置为结束状态")
            );
            changeToNoEndStatuses.forEach(status ->
                    addOperationLog(opContext, projectId, type.getKey() + "-" + statusMap.get(status).getKey(), LogOperationType.CARD_TYPE_FLOW_UPDATE,
                            "将" + companyCardSchema.findCardTypeName(type.getKey()) + "的" + statusMap.get(status).getName() + "设置为非结束状态")
            );
        });
    }

    @AfterCommit
    public void updateCardTypeWorkFlowNum(OperationContext opContext, Long companyId, Long projectId, CardType cardType, CardStatus currentCardStatus, @Nonnull List<CardType.StatusFlowConf> addFlowConfigs,
                                          @Nonnull List<CardType.StatusFlowConf> deleteFlowConfigs) {
        if (addFlowConfigs.isEmpty() && deleteFlowConfigs.isEmpty()) {
            return;
        }
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(companyId);
        String statusName = currentCardStatus.getName();
        ProjectCardSchema projectCardSchema = projectSchemaQueryService.getProjectCardSchema(projectId);
        Map<String, CardStatus> statusMap = projectCardSchema.getStatuses().stream().collect(Collectors.toMap(CardStatus::getKey, cardStatus -> cardStatus));
        projectCardSchema.getTypes().stream().filter(type -> type.getKey().equals(cardType.getKey())).findFirst().ifPresent(type -> {
            addFlowConfigs.forEach(flowConfig -> {
                String content = "将" + companyCardSchema.findCardTypeName(type.getKey()) + "中添加" + statusName + "可流转为" +
                        statusMap.get(flowConfig.getTargetStatus()).getName();
                addOperationLog(opContext, projectId, type.getKey() + "-" + statusName, LogOperationType.CARD_TYPE_FLOW_UPDATE, content);
            });
            deleteFlowConfigs.forEach(flowConfig -> {
                String content = "将" + companyCardSchema.findCardTypeName(type.getKey()) + "中删除" + statusName + "可流转为" +
                        statusMap.get(flowConfig.getTargetStatus()).getName();
                addOperationLog(opContext, projectId, type.getKey() + "-" + statusName, LogOperationType.CARD_TYPE_FLOW_UPDATE, content);
            });
        });
    }

    @AfterCommit
    public void updateCardTypeWorkFlowPermission(OperationContext opContext, Long companyId, Long projectId, CardType cardType, String statusName, CardType.StatusFlowConf newFlowConf) {
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(companyId);
        ProjectCardSchema projectCardSchema = projectSchemaQueryService.getProjectCardSchema(projectId);
        projectCardSchema.getTypes().stream().filter(type -> type.getKey().equals(cardType.getKey())).findFirst().ifPresent(type -> {
            StringBuilder content = new StringBuilder();
            content.append("将").append(companyCardSchema.findCardTypeName(type.getKey())).append("中的").append(statusName).append(">").append(newFlowConf.getTargetStatus());
            if (StringUtils.isEmpty(newFlowConf.getOpUserField())) {
                content.append("步骤的权限限制删除");
            } else {
                content.append("步骤的权限限制设置为")
                        .append(projectCardSchema.findCardField(newFlowConf.getOpUserField()).getName());
            }
            addOperationLog(opContext, projectId, cardType.getKey(), LogOperationType.CARD_TYPE_FLOW_UPDATE, content.toString());
        });
    }

    @AfterCommit
    public void updateCardTypeAutoWorkFlow(OperationContext opContext, Long projectId, Long companyId, CardType cardType) {
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(companyId);
        String cardTypeName = companyCardSchema.findCardTypeName(cardType.getKey());
        addOperationLog(opContext, projectId, cardType.getKey(), LogOperationType.CARD_TYPE_FLOW_UPDATE, "修改了" + cardTypeName + "自动流转记录");
    }

    @AfterCommit
    public void deleteCardField(OperationContext opContext, Long projectId, CardField cardField) {
        addOperationLog(opContext, projectId, projectId.toString(), LogOperationType.CARD_FIELD_UPDATE, "删除了" + cardField.getName() + "字段");
    }

    private void addOperationLog(OperationContext opContext, Long projectId, String resourceId, LogOperationType logOperationType, String content) {
        Map<String, Object> operationArgs = new HashMap<>(2);
        operationArgs.put(RESOURCE_ID, resourceId);
        operationArgs.put(CONTENT, content);
        try {
            addOperationLog(opContext, projectId, logOperationType, operationArgs);
        } catch (IOException e) {
            log.error("add operation log exception！");
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void addOperationLog(OperationContext opContext, Long projectId, LogOperationType logOperationType, Map<String, Object> operationArgs) throws IOException {
        String detail = (String) operationArgs.get(CONTENT);
        String resourceId = (String) operationArgs.get(RESOURCE_ID);
        OperationLog operationLog = OperationLog.builder()
                .id(IdUtil.generateId())
                .createTime(opContext.getTime())
                .projectId(projectId)
                .detail(detail)
                .operator(opContext.getUserName())
                .ip(opContext.getIp())
                .operateType(logOperationType.name())
                .resourceId(resourceId)
                .build();
        operationLogDao.saveOrUpdate(operationLog);
    }

    public void updateCardTypeStatusIsReadOnly(OperationContext opContext, Long companyId, Long projectId, CardType cardType, CardStatus currentCardStatus, CardType.StatusConf statusConf,
                                               CardType.StatusConf oldStatusConf) {
        if (statusConf.isReadOnly() == oldStatusConf.isReadOnly()) {
            return;
        }
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(companyId);
        String statusName = currentCardStatus.getName();

        String content = "将" + companyCardSchema.findCardTypeName(cardType.getKey()) + "中" + statusName + (statusConf.isReadOnly() ? "，设置为只读状态" : "设置的只读状态取消");
        addOperationLog(opContext, projectId, cardType.getKey() + "-" + statusName, LogOperationType.CARD_TYPE_FLOW_UPDATE, content);
    }

}
