package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.ProjectRoleSchema;
import com.ezone.ezproject.es.util.EsIndexUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.elasticsearch.action.get.MultiGetResponse;
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
public class ProjectRoleSchemaDao extends AbstractEsDocDao<Long, ProjectRoleSchema> {
    @Override
    protected String index() {
        return EsIndexUtil.indexForProjectRoleSchema();
    }

    @Override
    public void saveOrUpdate(Long id, ProjectRoleSchema schema) throws IOException {
        setDocSourceYaml(id, schema.yaml());
    }

    public Map<Long, ProjectRoleSchema> find(List<Long> ids, String... fields) throws IOException {
        MultiGetResponse response = getDocSources(index(), ids, fields);
        return Arrays.stream(response.getResponses())
                .filter(r -> r.getResponse().isExists())
                .collect(Collectors.toMap(
                        r -> NumberUtils.createLong(r.getId()),
                        r -> deserializer().apply(r.getResponse().getSourceAsString())));
    }

    @Override
    protected @NotNull Function<String, ProjectRoleSchema> deserializer() {
        return s -> {
            try {
                return ProjectRoleSchema.from(s);
            } catch (JsonProcessingException e) {
                log.error("Deserialize ProjectRoleSchema json exception!", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };
    }
}
