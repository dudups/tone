package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.common.EsUtil;
import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.exception.ErrorCode;
import com.ezone.ezproject.common.template.VelocityTemplate;
import com.ezone.ezproject.configuration.CacheManagers;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.dao.CardEventDao;
import com.ezone.ezproject.es.dao.ProjectCardSchemaDao;
import com.ezone.ezproject.es.dao.ProjectRoleSchemaDao;
import com.ezone.ezproject.es.dao.ProjectWorkloadSettingDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardFieldFlow;
import com.ezone.ezproject.es.entity.CardFieldValue;
import com.ezone.ezproject.es.entity.CardFieldValueFlow;
import com.ezone.ezproject.es.entity.CardStatus;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.MergedProjectRoleSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectRole;
import com.ezone.ezproject.es.entity.ProjectRoleSchema;
import com.ezone.ezproject.es.entity.ProjectWorkloadSetting;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.es.entity.enums.Source;
import com.ezone.ezproject.es.util.EsIndexUtil;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bean.CardBean;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.bean.query.Exist;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.card.service.CardCmdService;
import com.ezone.ezproject.modules.card.service.CardQueryService;
import com.ezone.ezproject.modules.common.OperationContext;
import com.ezone.ezproject.modules.company.rank.RankLocation;
import com.ezone.ezproject.modules.company.service.RoleRankCmdService;
import com.ezone.ezproject.modules.log.service.OperationLogCmdService;
import com.ezone.ezproject.modules.permission.UserProjectPermissionsService;
import com.ezone.ezproject.modules.project.bean.CardStatusesConf;
import com.ezone.ezproject.modules.project.bean.CardTypeConf;
import com.ezone.ezproject.modules.project.bean.DeleteProjectRoleOptions;
import com.ezone.ezproject.modules.project.bean.RoleKeySource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.boot.autoconfigure.klock.lock.Lock;
import org.springframework.boot.autoconfigure.klock.lock.LockFactory;
import org.springframework.boot.autoconfigure.klock.model.LockInfo;
import org.springframework.boot.autoconfigure.klock.model.LockType;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectSchemaCmdService {
    private static final String ES_MAPPING_TEMPLATE_FILE = "/es/vm-card-mapping.yaml";

    private ProjectCardSchemaDao projectCardSchemaDao;
    private ProjectRoleSchemaDao projectRoleSchemaDao;
    private ProjectWorkloadSettingDao projectWorkloadSettingDao;

    private ProjectSchemaQueryService schemaQueryService;

    private ProjectCardSchemaHelper projectCardSchemaHelper;
    private CompanyService companyService;

    private RestHighLevelClient es;

    private LockFactory lockFactory;

    private ProjectCardSchemaSettingHelper schemaSettingHelper;

    private ProjectRoleSchemaSettingHelper roleSchemaSettingHelper;

    private CardQueryService cardQueryService;
    private CardCmdService cardCmdService;

    private UserService userService;

    private UserProjectPermissionsService userProjectPermissionsService;
    private ProjectMemberCmdService projectMemberCmdService;
    private ProjectMemberQueryService projectMemberQueryService;
    private ProjectSchemaQueryService projectSchemaQueryService;
    private RoleRankCmdService roleRankCmdService;

    private OperationLogCmdService operationLogCmdService;

    private CardDao cardDao;
    private CardEventDao cardEventDao;


    @Transactional(rollbackFor = Exception.class)
    public void setProjectCardSchema(Long projectId, ProjectCardSchema schema) throws IOException {
        projectCardSchemaHelper.checkSchema(schema);
        projectCardSchemaHelper.tripSysSchema(schema);
        projectCardSchemaHelper.generateCustomFieldKey(schema);
        projectCardSchemaHelper.generateCustomStatusKey(schema);
        projectCardSchemaDao.saveOrUpdate(projectId, schema);
        // List<CardField> fields = projectCardSchemaHelper.fillSysSchema(schema).getFields();
        // setEsDataMappingForCard(projectId, fields);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setProjectRoleSchema(Long projectId, ProjectRoleSchema schema) throws IOException {
        if (schema == null) {
            return;
        }
        schema = roleSchemaSettingHelper.mergeRoles(schema, RoleSource.CUSTOM, schema.getRoles());
        projectRoleSchemaDao.saveOrUpdate(projectId, schema);
    }

    @CacheEvict(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:ProjectSchemaService.getProjectCardSchema", "ProjectSchemaService.getProjectWorkloadSetting"},
            key = "#projectId"
    )
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long projectId) throws IOException {
        projectCardSchemaDao.delete(projectId);
        projectRoleSchemaDao.delete(projectId);
        projectWorkloadSettingDao.delete(projectId);
    }

    public void setTypes(Long id, List<CardTypeConf> cardTypeConfs) {
        setSchema(id, schema -> schemaSettingHelper.setTypes(id, schema, cardTypeConfs));
    }

    public void setFields(Long id, List<CardField> fields, Boolean isMergeCustomField) {
        setSchema(id, schema -> schemaSettingHelper.setFields(schema, schema.mergeFields(fields, isMergeCustomField)));
    }

    public void setFieldFlows(Long id, List<CardFieldFlow> fieldFlows) {
        setSchema(id, schema -> schemaSettingHelper.setFieldFlows(schema, fieldFlows));
    }

    // Deprecated since 20201203: 决定卡片和项目设置保证强一致性，故状态关闭后必须对历史卡片必须指定迁移状态并迁移。
    // 1. 整体提交校验和需要用户补充的点过多，所以写操作拆解;
    // 2. 同时卡片类型禁用后仅禁新建，故类型设置和校验需照旧；
    @Deprecated
    public void setStatuses(Long id, CardStatusesConf cardStatusesConf) {
        setSchema(id, schema -> schemaSettingHelper.setStatuses(schema, cardStatusesConf));
    }

    public void addStatus(Long id, CardStatus cardStatus) {
        setSchema(id, schema -> schemaSettingHelper.addStatus(schema, cardStatus));
    }

    public void updateStatus(Long id, CardStatus cardStatus) {
        setSchema(id, schema -> schemaSettingHelper.updateStatus(schema, cardStatus));
    }

    public void sortStatuses(Long id, List<String> statusKeys) {
        setSchema(id, schema -> schemaSettingHelper.sortStatuses(schema, statusKeys));
    }

    public void enableStatus(Long id, String cardTypeKey, String statusKey) {
        setSchema(id, schema -> {
            schema = schemaSettingHelper.enableStatus(schema, cardTypeKey, statusKey);
            operationLogCmdService.updateProjectSchemaStatus(OperationContext.instance(userService.currentUserName()), companyService.currentCompany(), id, cardTypeKey, schema.findCardStatus(statusKey).getName(), false);
            return schema;
        });
    }

    public void disableStatus(Long id, String cardTypeKey, String statusKey, String toStatusKey) throws IOException {
        if (CardStatus.FIRST.equals(statusKey)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "不能关闭系统内置初始状态！");
        }
        long count = cardQueryService.count(
                Eq.builder().field(CardField.PROJECT_ID).value(String.valueOf(id)).build(),
                Eq.builder().field(CardField.TYPE).value(cardTypeKey).build(),
                Eq.builder().field(CardField.STATUS).value(statusKey).build());
        setSchema(id, schema -> {
            schema = schemaSettingHelper.disableStatus(schema, cardTypeKey, statusKey);
            operationLogCmdService.updateProjectSchemaStatus(OperationContext.instance(userService.currentUserName()), companyService.currentCompany(), id, cardTypeKey, schema.findCardStatus(statusKey).getName(), true);
            if (count > 0) {
                if (null == schema.findCardType(cardTypeKey).findStatusConf(toStatusKey)) {
                    throw new CodedException(ErrorCode.REQUIRED_MIGRATE_STATUS, "卡片目标变更状态不存在！");
                } else {
                    cardCmdService.asyncMigrateForCloseStatus(
                            OperationContext.instance(userService.currentUserName()),
                            id,
                            cardTypeKey,
                            schema,
                            schema.findCardStatus(statusKey),
                            schema.findCardStatus(toStatusKey));
                }
            }
            return schema;
        });
    }

    public void deleteStatus(Long id, String statusKey, @NotNull Map<String, String> toStatuses) throws Exception {
        if (CardStatus.FIRST.equals(statusKey)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "不能删除系统内置初始状态！");
        }
        Map<String, Long> counts = cardQueryService.countGroupBy(CardField.TYPE, projectCardSchemaHelper.getTypesSize(),
                Eq.builder().field(CardField.PROJECT_ID).value(String.valueOf(id)).build(),
                Eq.builder().field(CardField.STATUS).value(statusKey).build());
        setSchema(id, schema -> {
            Map<String, CardStatus> oldStatuses = schema.getStatuses().stream().collect(Collectors.toMap(CardStatus::getKey, s -> s));
            schema = schemaSettingHelper.deleteStatus(schema, statusKey);
            for (Map.Entry<String, Long> e : counts.entrySet()) {
                if (null == schema.findCardType(e.getKey()).findStatusConf(toStatuses.get(e.getKey()))) {
                    throw new CodedException(ErrorCode.REQUIRED_MIGRATE_STATUS, "卡片目标变更状态不存在！", counts.keySet());
                }
            }
            OperationContext opContext = OperationContext.instance(userService.currentUserName());
            for (Map.Entry<String, Long> e : counts.entrySet()) {
                cardCmdService.asyncMigrateForCloseStatus(
                        opContext,
                        id,
                        e.getKey(),
                        schema,
                        oldStatuses.get(statusKey),
                        schema.findCardStatus(toStatuses.get(e.getKey())));
            }
            return schema;
        });
    }

    public void setFields4Card(Long id, String cardType, List<CardType.FieldConf> fields) {
        setSchema(id, schema -> schemaSettingHelper.setFields4Card(schema, cardType, fields));
    }

    public void setStatuses4Card(Long id, String cardType, List<CardType.StatusConf> statuses) {
        setSchema(id, schema -> {
            Set<String> toEndStatuses = statuses.stream().filter(s -> s.isEnd()).map(s -> s.getKey()).collect(Collectors.toSet());
            List<String> changeToEndStatuses = new ArrayList<>();
            List<String> changeToNoEndStatuses = new ArrayList<>();
            for (CardType.StatusConf status : schema.findCardType(cardType).getStatuses()) {
                if (status.isEnd()) {
                    if (!toEndStatuses.contains(status.getKey())) {
                        changeToNoEndStatuses.add(status.getKey());
                    }
                } else {
                    if (toEndStatuses.contains(status.getKey())) {
                        changeToEndStatuses.add(status.getKey());
                    }
                }
            }
            schema = schemaSettingHelper.setStatuses4Card(id, schema, cardType, statuses);
            cardCmdService.asyncChangeStatusIsEnd(
                    OperationContext.instance(userService.currentUserName()),
                    companyService.currentCompany(),
                    id,
                    cardType,
                    changeToEndStatuses,
                    changeToNoEndStatuses
            );
            return schema;
        });
    }

    public void setAutoStatusFlows4Card(Long id, String cardType, List<CardType.AutoStatusFlowConf> autoStatusFlowConfs) {
        setSchema(id, schema -> schemaSettingHelper.setAutoStatusFlows4Card(id, companyService.currentCompany(), schema, cardType, autoStatusFlowConfs));
    }

    private void setSchema(Long projectId, Function<ProjectCardSchema, ProjectCardSchema> set) {
        Lock lock = lockSchema(projectId);
        if (lock.acquire()) {
            try {
                //清除缓存-解决如dev环境中schema已经变化，但设置时获取到还是旧的，导致重新设置后，缓存中及es中保存的都会变成旧值的问题。
                schemaQueryService.cleanProjectCardSchemaCache(projectId);
                ProjectCardSchema schema = schemaQueryService.getProjectCardSchema(projectId);
                set.apply(schema);
                // List<CardField> fields = schema.getFields();
                projectCardSchemaDao.saveOrUpdate(projectId, projectCardSchemaHelper.tripSysSchema(schema));
                // setEsDataMappingForCard(projectId, fields);
            } catch (CodedException e) {
                throw e;
            } catch (Exception e) {
                log.error(String.format("Update schema for project:[%s] exception!", projectId), e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            } finally {
                lock.release();
            }
        } else {
            throw new CodedException(HttpStatus.CONFLICT, "Lock for project schema update fail!");
        }
    }

    /**
     * 把当前企业级角色持久化到项目级别，企业级角色的"操作权限"的这部分设置之后将由在本项目独立维护
     *
     * @param id
     * @param roleKeys
     */
    @Async
    public void forkCompanyRole(Long id, List<String> roleKeys) {
        setRoleSchema(id, schema -> {
            schema.getRefCompanyRoles().removeIf(r -> roleKeys.contains(r.getKey()));
            return schema;
        });
    }

    public void addRole(Long id, ProjectRole role) throws IOException {
        role.setKey(null);
        setRoleSchema(id, schema -> {
            String nextRank = roleRankCmdService.nextRank(schema);
            role.setRank(nextRank);
            return roleSchemaSettingHelper.addRole(schema, RoleSource.CUSTOM, role);
        });
        operationLogCmdService.addRole(OperationContext.instance(userService.currentUserName()), id, role);
    }

    public void updateRole(Long id, ProjectRole role) {
        setRoleSchema(id, schema -> {
            roleSchemaSettingHelper.updateRole(schema, RoleSource.CUSTOM, role);
            schema.getRefCompanyRoles().remove(role);
            return schema;
        });
    }

    public void deleteRole(Long id, String roleKey, DeleteProjectRoleOptions deleteOptions) {
        ProjectRole role = (ProjectRole) schemaQueryService.getProjectRoleSchema(id).findRole(RoleSource.CUSTOM, roleKey);
        if (role == null) {
            throw CodedException.NOT_FOUND;
        }
        setRoleSchema(id, schema -> {
            RoleKeySource sourceTargetRole = RoleKeySource.builder().role(roleKey).roleSource(RoleSource.CUSTOM).build();
            boolean hasMember = projectMemberQueryService.hasMember(id, RoleSource.CUSTOM, roleKey);
            if (deleteOptions == null && hasMember) {
                throw new CodedException(ErrorCode.NEED_MIGRATE_ROLE_USER, "需要将此角色下的用户迁移到其他角色下");
            }
            if (deleteOptions != null && deleteOptions.isMigrate()) {
                if (hasMember && (deleteOptions.getMigrateToRole() == null || deleteOptions.getMigrateToRole().getRole() == null)) {
                    throw new CodedException(HttpStatus.BAD_REQUEST, "需指定迁移的角色信息");
                }
                if (hasMember && deleteOptions.getMigrateToRole().getRole().equals(roleKey) && deleteOptions.getMigrateToRole().getRoleSource().equals(RoleSource.CUSTOM)) {
                    throw new CodedException(HttpStatus.BAD_REQUEST, "不能迁移到自身");
                }
                //迁移角色成员，可能还有角色中相同成员未迁移
                projectMemberCmdService.migrateRoleUsers(id, sourceTargetRole, deleteOptions.getMigrateToRole());
            }
            //删除角色成员
            projectMemberCmdService.deleteByProjectRole(id, RoleSource.CUSTOM, roleKey);
            return roleSchemaSettingHelper.deleteRole(schema, RoleSource.CUSTOM, roleKey);
        });
        operationLogCmdService.deleteRole(OperationContext.instance(userService.currentUserName()), id, role);
    }

    private Lock lockSchema(Long projectId) {
        return lockFactory.getLock(new LockInfo(LockType.Reentrant,
                String.format("lock:project:setting:%s", projectId), 2, 2));
    }

    private void setRoleSchema(Long projectId, Function<MergedProjectRoleSchema, ProjectRoleSchema> set) {
        Lock lock = lockRoleSchema(projectId);
        if (lock.acquire()) {
            try {
                MergedProjectRoleSchema schema = schemaQueryService.getProjectRoleSchema(projectId);
                set.apply(schema);
                schema.getRoles().removeAll(schema.getRefCompanyRoles());
                projectRoleSchemaDao.saveOrUpdate(projectId, ProjectRoleSchema.builder()
                        .roles(schema.getRoles())
                        .maxRank(schema.getMaxRank())
                        .build());
                String user = userService.currentUserName();
                userProjectPermissionsService.cacheEvict(projectId, user);
            } catch (CodedException e) {
                throw e;
            } catch (Exception e) {
                log.error(String.format("Update role schema for project:[%s] exception!", projectId), e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            } finally {
                lock.release();
            }
        } else {
            throw new CodedException(HttpStatus.CONFLICT, "Lock for project role schema update fail!");
        }
    }

    public void saveWorkloadSetting(Long projectId, ProjectWorkloadSetting workloadSetting) {
        Lock lock = lockWorkloadSetting(projectId);
        if (lock.acquire()) {
            try {
                projectWorkloadSettingDao.saveOrUpdate(projectId, workloadSetting);
            } catch (CodedException e) {
                throw e;
            } catch (Exception e) {
                log.error(String.format("Update project workload setting for project:[%s] exception!", projectId), e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            } finally {
                lock.release();
            }
        } else {
            throw new CodedException(HttpStatus.CONFLICT, "Lock for project workload setting update fail!");
        }
    }

    private Lock lockRoleSchema(Long projectId) {
        return lockFactory.getLock(new LockInfo(LockType.Reentrant,
                String.format("lock:project:role-setting:%s", projectId), 2, 2));
    }

    private Lock lockWorkloadSetting(Long projectId) {
        return lockFactory.getLock(new LockInfo(LockType.Reentrant,
                String.format("lock:project:workload-setting:%s", projectId), 2, 2));
    }

    private void setEsDataMappingForCard(Long projectId, List<CardField> fields) throws IOException {
        setEsDataMappingForCard(EsIndexUtil.indexForCard(projectId), fields);
        setEsDataMappingForCard(EsIndexUtil.indexForCardTemplate(projectId), fields);
    }

    private void setEsDataMappingForCard(String index, List<CardField> fields) throws IOException {
        PutMappingRequest putMappingRequest = new PutMappingRequest(index);
        // request.source(xContentBuilder(fields));
        String yaml = VelocityTemplate.render("fields", fields, ES_MAPPING_TEMPLATE_FILE);
        putMappingRequest.source(yaml, XContentType.YAML);
        try {
            es.indices().putMapping(putMappingRequest, EsUtil.REQUEST_OPTIONS);
        } catch (ElasticsearchStatusException e) {
            if (RestStatus.NOT_FOUND.equals(e.status())) {
                CreateIndexRequest createIndexRequest = new CreateIndexRequest(index);
                createIndexRequest.mapping(yaml, XContentType.YAML);
                es.indices().create(createIndexRequest, EsUtil.REQUEST_OPTIONS);
            }
        }
    }

    private XContentBuilder xContentBuilder(List<CardField> fields) throws IOException {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        {
            builder.startObject("properties");
            for (CardField field : fields) {
                builder.startObject(field.getKey());
                {
                    builder.field("type", field.getValueType().getEsDataType());
                }
                builder.endObject();
            }
            {
                builder.startObject("attachments");
                {
                    builder.field("type", "nested");
                    builder.startObject("properties");
                    {
                        builder.startObject("fileName");
                        {
                            builder.field("type", "keyword");
                        }
                        builder.endObject();
                        builder.startObject("storagePath");
                        {
                            builder.field("type", "keyword");
                        }
                        builder.endObject();
                    }
                    builder.endObject();
                }
                builder.endObject();
            }
            builder.endObject();
        }
        builder.endObject();
        return builder;
    }

    public void deleteField(Long projectId, String key) throws IOException {
        ProjectCardSchema cardSchema = projectSchemaQueryService.getProjectCardSchema(projectId);
        CardField cardField = cardSchema.findCardField(key);
        if (cardField == null) {
            throw new CodedException(HttpStatus.NOT_FOUND, "找不到要删除的字段！");
        } else if (Source.SYS.equals(cardField.getSource())) {
            throw new CodedException(HttpStatus.NOT_FOUND, "系统字段不能被删除！");
        }
        List<Query> queries = Arrays.asList(Exist.builder().field(key).build(), Eq.builder().field(CardField.PROJECT_ID).value(projectId.toString()).build());
        SearchEsRequest searchRequest = SearchEsRequest.builder().queries(queries).fields(new String[]{CardField.SEQ_NUM}).build();
        TotalBean<CardBean> totalBean = cardDao.search(searchRequest, 1, 1);
        if (totalBean.getTotal() > 0) {
            throw new CodedException(HttpStatus.NOT_FOUND, "字段已被卡片使用，无法删除此字段");
        }

        long count = cardEventDao.countByCardField(projectId, key);
        if (count > 0) {
            throw new CodedException(HttpStatus.NOT_FOUND, "字段已被卡片使用过，无法删除此字段");
        }

        List<CardField> fields = cardSchema.getFields();
        fields.removeIf(next -> next.getKey().equals(key));
        setFields(projectId, fields, false);
        List<CardFieldFlow> fieldFlows = cardSchema.getFieldFlows();
        if (CollectionUtils.isNotEmpty(fieldFlows)) {
            cardSchema.setFieldFlows(fieldFlows.stream()
                    .filter(fieldFlow -> {
                        if (key.equals(fieldFlow.getFieldKey())) {
                            return false;
                        }
                        List<CardFieldValueFlow> flows = fieldFlow.getFlows().stream()
                                .filter(flow -> {
                                    List<CardFieldValue> targetFieldValues = flow.getTargetFieldValues().stream()
                                            .filter(target -> !key.contains(target.getFieldKey()))
                                            .collect(Collectors.toList());
                                    if (CollectionUtils.isEmpty(targetFieldValues)) {
                                        return false;
                                    }
                                    flow.setTargetFieldValues(targetFieldValues);
                                    return true;
                                })
                                .collect(Collectors.toList());
                        if (CollectionUtils.isEmpty(flows)) {
                            return false;
                        }
                        fieldFlow.setFlows(flows);
                        return true;
                    })
                    .collect(Collectors.toList()));
        }
        operationLogCmdService.deleteCardField(OperationContext.instance(userService.currentUserName()), projectId, cardField);
    }


    public void customerRoleRank(Long id, String roleKey, String referenceRoleKey, RankLocation location) {
        setRoleSchema(id, schema -> (ProjectRoleSchema) roleRankCmdService.roleRank(schema, roleKey, referenceRoleKey, location, RoleSource.CUSTOM));
    }
}
