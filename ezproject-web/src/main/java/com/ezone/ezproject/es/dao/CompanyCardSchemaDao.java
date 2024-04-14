package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.configuration.CacheManagers;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.util.EsIndexUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.elasticsearch.action.get.MultiGetResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@AllArgsConstructor
public class CompanyCardSchemaDao extends AbstractEsDocDao<Long, CompanyCardSchema> {
    @Override
    protected String index() {
        return EsIndexUtil.indexForCompanyCardSchema();
    }

    @CacheEvict(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:ProjectSchemaService.getCompanyCardSchema"},
            key = "#id"
    )
    @Override
    public void saveOrUpdate(Long id, CompanyCardSchema schema) throws IOException {
        setDocSourceYaml(id, schema.yaml());
    }

    public Map<Long, CompanyCardSchema> find(List<Long> ids, String... fields) throws IOException {
        MultiGetResponse response = getDocSources(index(), ids, fields);
        return Arrays.stream(response.getResponses())
                .filter(r -> r.getResponse().isExists())
                .collect(Collectors.toMap(
                        r -> NumberUtils.createLong(r.getId()),
                        r -> deserializer().apply(r.getResponse().getSourceAsString())));
    }

    @Override
    protected @NotNull Function<String, CompanyCardSchema> deserializer() {
        return s -> {
            try {
                return CompanyCardSchema.from(s);
            } catch (JsonProcessingException e) {
                log.error("Deserialize CompanyCardSchema json exception!", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };
    }
}
