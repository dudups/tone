package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.util.EsIndexUtil;
import com.ezone.ezproject.modules.chart.config.Chart;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@Slf4j
@AllArgsConstructor
public class ChartDao extends AbstractEsDocDao<Long, Chart> {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    protected String index() {
        return EsIndexUtil.indexForChart();
    }

    @Override
    public void saveOrUpdate(Long id, Chart chart) throws IOException {
        setDocSourceJson(id, JSON_MAPPER.writeValueAsString(chart));
    }

    @Override
    protected @NotNull Function<String, Chart> deserializer() {
        return s -> {
            try {
                return JSON_MAPPER.readValue(s, Chart.class);
            } catch (JsonProcessingException e) {
                log.error("Deserialize Chart json exception!", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };
    }

    public void saveOrUpdate(Map<Long, Chart> charts) throws IOException {
        if (MapUtils.isEmpty(charts)) {
            return;
        }
        Map<Long, String> chartsJson = new HashMap<>(16);
        for (Map.Entry<Long, Chart> entry : charts.entrySet()) {
            chartsJson.put(entry.getKey(), JSON_MAPPER.writeValueAsString(entry.getValue()));
        }
        setDocsSources(index(), chartsJson, XContentType.JSON);
    }
}
