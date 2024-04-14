package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.ProjectMenuConfig;
import com.ezone.ezproject.es.util.EsIndexUtil;
import com.ezone.galaxy.framework.common.util.JsonUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;

@Component
@Slf4j
@AllArgsConstructor
public class ProjectMenuDao extends AbstractEsDocDao<Long, ProjectMenuConfig> {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Override
    protected String index() {
        return EsIndexUtil.indexForProjectMenu();
    }

    @Override
    public void saveOrUpdate(Long id, ProjectMenuConfig projectMenu) throws IOException {
        ProjectMenuConfig oldProjectMenu = find(id);
        if (oldProjectMenu != null) {
            ArrayList<Long> ids = new ArrayList<>();
            ids.add(id);
            updateDoc(index(), ids, JsonUtils.toObject(JSON_MAPPER.writeValueAsString(projectMenu), Map.class));
        } else {
            setDocSourceJson(id, JSON_MAPPER.writeValueAsString(projectMenu));
        }
    }

    @Override
    protected @NotNull Function<String, ProjectMenuConfig> deserializer() {
        return s -> {
            try {
                return JSON_MAPPER.readValue(s, ProjectMenuConfig.class);
            } catch (JsonProcessingException e) {
                log.error("Deserialize ProjectMenu json exception!", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };
    }
}
