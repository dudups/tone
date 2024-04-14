package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.util.EsIndexUtil;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.project.bean.ProjectExt;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@AllArgsConstructor
public class ProjectExtendDao extends AbstractEsDocDao<Long, Map<String, Object>> {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    protected String index() {
        return EsIndexUtil.indexForProjectExtend();
    }

    @Override
    public void saveOrUpdate(Long id, Map<String, Object> extend) throws IOException {
        setDocSourceJson(id, JSON_MAPPER.writeValueAsString(extend));
    }

    public void saveOrUpdate(Map<Long, Map<String, Object>> jsons) throws IOException {
        setDocsSources(index(), jsons);
    }

    public void updateSelective(Long id, Map<String, Object> props) throws IOException {
        updateDoc(index(), id, props);
    }

    public void updateField(Long id, String field, Object value) throws IOException {
        Map<String, Object> props = new HashMap<>();
        props.put(field, value);
        updateSelective(id, props);
    }

    public Map<Long, Map<String, Object>> findAsMap(List<Long> ids) throws IOException {
        MultiGetResponse multiGetResponse = getDocSources(index(), ids, null);
        return Arrays.stream(multiGetResponse.getResponses())
                .filter(r -> r.getResponse().isExists())
                .collect(Collectors.toMap(r -> NumberUtils.toLong(r.getId()), r -> r.getResponse().getSource()));
    }

    public TotalBean<ProjectExt> search(SearchEsRequest search, Integer pageNumber, Integer pageSize) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        queryBuilders(search.getQueries()).forEach(queryBuilder -> {
            bool.filter(queryBuilder);
        });
        searchSourceBuilder.query(bool);
        if (CollectionUtils.isNotEmpty(search.getSorts())) {
            search.getSorts().forEach(sort -> {
                searchSourceBuilder.sort(new FieldSortBuilder(sort.getField()).order(sort.getOrder()));
            });
        }
        searchSourceBuilder.fetchSource(search.getFields(), null);
        searchSourceBuilder.from((pageNumber - 1) * pageSize).size(pageSize);
        return search(searchSourceBuilder);
    }

    private TotalBean<ProjectExt> search(SearchSourceBuilder searchSourceBuilder) throws IOException {
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        return search(request);
    }

    private TotalBean<ProjectExt> search(SearchRequest request) throws IOException {
        return search(request, hit -> new ProjectExt(NumberUtils.toLong(hit.getId()), hit.getSourceAsMap()));
    }

    public Map<Long, Map<String, Object>> searchAsIdMap(List<Query> queries) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        queryBuilders(queries).forEach(queryBuilder -> bool.filter(queryBuilder));
        searchSourceBuilder.query(bool);
        return searchAsMap(searchSourceBuilder);
    }

    public void updateByQuery(List<Query> queries, String field, Object value) throws CodedException, IOException {
        super.updateByQuery(queries, setFieldScript(field, value));
    }

    private Map<Long, Map<String, Object>> searchAsMap(SearchSourceBuilder searchSourceBuilder) throws IOException {
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        return searchAsMap(request);
    }

    private Map<Long, Map<String, Object>> searchAsMap(SearchRequest request) throws IOException {
        return searchAsMap(request, hit -> Long.parseLong(hit.getId()), hit -> deserializer().apply(hit.getSourceAsString()));
    }

    @Override
    protected @NotNull Function<String, Map<String, Object>> deserializer() {
        return s -> {
            try {
                return JSON_MAPPER.readValue(s, Map.class);
            } catch (JsonProcessingException e) {
                log.error("Deserialize ProjectExtend(Map) json exception!", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };
    }
}
