package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.ProjectAlarmExt;
import com.ezone.ezproject.es.util.EsIndexUtil;
import com.ezone.ezproject.modules.alarm.bean.AlarmItem;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@AllArgsConstructor
public class ProjectAlarmDao extends AbstractEsBaseDao<Long, ProjectAlarmExt> {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    protected String index() {
        return EsIndexUtil.indexProjectAlarmConfig();
    }

    public ProjectAlarmExt find(Long id) throws IOException {
        return find(index(), id);
    }

    public void saveOrUpdate(Long id, ProjectAlarmExt projectAlarm) throws IOException {
        setDocSourceJson(index(), id, JSON_MAPPER.writeValueAsString(projectAlarm));
    }

    public void saveOrUpdate(List<ProjectAlarmExt> projectAlarms) throws IOException {
        Map<Long, String> alarmMap = new HashMap<>();
        for (ProjectAlarmExt projectAlarm : projectAlarms) {
            alarmMap.put(projectAlarm.getId(), JSON_MAPPER.writeValueAsString(projectAlarm));
        }
        setDocsSources(index(), alarmMap, XContentType.JSON);
    }

    @Override
    protected @NotNull Function<String, ProjectAlarmExt> deserializer() {
        return s -> {
            try {
                return JSON_MAPPER.readValue(s, ProjectAlarmExt.class);
            } catch (JsonProcessingException e) {
                log.error("Deserialize ProjectMenu json exception!", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };
    }

    public List<ProjectAlarmExt> search(Boolean active, List<AlarmItem.Type> types, Integer pageNumber, Integer pageSize, String sortField, SortOrder order) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        List<QueryBuilder> queries = new ArrayList<>();
        if (active != null) {
            queries.add(QueryBuilders.termQuery(ProjectAlarmExt.FIELD_ES_ACTIVE, active));
        }
        if (CollectionUtils.isNotEmpty(types)) {
            queries.add(QueryBuilders.termsQuery(ProjectAlarmExt.FIELD_ES_TYPE, types.stream().map(Enum::name).collect(Collectors.toList())));
        }
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        queries.forEach(boolQuery::filter);
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.from((pageNumber - 1) * pageSize).size(pageSize);
        searchSourceBuilder.sort(sortField, order);
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        return search(request);
    }

    public List<ProjectAlarmExt> findByProjectId(Long projectId) throws IOException {
        return findByProjectId(projectId, null);
    }

    public List<ProjectAlarmExt> findByProjectId(Long projectId, Boolean active) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        List<QueryBuilder> queries = new ArrayList<>();
        queries.add(QueryBuilders.termQuery(ProjectAlarmExt.FIELD_ES_PROJECT_ID, projectId));
        if (active != null) {
            queries.add(QueryBuilders.termQuery(ProjectAlarmExt.FIELD_ES_ACTIVE, active));
        }
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        queries.forEach(boolQuery::filter);
        searchSourceBuilder
                .query(boolQuery)
                .sort(SortBuilders.fieldSort(ProjectAlarmExt.FIELD_ES_CREATE_TIME).order(SortOrder.DESC));
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        return searchAll(searchSourceBuilder, 1000).getList();
    }

    public List<ProjectAlarmExt> findByProjectIds(List<Long> projectIds, Boolean active) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        List<QueryBuilder> queries;
        if (active != null) {
            queries = Arrays.asList(QueryBuilders.termsQuery(ProjectAlarmExt.FIELD_ES_PROJECT_ID, projectIds),
                    QueryBuilders.termQuery(ProjectAlarmExt.FIELD_ES_ACTIVE, active));
        } else {
            queries = Arrays.asList(QueryBuilders.termsQuery(ProjectAlarmExt.FIELD_ES_PROJECT_ID, projectIds));
        }
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        queries.forEach(boolQuery::filter);
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.sort(SortBuilders.fieldSort(ProjectAlarmExt.FIELD_ES_CREATE_TIME).order(SortOrder.DESC));
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        return searchAll(searchSourceBuilder, 1000).getList();
    }

    private TotalBean<ProjectAlarmExt> searchAll(SearchSourceBuilder searchSourceBuilder, int firstPageSize) throws IOException {
        searchSourceBuilder.from(0).size(firstPageSize);
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        TotalBean<ProjectAlarmExt> total = search(request, hit -> deserializer().apply(hit.getSourceAsString()));
        if (total.getTotal() > firstPageSize) {
            List<ProjectAlarmExt> list = new ArrayList<>(total.getList());
            searchSourceBuilder.from(firstPageSize).size((int) total.getTotal() - firstPageSize);
            list.addAll(search(request, hit -> deserializer().apply(hit.getSourceAsString())).getList());
            total.setList(list);
        }
        return total;
    }

    private List<ProjectAlarmExt> search(SearchRequest request) throws IOException {
        return search(request, hit -> deserializer().apply(hit.getSourceAsString())).getList();
    }

    public void delete(Long id) throws IOException {
        deleteDoc(index(), id);
    }
}
