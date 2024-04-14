package com.ezone.ezproject.modules.template.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.ProjectTemplate;
import com.ezone.ezproject.dal.entity.ProjectTemplateExample;
import com.ezone.ezproject.dal.mapper.ProjectTemplateMapper;
import com.ezone.ezproject.es.dao.ProjectTemplateDetailDao;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectMenuConfig;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.es.entity.ProjectRoleSchema;
import com.ezone.ezproject.es.entity.ProjectSummaryTemplate;
import com.ezone.ezproject.es.entity.ProjectTemplateAlarm;
import com.ezone.ezproject.es.entity.ProjectTemplateDetail;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.es.entity.enums.Source;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.alarm.bean.AlarmItem;
import com.ezone.ezproject.modules.common.LockHelper;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService;
import com.ezone.ezproject.modules.project.bean.ProjectMenu;
import com.ezone.ezproject.modules.project.service.ProjectCardSchemaHelper;
import com.ezone.ezproject.modules.project.service.ProjectMenuService;
import com.ezone.ezproject.modules.project.service.ProjectRoleSchemaHelper;
import com.ezone.ezproject.modules.template.bean.CreateProjectTemplateRequest;
import com.ezone.ezproject.modules.template.bean.ProjectTemplateBean;
import com.ezone.ezproject.modules.template.bean.UpdateProjectTemplateRequest;
import com.github.pagehelper.PageHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.boot.autoconfigure.klock.lock.Lock;
import org.springframework.boot.autoconfigure.klock.lock.LockFactory;
import org.springframework.boot.autoconfigure.klock.model.LockInfo;
import org.springframework.boot.autoconfigure.klock.model.LockType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectTemplateService {
    public static final String DEFAULT_ENABLE_TEMPLATE = "软件研发";

    private ProjectTemplateMapper projectTemplateMapper;

    private ProjectTemplateDetailDao projectTemplateDetailDao;

    private ProjectCardSchemaHelper projectCardSchemaHelper;

    private ProjectRoleSchemaHelper projectRoleSchemaHelper;

    private UserService userService;

    private CompanyService companyService;

    private LockFactory lockFactory;

    private LockHelper lockHelper;

    private CompanyProjectSchemaQueryService companyProjectSchemaQueryService;

    private ProjectMenuService projectMenuService;

    private static final Map<String, List<String>> SYS_TEMPLATES = new HashMap<>();
    private static final Map<String, List<ProjectMenu>> SYS_TEMPLATES_MENU = new HashMap<>();

    static {
        SYS_TEMPLATES.put(DEFAULT_ENABLE_TEMPLATE, Arrays.asList("story", "task", "bug"));
        SYS_TEMPLATES.put("通用项目", Arrays.asList("transaction"));
        SYS_TEMPLATES_MENU.put(DEFAULT_ENABLE_TEMPLATE, Arrays.asList(ProjectMenu.values()));
        SYS_TEMPLATES_MENU.put("通用项目", Arrays.asList(ProjectMenu.values()));
    }

    @Transactional
    public ProjectTemplate create(CreateProjectTemplateRequest request) throws IOException {
        String user = userService.currentUserName();
        Long company = companyService.currentCompany();
        Long id = IdUtil.generateId();
        ProjectTemplate template = ProjectTemplate.builder()
                .id(id)
                .companyId(company)
                .source(Source.CUSTOM.name())
                .name(request.getName())
                .enable(true)
                .createUser(user)
                .createTime(new Date())
                .lastModifyUser(user)
                .lastModifyTime(new Date())
                .build();
        projectTemplateMapper.insert(template);
        ProjectTemplateDetail templateDetail = null;
        Long copyTemplateId = request.getCopyTemplateId();
        if (copyTemplateId > 0) {
            templateDetail = getProjectTemplateDetail(copyTemplateId);
        }
        if (null == templateDetail) {
            ProjectCardSchema schema = projectCardSchemaHelper.fillSysSchema(projectCardSchemaHelper.newSysProjectCardSchema());
            projectCardSchemaHelper.tripSysSchema(schema);
            templateDetail = ProjectTemplateDetail.builder()
                    .projectCardSchema(schema)
                    .projectCardTemplates(projectCardSchemaHelper.getSysProjectCardTemplate())
                    .build();
        }
        try {
            projectTemplateDetailDao.saveOrUpdate(id, templateDetail);
        } catch (IOException e) {
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        return template;
    }

    @Transactional
    public void update(Long id, UpdateProjectTemplateRequest request) {
        ProjectTemplateExample example = new ProjectTemplateExample();
        example.createCriteria().andCompanyIdEqualTo(companyService.currentCompany());
        List<ProjectTemplate> templates = projectTemplateMapper.selectByExample(example);
        ProjectTemplate template = null;
        boolean hasEnable = false;
        for (ProjectTemplate projectTemplate : templates) {
            if (projectTemplate.getId().equals(id)) {
                template = projectTemplate;
                hasEnable = hasEnable || request.isEnable();
            } else {
                hasEnable = hasEnable || BooleanUtils.isTrue(projectTemplate.getEnable());
            }
        }
        if (null == template) {
            throw CodedException.NOT_FOUND;
        }
        if (!hasEnable) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "至少需要保留开启一个模版!");
        }
        template.setName(request.getName());
        template.setEnable(request.isEnable());
        template.setLastModifyUser(userService.currentUserName());
        template.setLastModifyTime(new Date());
        projectTemplateMapper.updateByPrimaryKey(template);
    }

    public List<ProjectTemplateBean> getProjectTemplateBeans(Long company, boolean detail) throws IOException {
        List<ProjectTemplateBean> beans = new ArrayList<>();
        List<ProjectTemplate> templates = getProjectTemplates(company);
        if (CollectionUtils.isNotEmpty(templates)) {
            beans.addAll(templates.stream()
                    .map(template -> {
                        ProjectTemplateBean bean = ProjectTemplateBean.builder().template(template).build();
                        if (detail) {
                            ProjectTemplateDetail templateDetail = getProjectTemplateDetail(template.getId());
                            templateDetail.setProjectCardSchema(projectCardSchemaHelper.fillSysSchema(templateDetail.getProjectCardSchema()));
                            ProjectRoleSchema companyRoleSchema = companyProjectSchemaQueryService.getCompanyProjectRoleSchema(company);
                            templateDetail.setProjectRoleSchema(projectRoleSchemaHelper.mergeDefaultSchema(templateDetail.getProjectRoleSchema(), RoleSource.CUSTOM, companyRoleSchema));
                            templateDetail.setProjectNoticeConfig(getProjectNoticeConfig(template.getId()));
                            bean.setDetail(templateDetail);

                        }
                        return bean;
                    })
                    .collect(Collectors.toList())
            );
        }
        return beans;
    }

    @Transactional
    public void setProjectTemplateBeans(String user, Long company, List<ProjectTemplateBean> beans) throws IOException {
        List<ProjectTemplate> templatesInDb = getProjectTemplates(company);
        templatesInDb.forEach(templateInDb -> {
            ProjectTemplateBean templateBean = beans.stream()
                    .filter(bean -> templateInDb.getName().equals(bean.getTemplate().getName()))
                    .findFirst()
                    .get();
            if (null == templateBean) {
                deleteTemplate(templateInDb);
            } else {
                templateInDb.setLastModifyUser(user);
                templateInDb.setLastModifyTime(new Date());
                projectTemplateMapper.updateByPrimaryKey(templateInDb);
                try {
                    projectCardSchemaHelper.tripSysSchema(templateBean.getDetail().getProjectCardSchema());
                    projectTemplateDetailDao.saveOrUpdate(templateInDb.getId(), templateBean.getDetail());
                } catch (IOException e) {
                    log.error(String.format("SaveOrUpdate es projectTemplate id:[%s] exception!", templateInDb.getId()), e);
                    throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
                }
            }
        });
        beans.forEach(bean -> {
            if (!templatesInDb.stream().anyMatch(t -> t.getName().equals(bean.getTemplate().getName()))) {
                ProjectTemplate template = bean.getTemplate();
                Long id = IdUtil.generateId();
                projectTemplateMapper.insert(ProjectTemplate.builder()
                        .id(id)
                        .companyId(company)
                        .source(template.getSource())
                        .name(template.getName())
                        .enable(template.getEnable())
                        .createUser(user)
                        .createTime(new Date())
                        .lastModifyUser(user)
                        .lastModifyTime(new Date())
                        .build());
                try {
                    projectCardSchemaHelper.tripSysSchema(bean.getDetail().getProjectCardSchema());
                    projectTemplateDetailDao.saveOrUpdate(id, bean.getDetail());
                } catch (IOException e) {
                    throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
                }
            }
        });
    }

    @Transactional
    public void deleteTemplate(ProjectTemplate template) {
        projectTemplateMapper.deleteByPrimaryKey(template.getId());
        try {
            projectTemplateDetailDao.delete(template.getId());
        } catch (IOException e) {
            log.error(String.format("Delete es template detail id:[%s] exception!", template.getId()), e.getMessage());
        }
    }

    public ProjectTemplate select(Long id) {
        return projectTemplateMapper.selectByPrimaryKey(id);
    }

    public ProjectTemplateDetail getProjectTemplateDetail(Long id) {
        try {
            return projectTemplateDetailDao.find(id);
        } catch (IOException e) {
            log.error(String.format("Find template detail:[%s] exception!", id), e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public ProjectCardSchema getSchema(Long id) {
        return projectCardSchemaHelper.fillSysSchema(getProjectTemplateDetail(id).getProjectCardSchema());
    }

    public ProjectRoleSchema getRoleSchema(Long id) {
        ProjectTemplate template = select(id);
        ProjectRoleSchema companyRoleSchema = companyProjectSchemaQueryService.getCompanyProjectRoleSchema(template.getCompanyId());
        return projectRoleSchemaHelper.mergeDefaultSchema(getProjectTemplateDetail(id).getProjectRoleSchema(), RoleSource.CUSTOM, companyRoleSchema);
    }

    public ProjectSummaryTemplate getProjectSummaryTemplate(Long id) {
        return getProjectTemplateDetail(id).getProjectSummaryConfigTemplates();
    }

    public Map<String, Object> getTemplate4Card(Long id, String cardType) {
        Map<String, Map<String, Object>> templates = getProjectTemplateDetail(id).getProjectCardTemplates();
        if (templates == null) {
            return null;
        }
        return templates.get(cardType);
    }

    public ProjectMenuConfig getMenus(Long id) {
        ProjectMenuConfig projectMenu = getProjectTemplateDetail(id).getProjectMenu();
        if (projectMenu == null) {
            return projectMenuService.getDefaultMenuConfig();
        }
        return projectMenu;
    }

    public List<ProjectTemplate> getProjectTemplates(Long company) {
        ProjectTemplateExample example = new ProjectTemplateExample();
        example.createCriteria().andCompanyIdEqualTo(company);
        List<ProjectTemplate> templates = projectTemplateMapper.selectByExample(example);
        if (null == templates) {
            templates = ListUtils.EMPTY_LIST;
        }
        return templates;
    }

    @Transactional
    public List<ProjectTemplate> initSysProjectTemplates(String user, Long company) {
        Lock lock = lockInit(company);
        if (lock.acquire()) {
            try {
                return sysProjectTemplateBeans(true).stream().map(bean -> {
                    Long id = IdUtil.generateId();
                    ProjectTemplate template = ProjectTemplate.builder()
                            .id(id)
                            .companyId(company)
                            .source(Source.SYS.name())
                            .name(bean.getTemplate().getName())
                            .enable(true)
                            .createUser(user)
                            .createTime(new Date())
                            .lastModifyUser(user)
                            .lastModifyTime(new Date())
                            .build();

                    projectTemplateMapper.insert(template);
                    try {
                        projectCardSchemaHelper.tripSysSchema(bean.getDetail().getProjectCardSchema());
                        projectTemplateDetailDao.saveOrUpdate(id, bean.getDetail());
                    } catch (IOException e) {
                        throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
                    }
                    return template;
                }).collect(Collectors.toList());
            } finally {
                lock.release();
            }
        } else {
            throw new CodedException(HttpStatus.CONFLICT, "初始化锁定公司冲突!");
        }
    }

    private Lock lockInit(Long company) {
        return lockFactory.getLock(new LockInfo(LockType.Reentrant,
                String.format("lock:company:init:%s", company), 2, 2));
    }

    private List<ProjectTemplateBean> sysProjectTemplateBeans(boolean detail) {
        return SYS_TEMPLATES.entrySet().stream().map(entry -> {
            ProjectTemplate template = ProjectTemplate.builder()
                    .source(Source.SYS.name())
                    .name(entry.getKey())
                    .enable(DEFAULT_ENABLE_TEMPLATE.equals(entry.getKey()))
                    .build();
            ProjectTemplateBean bean = ProjectTemplateBean.builder().template(template).build();
            if (detail) {
                ProjectCardSchema schema = projectCardSchemaHelper.fillSysSchema(projectCardSchemaHelper.newSysProjectCardSchema());
                schema.getTypes().forEach(cardType -> cardType.setEnable(entry.getValue().contains(cardType.getKey())));
                List<ProjectMenu> defaultMenu = SYS_TEMPLATES_MENU.get(entry.getKey());
                bean.setDetail(ProjectTemplateDetail.builder()
                        .projectCardSchema(schema)
                        .projectCardTemplates(projectCardSchemaHelper.getSysProjectCardTemplate())
                        .projectMenu(ProjectMenuConfig.builder().menus(defaultMenu).build())
                        .build());
            }
            return bean;
        }).collect(Collectors.toList());
    }

    public List<ProjectTemplate> selectAll(Integer pageNumber, Integer pageSize) {
        ProjectTemplateExample example = new ProjectTemplateExample();
        PageHelper.startPage(pageNumber, pageSize, true);
        return projectTemplateMapper.selectByExample(example);
    }

    public ProjectNoticeConfig getProjectNoticeConfig(Long id) {
        ProjectNoticeConfig projectNoticeConfig = getProjectTemplateDetail(id).getProjectNoticeConfig();
        if (projectNoticeConfig == null) {
            projectNoticeConfig = ProjectNoticeConfig.DEFAULT_CONFIG;
        }
        return projectNoticeConfig;
    }

    @NotNull
    public List<ProjectTemplateAlarm> getAlarmConfig(Long id) {
        List<ProjectTemplateAlarm> alarms = getProjectTemplateDetail(id).getAlarms();
        if (alarms == null) {
            alarms = Collections.emptyList();
        }
        return alarms;
    }
}
