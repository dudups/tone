package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.PortfolioConfig;
import com.ezone.ezproject.es.util.EsIndexUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
public class PortfolioConfigDao extends AbstractEsDocDao<Long, PortfolioConfig> {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    protected String index() {
        return EsIndexUtil.indexForPortfolioConfig();
    }

    @Override
    public void saveOrUpdate(Long id, PortfolioConfig config) throws IOException {
        setDocSourceJson(id, JSON_MAPPER.writeValueAsString(config));
    }

    @Override
    protected @NotNull Function<String, PortfolioConfig> deserializer() {
        return s -> {
            try {
                return JSON_MAPPER.readValue(s, new TypeReference<PortfolioConfig>() {
                });
            } catch (JsonProcessingException e) {
                log.error("Deserialize ChartConfig json exception!", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };
    }
}
