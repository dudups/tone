package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.WebHookProject;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Component
@Slf4j
@AllArgsConstructor
public class WebHookProjectDao extends AbstractEsDocDao<Long, WebHookProject> {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    protected String index() {
        return EsIndexUtil.indexForWebHookProject();
    }

    @Override
    public void saveOrUpdate(Long id, WebHookProject webHookProject) throws IOException {
        setDocSourceJson(id, JSON_MAPPER.writeValueAsString(webHookProject));
    }

    public List<Long> searchIds(List<Query> queries) throws IOException {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        queryBuilders(queries).forEach(queryBuilder -> {
            bool.filter(queryBuilder);
        });
        return searchAllIds(index(), bool);
    }

    public List<Long> searchIds(Query... queries) throws IOException {
        return searchIds(Arrays.asList(queries));
    }

    public List<WebHookProject> search(List<Query> queries, String... includes) throws IOException {
        SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(queries, includes);
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        return search(request, hit -> deserializer().apply(hit.getSourceAsString())).getList();
    }

    public List<WebHookProject> search(Query query, String... includes) throws IOException {
        return search(Arrays.asList(query), includes);
    }

    @Override
    public void delete(List<Long> ids) throws IOException {
        deleteDoc(index(), ids);
    }

    public void delete(Query... queries) throws IOException {
        deleteDocByQuery(index(), Arrays.asList(queries));
    }

    @Override
    protected @NotNull Function<String, WebHookProject> deserializer() {
        return s -> {
            try {
                return JSON_MAPPER.readValue(s, WebHookProject.class);
            } catch (JsonProcessingException e) {
                log.error("Deserialize WebHookProject json exception!", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };
    }
}
