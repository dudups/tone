package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.PlanNotice;
import com.ezone.ezproject.es.util.EsIndexUtil;
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
import java.util.function.Function;

@Component
@Slf4j
@AllArgsConstructor
@Deprecated
public class PlanNoticeDao extends AbstractEsDocDao<Long, PlanNotice> {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    protected String index() {
        return EsIndexUtil.indexForPlanNotice();
    }

    @Override
    public void saveOrUpdate(Long id, PlanNotice PlanNotice) throws IOException {
        setDocSourceJson(id, JSON_MAPPER.writeValueAsString(PlanNotice));
    }

    @Override
    protected @NotNull Function<String, PlanNotice> deserializer() {
        return s -> {
            try {
                return JSON_MAPPER.readValue(s, PlanNotice.class);
            } catch (JsonProcessingException e) {
                log.error("Deserialize PlanNotice json exception!", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };
    }
}
