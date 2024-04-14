package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.configuration.CacheManagers;
import com.ezone.ezproject.es.dao.ProjectCardSchemaDao;
import com.ezone.ezproject.es.dao.ProjectRoleSchemaDao;
import com.ezone.ezproject.es.dao.ProjectWorkloadSettingDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CompanyWorkloadSetting;
import com.ezone.ezproject.es.entity.MergedProjectRoleSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectRoleSchema;
import com.ezone.ezproject.es.entity.ProjectWorkloadSetting;
import com.ezone.ezproject.es.entity.enums.FieldType;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.modules.card.bean.ChangeTypeCheckResult;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService;
import com.ezone.ezproject.modules.project.bean.CheckSchemaResult;
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectSchemaQueryService {
    private ProjectCardSchemaDao projectCardSchemaDao;
    private ProjectRoleSchemaDao projectRoleSchemaDao;
    private ProjectWorkloadSettingDao projectWorkloadSettingDao;

    private ProjectCardSchemaHelper projectCardSchemaHelper;
    private ProjectRoleSchemaHelper projectRoleSchemaHelper;

    private CompanyProjectSchemaQueryService companyProjectSchemaQueryService;
    private ProjectQueryService projectQueryService;

    @Cacheable(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:ProjectSchemaService.getProjectCardSchema"},
            key = "#projectId",
            unless = "#result == null"
    )
    public ProjectCardSchema getProjectCardSchema(Long projectId) {
        try {
            return projectCardSchemaHelper.fillSysSchema(projectCardSchemaDao.find(projectId));
        } catch (IOException e) {
            log.error(String.format("getProjectCardSchema for projectId:[%s] exception!", projectId), e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public MergedProjectRoleSchema getProjectRoleSchema(Long projectId) {
        try {
            ProjectRoleSchema schema = projectRoleSchemaDao.find(projectId);
            Long companyId = projectQueryService.getProjectCompany(projectId);
            ProjectRoleSchema companySchema = companyProjectSchemaQueryService.getCompanyProjectRoleSchema(companyId);
            return projectRoleSchemaHelper.mergeDefaultSchema(schema, RoleSource.CUSTOM, companySchema);
        } catch (IOException e) {
            log.error(String.format("getProjectRoleSchema for projectId:[%s] exception!", projectId), e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Cacheable(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:ProjectSchemaService.getProjectWorkloadSetting"},
            key = "#projectId",
            unless = "#result == null"
    )
    public ProjectWorkloadSetting findProjectWorkloadSetting(Long projectId) {
        try {
            return projectWorkloadSettingDao.find(projectId);
        } catch (IOException e) {
            log.error(String.format("getCompanyWorkloadSetting for projectId:[%s] exception!", projectId), e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public ProjectWorkloadSetting getProjectWorkloadSetting(Long projectId) {
        Long companyId = projectQueryService.getProjectCompany(projectId);
        CompanyWorkloadSetting companyWorkloadSetting = companyProjectSchemaQueryService.getCompanyWorkloadSetting(companyId);
        if (companyWorkloadSetting != null && companyWorkloadSetting.isForceCompanyRule()) {
            return companyWorkloadSetting;
        }
        return SpringBeanFactory.getBean(ProjectSchemaQueryService.class).findProjectWorkloadSetting(projectId);
    }

    @CacheEvict(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:ProjectSchemaService.getProjectCardSchema"},
            key = "#projectId"
    )
    public void cleanProjectCardSchemaCache(Long projectId) {

    }

    public List<String> getUserOrMemberFields(Long projectId) {
        ProjectCardSchema projectCardSchema = getProjectCardSchema(projectId);
        if (projectCardSchema == null) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, projectId + "项目不存在!");
        }
        List<CardField> fields = projectCardSchema.getFields();
        return fields.stream().filter(field -> {
            if (field == null) {
                return false;
            }
            FieldType type = field.getType();
            return type.equals(FieldType.USER) || type.equals(FieldType.USERS)
                    || type.equals(FieldType.MEMBER) || type.equals(FieldType.MEMBERS);
        }).map(CardField::getKey).collect(Collectors.toList());
    }

    public Map<Long, ProjectCardSchema> find(List<Long> projectIds, String... fields) {
        try {
            return projectCardSchemaDao.find(projectIds, fields);
        } catch (IOException e) {
            log.error("find ProjectCardSchema for projectIds exception!", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public CheckSchemaResult checkSchemaForCopy(Long fromProjectId, Long toProjectId) throws IOException {
        ProjectSchemaQueryService service = SpringBeanFactory.getBean(ProjectSchemaQueryService.class);
        return projectCardSchemaHelper.checkSchemaForCopy(
                service.getProjectCardSchema(fromProjectId),
                service.getProjectCardSchema(toProjectId));
    }

    public List<ChangeTypeCheckResult> checkSchemaForChangeType(Long fromProjectId, Map<String, String> typeMap) throws IOException {
        ProjectSchemaQueryService service = SpringBeanFactory.getBean(ProjectSchemaQueryService.class);
        return projectCardSchemaHelper.checkSchemaForChangeType(
                service.getProjectCardSchema(fromProjectId),
                typeMap);
    }
}
