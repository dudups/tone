package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.util.EsIndexUtil;
import com.ezone.ezproject.modules.chart.ezinsight.config.EzInsightChart;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.client.Requests;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
@Slf4j
@AllArgsConstructor
public class PortfolioChartConfigDao extends AbstractEsDocDao<Long, Map<String, Object>> {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    protected String index() {
        return EsIndexUtil.indexForPortfolioChartConfig();
    }

    @Override
    public void saveOrUpdate(Long id, Map<String, Object> chart) throws IOException {
        setDocSourceJson(id, JSON_MAPPER.writeValueAsString(chart));
    }

    public void saveOrUpdate(Long id, EzInsightChart chart) throws IOException {
        setDocSourceJson(id, JSON_MAPPER.writeValueAsString(chart));
    }

    public void saveOrUpdate(Map<Long, EzInsightChart> charts) throws IOException {
        if (MapUtils.isEmpty(charts)) {
            return;
        }
        Map<Long, String> docs = new HashMap<>();
        for (Map.Entry<Long, EzInsightChart> entry : charts.entrySet()) {
            Long id = entry.getKey();
            EzInsightChart chart = entry.getValue();
            docs.put(id, JSON_MAPPER.writeValueAsString(chart));
        }
        setDocsSources(index(), docs, Requests.INDEX_CONTENT_TYPE);
    }

    @Override
    protected @NotNull Function<String, Map<String, Object>> deserializer() {
        return s -> {
            try {
                return JSON_MAPPER.readValue(s, new TypeReference<Map<String, Object>>() {
                });
            } catch (JsonProcessingException e) {
                log.error("Deserialize ChartConfig json exception!", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };
    }
}
