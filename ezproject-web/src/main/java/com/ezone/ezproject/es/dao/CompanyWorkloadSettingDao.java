package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.configuration.CacheManagers;
import com.ezone.ezproject.es.entity.CompanyWorkloadSetting;
import com.ezone.ezproject.es.util.EsIndexUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.function.Function;

@Component
@Slf4j
@AllArgsConstructor
public class CompanyWorkloadSettingDao extends AbstractEsDocDao<Long, CompanyWorkloadSetting> {
    @Override
    protected String index() {
        return EsIndexUtil.indexForCompanyWorkloadSetting();
    }

    @CacheEvict(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:CompanyProjectSchemaService.getCompanyWorkloadSetting"},
            key = "#id"
    )
    @Override
    public void saveOrUpdate(Long id, CompanyWorkloadSetting setting) throws IOException {
        setDocSourceYaml(id, setting.yaml());
    }

    @Override
    protected @NotNull Function<String, CompanyWorkloadSetting> deserializer() {
        return s -> {
            try {
                return CompanyWorkloadSetting.from(s);
            } catch (JsonProcessingException e) {
                log.error("Deserialize CompanyWorkloadSetting json exception!", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };
    }
}
