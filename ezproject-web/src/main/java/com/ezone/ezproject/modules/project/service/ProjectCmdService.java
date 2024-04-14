package com.ezone.ezproject.modules.project.service;

import com.ezone.ezbase.iam.bean.mq.CompanyAuditMessage;
import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.configuration.CacheManagers;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.ProjectDomain;
import com.ezone.ezproject.dal.entity.RelPortfolioProject;
import com.ezone.ezproject.dal.entity.RelPortfolioProjectExample;
import com.ezone.ezproject.dal.mapper.ProjectDomainMapper;
import com.ezone.ezproject.dal.mapper.ProjectMapper;
import com.ezone.ezproject.dal.mapper.RelPortfolioProjectMapper;
import com.ezone.ezproject.es.dao.ProjectExtendDao;
import com.ezone.ezproject.es.dao.ProjectRoleSchemaDao;
import com.ezone.ezproject.es.entity.CompanyProjectSchema;
import com.ezone.ezproject.es.entity.ProjectAlarmExt;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectField;
import com.ezone.ezproject.es.entity.ProjectMenuConfig;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.es.entity.ProjectRoleSchema;
import com.ezone.ezproject.es.entity.ProjectSummary;
import com.ezone.ezproject.es.entity.ProjectSummaryTemplate;
import com.ezone.ezproject.es.entity.ProjectTemplateAlarm;
import com.ezone.ezproject.es.entity.ProjectTemplateDetail;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.alarm.service.AlarmConfigCmdService;
import com.ezone.ezproject.modules.alarm.service.AlarmConfigQueryService;
import com.ezone.ezproject.modules.card.service.CardCmdService;
import com.ezone.ezproject.modules.card.service.CardDraftCmdService;
import com.ezone.ezproject.modules.card.service.CardHelper;
import com.ezone.ezproject.modules.chart.service.ChartCmdService;
import com.ezone.ezproject.modules.common.OperationContext;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaHelper;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService;
import com.ezone.ezproject.modules.hook.service.WebHookProjectCmdService;
import com.ezone.ezproject.modules.log.service.CompanyAuditService;
import com.ezone.ezproject.modules.log.service.OperationLogCmdService;
import com.ezone.ezproject.modules.plan.service.PlanCmdService;
import com.ezone.ezproject.modules.portfolio.service.UserPortfolioPermissionsService;
import com.ezone.ezproject.modules.project.bean.CreateProjectRequest;
import com.ezone.ezproject.modules.project.bean.ProjectExt;
import com.ezone.ezproject.modules.project.bean.ProjectMenuConfigReq;
import com.ezone.ezproject.modules.project.bean.UpdateProjectRequest;
import com.ezone.ezproject.modules.project.util.ProjectUtil;
import com.ezone.ezproject.modules.query.service.CardQueryViewCmdService;
import com.ezone.ezproject.modules.storymap.service.StoryMapCmdService;
import com.ezone.ezproject.modules.template.service.ProjectCardTemplateService;
import com.ezone.ezproject.modules.template.service.ProjectTemplateService;
import jdk.nashorn.internal.objects.annotations.Property;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ezone.ezbase.iam.bean.mq.CompanyAuditMessage.AuditType.RESOURCE_ADD;

