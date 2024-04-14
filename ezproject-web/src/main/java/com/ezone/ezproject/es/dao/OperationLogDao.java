package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.OperationLog;
import com.ezone.ezproject.es.entity.OperationLogField;
import com.ezone.ezproject.es.util.EsIndexUtil;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

@Component
@Slf4j
@AllArgsConstructor
public class OperationLogDao extends AbstractEsDocDao<Long, OperationLog> {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    protected String index() {
        return EsIndexUtil.indexForOperationLog();
    }

    public void saveOrUpdate(Long id, String eventJson) throws IOException {
        setDocSourceJson(id, eventJson);
    }

    @Override
    public void saveOrUpdate(Long id, OperationLog operationLog) throws IOException {
        setDocSourceJson(id, JSON_MAPPER.writeValueAsString(operationLog));
    }

    public void saveOrUpdate(OperationLog operationLog) throws IOException {
        setDocSourceJson(operationLog.getId(), JSON_MAPPER.writeValueAsString(operationLog));
    }

    @Override
    protected Function<String, OperationLog> deserializer() {
        return s -> {
            try {
                return JSON_MAPPER.readValue(s, OperationLog.class);
            } catch (JsonProcessingException e) {
                log.error("Deserialize PlanNotice json exception!", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };
    }

    public TotalBean<OperationLog> search(List<Query> queries, Integer pageNumber, Integer pageSize) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        queryBuilders(queries).forEach(queryBuilder -> bool.filter(queryBuilder));
        searchSourceBuilder.query(bool);
        searchSourceBuilder.sort(new FieldSortBuilder(OperationLogField.CREATE_TIME).order(SortOrder.DESC));

        searchSourceBuilder.fetchSource(OperationLogField.DEFAULT_SHOW_FIELDS, null);
        searchSourceBuilder.from((pageNumber - 1) * pageSize).size(pageSize);
        return search(searchSourceBuilder);
    }

    private TotalBean<OperationLog> search(SearchSourceBuilder searchSourceBuilder) throws IOException {
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        return search(request);
    }

    private TotalBean<OperationLog> search(SearchRequest request) throws IOException {
        return search(request, hit -> deserializer().apply(hit.getSourceAsString()));
    }
}
