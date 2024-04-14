package com.ezone.ezproject.modules.company.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.configuration.CacheManagers;
import com.ezone.ezproject.es.dao.CompanyCardSchemaDao;
import com.ezone.ezproject.es.dao.CompanyProjectRoleSchemaDao;
import com.ezone.ezproject.es.dao.CompanyProjectSchemaDao;
import com.ezone.ezproject.es.dao.CompanyWorkloadSettingDao;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.CompanyProjectSchema;
import com.ezone.ezproject.es.entity.CompanyWorkloadSetting;
import com.ezone.ezproject.es.entity.ProjectRoleSchema;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.modules.project.bean.CompanyCardTypeConf;
import com.ezone.ezproject.modules.project.service.ProjectCardSchemaHelper;
import com.ezone.ezproject.modules.project.service.ProjectRoleSchemaHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.ListUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class CompanyProjectSchemaQueryService {
    private CompanyProjectSchemaDao companyProjectSchemaDao;
    private CompanyCardSchemaDao companyCardSchemaDao;
    private CompanyCardSchemaHelper companyCardSchemaHelper;

    private CompanyProjectRoleSchemaDao companyProjectRoleSchemaDao;
    private ProjectRoleSchemaHelper projectRoleSchemaHelper;
    private ProjectCardSchemaHelper projectCardSchemaHelper;

    private CompanyWorkloadSettingDao companyWorkloadSettingDao;

    @NotNull
    public CompanyProjectSchema getCompanyProjectSchema(Long companyId) {
        try {
            CompanyProjectSchema schema = companyProjectSchemaDao.find(companyId);
            if (schema == null) {
                schema = CompanyProjectSchema.builder()
                        .fields(ListUtils.EMPTY_LIST)
                        .build();
            }
            return schema;
        } catch (IOException e) {
            log.error(String.format("getCompanyProjectSchema for companyId:[%s] exception!", companyId), e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Cacheable(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:ProjectSchemaService.getCompanyProjectRoleSchema"},
            key = "#companyId",
            unless = "#result == null"
    )
    public ProjectRoleSchema getCompanyProjectRoleSchema(Long companyId) {
        try {
            return projectRoleSchemaHelper.mergeDefaultSchema(companyProjectRoleSchemaDao.find(companyId), RoleSource.COMPANY);
        } catch (IOException e) {
            log.error(String.format("getCompanyProjectRoleSchema for companyId:[%s] exception!", companyId), e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Cacheable(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:ProjectSchemaService.getCompanyCardSchema"},
            key = "#companyId",
            unless = "#result == null"
    )
    public CompanyCardSchema getCompanyCardSchema(Long companyId) {
        try {
            CompanyCardSchema companyCardSchema = companyCardSchemaHelper.fillSysSchema(companyCardSchemaDao.find(companyId));
            List<String> typeOrders = projectCardSchemaHelper.getSysProjectCardSchema().getOrderOfTypes();
            List<CompanyCardTypeConf> sortedTypes = companyCardSchema.getTypes().stream().sorted(Comparator.comparingInt(type -> typeOrders.indexOf(type.getKey()))).collect(Collectors.toList());
            companyCardSchema.setTypes(sortedTypes);
            return companyCardSchema;

        } catch (IOException e) {
            log.error(String.format("getCompanyCardSchema for companyId:[%s] exception!", companyId), e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Cacheable(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:CompanyProjectSchemaService.getCompanyWorkloadSetting"},
            key = "#companyId",
            unless = "#result == null"
    )
    public CompanyWorkloadSetting getCompanyWorkloadSetting(Long companyId) {
        try {
            return companyWorkloadSettingDao.find(companyId);
        } catch (IOException e) {
            log.error(String.format("getCompanyWorkloadSetting for companyId:[%s] exception!", companyId), e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @CacheEvict(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:ProjectSchemaService.getCompanyCardSchema"},
            key = "#companyId"
    )
    public void cleanCompanyCardSchemaCache(Long companyId) {
    }
}