@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectCmdService {
    @Value(value = "${project.defaultConfig.planKeepDays:0}")
    private Long defaultPlanKeepDays;

    private final ProjectMapper projectMapper;
    private final ProjectDomainMapper projectDomainMapper;

    private final UserService userService;

    private final CompanyService companyService;

    private final ProjectQueryService projectQueryService;

    private final ProjectMemberCmdService projectMemberCmdService;

    private final ProjectTemplateService projectTemplateService;

    private final ProjectSchemaQueryService projectSchemaQueryService;

    private final ProjectSchemaCmdService projectSchemaCmdService;

    private final ProjectCardTemplateService projectCardTemplateService;

    private final CardCmdService cardCmdService;

    private final PlanCmdService planCmdService;

    private final StoryMapCmdService storyMapCmdService;

    private final ProjectFavouriteCmdService projectFavouriteCmdService;

    private final ProjectNoticeBoardService projectNoticeBoardService;

    private final ProjectSummaryService projectSummaryService;

    private final CardDraftCmdService cardDraftCmdService;

    private final WebHookProjectCmdService webHookProjectCmdService;

    private final CardQueryViewCmdService cardQueryViewCmdService;

    private final ChartCmdService chartCmdService;

    private final ProjectRepoService projectRepoService;

    private final ProjectWikiSpaceService projectWikiSpaceService;

    private final ProjectResourceService projectResourceService;

    private final ProjectDocSpaceService projectDocSpaceService;

    private final ProjectExtendDao projectExtendDao;

    private final CompanyProjectSchemaHelper companyProjectSchemaHelper;

    private final CompanyProjectSchemaQueryService companyProjectSchemaQueryService;

    private final OperationLogCmdService operationLogCmdService;
    private final ProjectNoticeConfigService projectNoticeConfigService;

    private final RelPortfolioProjectMapper relPortfolioProjectMapper;

    private final ProjectRoleSchemaDao projectRoleSchemaDao;

    private final CompanyAuditService companyAuditService;

    private final ProjectMenuService projectMenuService;
    private final UserPortfolioPermissionsService portfolioPermissionsService;

    private final AlarmConfigQueryService alarmConfigQueryService;

    private final AlarmConfigCmdService alarmConfigCmdService;

    public Project create(CreateProjectRequest createProjectRequest) throws IOException {
        String user = userService.currentUserName();
        Long company = companyService.currentCompany();
        projectQueryService.checkKey(createProjectRequest.getKey());
        projectQueryService.checkName(createProjectRequest.getName());
        Date createTime = new Date();
        Project project = Project.builder()
                .id(IdUtil.generateId())
                .companyId(company)
                .key(createProjectRequest.getKey())
                .name(createProjectRequest.getName())
                .description(StringUtils.defaultString(createProjectRequest.getDescription()))
                .maxSeqNum(0L)
                .maxRank(CardHelper.PROJECT_FIRST_CARD_DEFAULT_RANK)
                .isPrivate(BooleanUtils.isTrue(createProjectRequest.getIsPrivate()))
                .isStrict(BooleanUtils.isTrue(createProjectRequest.getIsStrict()))
                .isActive(true)
                .keepDays(180L)
                .planKeepDays(defaultPlanKeepDays)
                .createUser(user)
                .createTime(createTime)
                .lastModifyUser(user)
                .lastModifyTime(createTime)
                .startTime(createProjectRequest.getStartTime())
                .endTime(createProjectRequest.getEndTime())
                .topScore(0L)
                .build();
        projectMapper.insert(project);

        List<Long> portfolioIds = createProjectRequest.getPortfolioIds();
        if (CollectionUtils.isNotEmpty(portfolioIds)) {
            for (Long portfolioId : portfolioIds) {
                relPortfolioProjectMapper.insert(RelPortfolioProject.builder().companyId(company).projectId(project.getId())
                        .createTime(createTime).createUser(user).portfolioId(portfolioId)
                        .id(IdUtil.generateId())
                        .build());
            }
        }

        ProjectDomain projectDomain = ProjectDomain.builder()
                .id(project.getId())
                .maxSeqNum(0L)
                .maxRank(CardHelper.PROJECT_FIRST_CARD_DEFAULT_RANK)
                .build();
        projectDomainMapper.insert(projectDomain);
        projectMemberCmdService.initMembers(project.getId(), user, BooleanUtils.isTrue(createProjectRequest.getIsPrivate()) ? null : "all");

        ProjectCardSchema cardSchema = null;
        Map<String, Map<String, Object>> cardTemplates = null;
        ProjectRoleSchema roleSchema = null;
        // todo reGenerateCustomKey，低优
        List<String> charts = null;
        List<String> rightCharts = null;
        ProjectMenuConfig projectMenuConfig = null;
        ProjectNoticeConfig projectNoticeConfig = null;
        List<ProjectAlarmExt> alarms = null;
        switch (createProjectRequest.getTemplateType()) {
            case PROJECT:
                Long templateProjectId = createProjectRequest.getTemplateProjectId();
                cardSchema = projectSchemaQueryService.getProjectCardSchema(templateProjectId);
                cardTemplates = projectCardTemplateService.getProjectCardTemplates(templateProjectId);
                roleSchema = projectRoleSchemaDao.find(templateProjectId);
                ProjectSummary projectSummary = projectSummaryService.find(templateProjectId);
                if (projectSummary != null) {
                    charts = projectSummary.getCharts();
                    rightCharts = projectSummary.getRightCharts();
                }
                projectNoticeConfig = projectNoticeConfigService.getProjectNoticeConfig(templateProjectId);
                if (BooleanUtils.isTrue(createProjectRequest.getCopyCharts())) {
                    chartCmdService.copyProjectChart(templateProjectId, project.getId());
                }
                projectMenuConfig = projectMenuService.getMenuConfig(templateProjectId);
                alarms = alarmConfigQueryService.getProjectAlarms(project.getId());
                break;
            case TEMPLATE:
            default:
                ProjectTemplateDetail templateDetail = projectTemplateService.getProjectTemplateDetail(
                        createProjectRequest.getProjectTemplateId());
                if (null == templateDetail) {
                    throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot find Project template!");
                }
                cardSchema = templateDetail.getProjectCardSchema();
                cardTemplates = templateDetail.getProjectCardTemplates();
                roleSchema = templateDetail.getProjectRoleSchema();

                projectMenuConfig = templateDetail.getProjectMenu();

                ProjectSummaryTemplate projectSummaryTemplate = templateDetail.getProjectSummaryConfigTemplates();
                if (projectSummaryTemplate != null) {
                    charts = projectSummaryTemplate.getCharts();
                    rightCharts = projectSummaryTemplate.getRightCharts();
                }
                projectNoticeConfig = templateDetail.getProjectNoticeConfig();
                List<ProjectTemplateAlarm> templateAlarms = templateDetail.getAlarms();
                if (CollectionUtils.isNotEmpty(templateAlarms)) {
                    alarms = templateAlarms.stream()
                            .map(projectTemplateAlarm -> ProjectAlarmExt.builder()
                                    .alarmItem(projectTemplateAlarm.getAlarmItem())
                                    .type(alarmConfigCmdService.getAlarmType(projectTemplateAlarm.getAlarmItem()).name())
                                    .active(projectTemplateAlarm.getActive()).build())
                            .collect(Collectors.toList());
                }
        }
        if (projectMenuConfig == null) {
            projectMenuConfig = projectMenuService.getDefaultMenuConfig();
        }
        projectSchemaCmdService.setProjectCardSchema(project.getId(), cardSchema);
        projectCardTemplateService.initProjectCardTemplates(project.getId(), cardTemplates);
        projectSchemaCmdService.setProjectRoleSchema(project.getId(), roleSchema);
        projectSummaryService.updateProjectSummaryConfig(project.getId(), charts, rightCharts);
        projectNoticeConfigService.initProjectNoticeConfig(project.getId(), projectNoticeConfig);
        projectMenuService.saveOrUpdateOpenMenus(project.getId(), ProjectMenuConfigReq.builder()
                .menus(projectMenuConfig.getMenus())
                .defaultMenu(projectMenuConfig.getDefaultMenu()).build());
        if (CollectionUtils.isNotEmpty(alarms)) {
            alarmConfigCmdService.copy(project.getId(), alarms);
        }
        saveProjectExtend(project, createProjectRequest.getExtend());
        OperationContext context = OperationContext.instance(user);
        companyAuditService.sendProjectCompanyAuditMessage(context, userService.currentUserId(), project, RESOURCE_ADD);
        return project;
    }

    @CacheEvict(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:ProjectQueryService.select"},
            key = "#id"
    )
    public ProjectExt update(Long id, UpdateProjectRequest updateProjectRequest) throws IOException {
        Project project = projectMapper.selectByPrimaryKey(id);
        if (!project.getName().equals(updateProjectRequest.getName())) {
            projectQueryService.checkName(updateProjectRequest.getName());
        }
        Map<String, Object> updateContent = calculateUpdateContent(project, updateProjectRequest);
        String user = userService.currentUserName();
        OperationContext opContext = OperationContext.instance(user);
        project.setName(updateProjectRequest.getName());
        project.setDescription(StringUtils.defaultString(updateProjectRequest.getDescription()));
        project.setIsPrivate(updateProjectRequest.isPrivate());
        project.setIsStrict(BooleanUtils.isTrue(updateProjectRequest.getIsStrict()));
        project.setKeepDays(updateProjectRequest.getKeepDays());
        project.setPlanKeepDays(updateProjectRequest.getPlanKeepDays());
        project.setLastModifyUser(user);
        Date createTime = new Date();
        project.setLastModifyTime(createTime);
        project.setStartTime(updateProjectRequest.getStartTime());
        project.setEndTime(updateProjectRequest.getEndTime());
        List<Long> portfolioIdsReq = updateProjectRequest.getPortfolioIds();
        resetRelPortfolioProject(project, portfolioIdsReq, createTime, user);
        projectMapper.updateByPrimaryKey(project);

        saveProjectExtend(project, updateProjectRequest.getExtend());
        operationLogCmdService.updateProjectBaseInfo(opContext, id, updateContent);
        return new ProjectExt(project, updateProjectRequest.getExtend());
    }

    private void resetRelPortfolioProject(Project project, List<Long> portfolioIdsReq, Date createTime, String user) {
        Long projectId = project.getId();
        RelPortfolioProjectExample example = new RelPortfolioProjectExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        List<RelPortfolioProject> relPortfolioProjects = relPortfolioProjectMapper.selectByExample(example);
        if (relPortfolioProjects == null) {
            relPortfolioProjects = new ArrayList<>();
        }
        if (portfolioIdsReq == null) {
            portfolioIdsReq = new ArrayList<>();
        }
        portfolioPermissionsService.checkHasPortfolioManager(portfolioIdsReq);
        final List<Long> newPortfolioIds = portfolioIdsReq;
        List<Long> needDeleteIds = relPortfolioProjects.stream().filter(rel -> !newPortfolioIds.contains(rel.getPortfolioId())).map(RelPortfolioProject::getPortfolioId).collect(Collectors.toList());
        Map<Long, RelPortfolioProject> portfolioProjectMap = relPortfolioProjects.stream().collect(Collectors.toMap(RelPortfolioProject::getPortfolioId, Function.identity()));
        List<Long> needAddIds = portfolioIdsReq.stream().filter(portfolioId -> !portfolioProjectMap.containsKey(portfolioId)).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(needDeleteIds)) {
            RelPortfolioProjectExample deleteExample = new RelPortfolioProjectExample();
            deleteExample.createCriteria().andPortfolioIdIn(needDeleteIds);
            relPortfolioProjectMapper.deleteByExample(example);
        }
        if (CollectionUtils.isNotEmpty(needAddIds)) {
            for (Long portfolioId : needAddIds) {
                relPortfolioProjectMapper.insert(RelPortfolioProject.builder().companyId(project.getCompanyId()).projectId(project.getId())
                        .createTime(createTime).createUser(user).portfolioId(portfolioId)
                        .id(IdUtil.generateId())
                        .build());
            }
        }
    }

    private Map<String, Object> calculateUpdateContent(Project project, UpdateProjectRequest updateProjectRequest) throws IOException {
        Map<String, Object> updateContent = new HashMap<>();
        boolean modifyName = !project.getName().equals(updateProjectRequest.getName());
        boolean addName = project.getName() == null && StringUtils.isNotEmpty(updateProjectRequest.getName());
        if (modifyName || addName) {
            updateContent.put("项目名称", updateProjectRequest.getName());
        }
        boolean modifyDescription = !project.getDescription().equals(updateProjectRequest.getDescription());
        boolean addDescription = project.getDescription() == null && StringUtils.isNotEmpty(updateProjectRequest.getDescription());
        if (modifyDescription || addDescription) {
            updateContent.put("项目描述", updateProjectRequest.getDescription());
        }
        boolean modifyKeepDays = !project.getKeepDays().equals(updateProjectRequest.getKeepDays());
        boolean addKeepDays = project.getKeepDays() == null && updateProjectRequest.getKeepDays() != null;
        if (modifyKeepDays || addKeepDays) {
            updateContent.put("回收站清理周期", updateProjectRequest.getKeepDays());
        }
        boolean modifyPlanKeepDays = !project.getPlanKeepDays().equals(updateProjectRequest.getPlanKeepDays());
        boolean addPlanKeepDays = project.getPlanKeepDays() == null && updateProjectRequest.getPlanKeepDays() != null;
        if (modifyPlanKeepDays || addPlanKeepDays) {
            updateContent.put("归档计划清理周期", updateProjectRequest.getPlanKeepDays());
        }
        CompanyProjectSchema schema = companyProjectSchemaQueryService.getCompanyProjectSchema(companyService.currentCompany());
        if (schema != null) {
            Map<String, ProjectField> fields = schema.getFields().stream()
                    .filter(f -> f.isEnable()).collect(Collectors
                            .toMap(ProjectField::getKey, Function.identity()));
            Map<String, Object> oldExtend = projectExtendDao.find(project.getId());
            if (oldExtend == null) {
                oldExtend = new HashMap<>(0);
            }
            final Map<String, Object> finalOldExtend = oldExtend;
            Map<String, Object> reqExtend = companyProjectSchemaHelper.preProcessProjectExtendProps(schema, updateProjectRequest.getExtend());
            Map<String, Object> changed = reqExtend.entrySet().stream()
                    .filter(e -> finalOldExtend.get(e.getKey()) == null || !finalOldExtend.get(e.getKey()).equals(e.getValue()))
                    .collect(Collectors.toMap(e -> fields.get(e.getKey()).getName(), Map.Entry::getValue));
            updateContent.putAll(changed);
        }
        return updateContent;
    }

    public void saveProjectExtend(Project project, Map<String, Object> extend) throws IOException {
        CompanyProjectSchema schema = companyProjectSchemaQueryService.getCompanyProjectSchema(companyService.currentCompany());
        extend = companyProjectSchemaHelper.preProcessProjectExtendProps(schema, extend);
        projectExtendDao.saveOrUpdate(project.getId(), ProjectUtil.projectIndexMap(project, extend));
    }

    @CacheEvict(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:ProjectQueryService.select"},
            key = "#id"
    )
    public void delete(Long id) throws IOException {
        Project project = projectMapper.selectByPrimaryKey(id);
        projectMapper.deleteByPrimaryKey(id);
        projectDomainMapper.deleteByPrimaryKey(id);
        projectSchemaCmdService.delete(id);
        projectExtendDao.delete(id);
        projectMemberCmdService.deleteByProjectId(id);
        cardCmdService.deleteByProject(id);
        planCmdService.deleteByProject(id);
        storyMapCmdService.deleteByProject(id);
        projectCardTemplateService.deleteByProjectId(id);
        projectFavouriteCmdService.deleteByProjectId(id);
        projectNoticeBoardService.delete(id); // 1
        projectSummaryService.delete(id);
        cardDraftCmdService.deleteByProject(id);
        webHookProjectCmdService.deleteByProjectId(id);
        cardQueryViewCmdService.deleteByProject(id);
        chartCmdService.deleteByProject(id);
        projectRepoService.deleteByProjectId(id);
        projectWikiSpaceService.deleteByProjectId(id);
        projectResourceService.deleteByProjectId(id);
        projectDocSpaceService.deleteByProjectId(id);
        RelPortfolioProjectExample example = new RelPortfolioProjectExample();
        example.createCriteria().andProjectIdEqualTo(id);
        relPortfolioProjectMapper.deleteByExample(example);
        OperationContext context = OperationContext.instance(userService.currentUserName());
        companyAuditService.sendProjectCompanyAuditMessage(context, userService.currentUserId(), project, CompanyAuditMessage.AuditType.RESOURCE_DELETE);
    }

    @CacheEvict(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:ProjectQueryService.select"},
            key = "#projectId"
    )
    public void addTop(Long projectId) throws IOException {
        Project project= projectMapper.selectByPrimaryKey(projectId);
        project.setTopScore(System.currentTimeMillis());
        projectMapper.updateByPrimaryKey(project);
        projectExtendDao.updateField(project.getId(), ProjectField.TOP_SCORE, project.getTopScore());
    }

    @CacheEvict(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:ProjectQueryService.select"},
            key = "#projectId"
    )
    public void deleteTop(Long projectId) throws IOException {
        Project project= projectMapper.selectByPrimaryKey(projectId);
        project.setTopScore(0L);
        projectMapper.updateByPrimaryKey(project);
        projectExtendDao.updateField(project.getId(), ProjectField.TOP_SCORE, project.getTopScore());
    }

    @CacheEvict(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:ProjectQueryService.select"},
            key = "#projectId"
    )
    public void setIsActive(Long projectId, boolean projectIsActive) throws IOException {
        Project project= projectMapper.selectByPrimaryKey(projectId);
        if (BooleanUtils.isTrue(project.getIsActive()) == projectIsActive) {
            return;
        }
        project.setIsActive(projectIsActive);
        projectMapper.updateByPrimaryKey(project);
        projectExtendDao.updateField(project.getId(), ProjectField.IS_ACTIVE, projectIsActive);
        OperationContext opContext = OperationContext.instance(userService.currentUserName());
        cardCmdService.onChangeProjectActive(opContext, project.getId(), projectIsActive);
        Map<String, Object> updateContent = new HashMap<>();
        updateContent.put("是否归档", projectIsActive);
        operationLogCmdService.updateProjectBaseInfo(opContext, project.getId(), updateContent);
    }
}
