package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.EsUtil;
import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.card.bean.query.Query;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AbstractEsBaseDao
 * RestHighLevelClient: https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/index.html
 *
 * @param <K> type for doc id
 * @param <O> type for doc source content
 */
@Slf4j
@NoArgsConstructor
@Setter(onMethod_ = {@Autowired})
public abstract class AbstractEsBaseDao<K, O> {
    protected RestHighLevelClient es;

    protected String string(K docId) {
        if (null == docId) {
            return null;
        }
        return String.valueOf(docId);
    }

    @NotNull
    protected abstract Function<String, O> deserializer();

    protected void refresh(String index) {
        try {
            RefreshRequest request = new RefreshRequest(index);
            es.indices().refresh(request, EsUtil.REQUEST_OPTIONS);
        } catch (Exception e) {
            log.error("refresh exception!", e);
        }
    }

    protected O find(String index, K id) throws IOException {
        return getDocSourceObject(index, id);
    }

    protected O find(String index, K id, String... includes) throws IOException {
        return getDocSourceObject(index, id, includes);
    }

    protected List<O> find(String index, List<K> ids) throws IOException {
        return getDocSourceObject(index, ids);
    }

    protected String getDocSource(String index, K docId) throws IOException {
        GetRequest request = new GetRequest(index, string(docId));
        GetResponse response = es.get(request, EsUtil.REQUEST_OPTIONS);
        if (response.isExists()) {
            return response.getSourceAsString();
        }
        return null;
    }

    protected Map<String, Object> getDocSourceAsMap(String index, K docId) throws IOException {
        GetRequest request = new GetRequest(index, string(docId));
        GetResponse response = es.get(request, EsUtil.REQUEST_OPTIONS);
        if (response.isExists()) {
            return response.getSource();
        }
        return null;
    }

    protected Map<String, Object> getDocSourceAsMap(String index, K docId, String[] includes) throws IOException {
        GetRequest request = new GetRequest(index, string(docId));
        request.fetchSourceContext(new FetchSourceContext(true, includes, null));
        GetResponse response = es.get(request, EsUtil.REQUEST_OPTIONS);
        if (response.isExists()) {
            return response.getSource();
        }
        return null;
    }

    protected String getDocSourceString(String index, K docId, String[] includes) throws IOException {
        GetRequest request = new GetRequest(index, string(docId));
        request.fetchSourceContext(new FetchSourceContext(true, includes, null));
        GetResponse response = es.get(request, EsUtil.REQUEST_OPTIONS);
        if (response.isExists()) {
            return response.getSourceAsString();
        }
        return null;
    }

    protected O getDocSourceObject(String index, K docId, String[] includes) throws IOException {
        String source = getDocSourceString(index, docId, includes);
        if (null == source) {
            return null;
        }
        return deserializer().apply(source);
    }

    protected O getDocSourceObject(String index, K docId) throws IOException {
        String source = getDocSource(index, docId);
        if (null == source) {
            return null;
        }
        return deserializer().apply(source);
    }

    protected List<String> getDocSource(String index, List<K> docIds) throws IOException {
        MultiGetRequest request = new MultiGetRequest();
        docIds.stream().distinct().forEach(docId -> request.add(new MultiGetRequest.Item(index, string(docId))));
        MultiGetResponse response = es.mget(request, EsUtil.REQUEST_OPTIONS);
        return Arrays.stream(response.getResponses())
                .filter(item -> item.getResponse().isExists())
                .map(item -> item.getResponse().getSourceAsString())
                .collect(Collectors.toList());
    }

    protected List<O> getDocSourceObject(String index, List<K> docIds) throws IOException {
        MultiGetRequest request = new MultiGetRequest();
        docIds.stream().distinct().forEach(docId -> request.add(new MultiGetRequest.Item(index, string(docId))));
        MultiGetResponse response = es.mget(request, EsUtil.REQUEST_OPTIONS);
        return Arrays.stream(response.getResponses())
                .filter(item -> item.getResponse().isExists())
                .map(item -> deserializer().apply(item.getResponse().getSourceAsString()))
                .collect(Collectors.toList());
    }

    protected <T> TotalBean<T> getDocSourceObject(String index, List<K> docIds, String[] includes, Function<GetResponse, T> function) throws IOException {
        MultiGetResponse response = getDocSources(index, docIds, includes);
        return TotalBean.<T>builder()
                .total(response.getResponses().length)
                .list(Arrays.stream(response.getResponses())
                        .filter(r -> r.getResponse().isExists())
                        .map(r -> function.apply(r.getResponse()))
                        .collect(Collectors.toList())
                )
                .build();
    }

    /**
     * docId不存在也会返回Response，注意过滤.filter(r -> r.getResponse().isExists())
     */
    protected MultiGetResponse getDocSources(String index, List<K> docIds, String[] includes) throws IOException {
        MultiGetRequest request = new MultiGetRequest();
        FetchSourceContext fetchSourceContext = new FetchSourceContext(true, includes, null);
        docIds.stream().distinct().forEach(docId -> request.add(new MultiGetRequest.Item(index, string(docId)).fetchSourceContext(fetchSourceContext)));
        return es.mget(request, EsUtil.REQUEST_OPTIONS);
    }

    /**
     * create or update
     */
    protected void setDocSourceYaml(String index, K docId, String yaml) throws CodedException, IOException {
        setDocSource(index, docId, yaml, XContentType.YAML);
    }

    /**
     * create or update
     */
    protected void setDocSourceJson(String index, K docId, String json) throws CodedException, IOException {
        setDocSource(index, docId, json, XContentType.JSON);
    }

    /**
     * create or update
     */
    protected void setDocSource(String index, K docId, String docSource, XContentType docContentType)
            throws CodedException, IOException {
        IndexRequest request = new IndexRequest(index)
                .id(String.valueOf(docId))
                .source(docSource, docContentType)
                .opType(DocWriteRequest.OpType.INDEX)
                .timeout(EsUtil.TIME_OUT);

        IndexResponse response = es.index(request, EsUtil.REQUEST_OPTIONS);
        if (!(RestStatus.OK.equals(response.status()) || RestStatus.CREATED.equals(response.status()))) {
            throw new CodedException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("Es update response:[%s]", response.status())
            );
        }
    }

    /**
     * batch create or update
     */
    protected void setDocsSources(String index, Map<K, String> docs, XContentType docContentType)
            throws CodedException, IOException {
        BulkRequest request = new BulkRequest();
        docs.entrySet().forEach(entry -> {
            K docId = entry.getKey();
            request.add(new IndexRequest(index)
                    .id(String.valueOf(docId))
                    .source(entry.getValue(), docContentType)
                    .opType(DocWriteRequest.OpType.INDEX));
        });
        request.timeout(EsUtil.TIME_OUT);
        BulkResponse response = es.bulk(request, EsUtil.REQUEST_OPTIONS);
        if (!(RestStatus.OK.equals(response.status()))) {
            throw new CodedException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("Es index update response:[%s]", response.status())
            );
        }
        if (response.hasFailures()) {
            log.error(response.buildFailureMessage());
        }
    }

    /**
     * batch create or update
     */
    protected void setDocsSources(String index, Map<K, O> docs, Function<O, String> function, XContentType docContentType)
            throws CodedException, IOException {
        BulkRequest request = new BulkRequest();
        docs.entrySet().forEach(entry -> {
            K docId = entry.getKey();
            request.add(new IndexRequest(index)
                    .id(String.valueOf(docId))
                    .source(function.apply(entry.getValue()), docContentType)
                    .opType(DocWriteRequest.OpType.INDEX));
        });
        request.timeout(EsUtil.TIME_OUT);
        BulkResponse response = es.bulk(request, EsUtil.REQUEST_OPTIONS);
        if (!(RestStatus.OK.equals(response.status()))) {
            throw new CodedException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("Es index update response:[%s]", response.status())
            );
        }
        if (response.hasFailures()) {
            log.error(response.buildFailureMessage());
        }
    }

    /**
     * batch create or update
     */
    protected void setDocsSources(String index, Map<K, Map<String, Object>> docs) throws CodedException, IOException {
        BulkRequest request = new BulkRequest();
        docs.entrySet().forEach(entry -> {
            K docId = entry.getKey();
            request.add(new IndexRequest(index)
                    .id(String.valueOf(docId))
                    .source(entry.getValue())
                    .opType(DocWriteRequest.OpType.INDEX));
        });
        request.timeout(EsUtil.TIME_OUT);
        BulkResponse response = es.bulk(request, EsUtil.REQUEST_OPTIONS);
        if (!(RestStatus.OK.equals(response.status()))) {
            throw new CodedException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("Es index update response:[%s]", response.status())
            );
        }
        if (response.hasFailures()) {
            log.error(response.buildFailureMessage());
        }
    }

    /**
     * update props; 注意：更新请求es，传的是Date.toString，直接传Date日期格式有问题
     */
    protected void updateDoc(String index, K docId, Map<String, Object> map) throws CodedException, IOException {

        UpdateRequest request = new UpdateRequest(index, string(docId))
                .id(String.valueOf(docId))
                .doc(map)
                .timeout(EsUtil.TIME_OUT);

        UpdateResponse response = es.update(request, EsUtil.REQUEST_OPTIONS);
        if (!(RestStatus.OK.equals(response.status()))) {
            throw new CodedException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("Es update response:[%s]", response.status())
            );
        }
    }

    /**
     * update props; 注意：更新请求es，传的是Date.toString，直接传Date日期格式有问题
     */
    protected void updateDoc(String index, List<K> docIds, Map<String, Object> map) throws CodedException, IOException {
        BulkRequest request = new BulkRequest();
        docIds.forEach(docId -> {
            request.add(new UpdateRequest(index, string(docId)).doc(map));
        });
        request.timeout(EsUtil.TIME_OUT);
        BulkResponse response = es.bulk(request, EsUtil.REQUEST_OPTIONS);
        if (!(RestStatus.OK.equals(response.status()))) {
            throw new CodedException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("Es doc update response:[%s]", response.status())
            );
        }
        if (response.hasFailures()) {
            log.error(response.buildFailureMessage());
        }
    }

    /**
     * update props; 注意：更新请求es，传的是Date.toString，直接传Date日期格式有问题
     */
    protected void updateDoc(String index, Map<K, Map<String, Object>> map) throws CodedException, IOException {
        BulkRequest request = new BulkRequest();
        map.entrySet().forEach(entry -> {
            request.add(new UpdateRequest(index, string(entry.getKey())).doc(entry.getValue()));
        });
        request.timeout(EsUtil.TIME_OUT);
        BulkResponse response = es.bulk(request, EsUtil.REQUEST_OPTIONS);
        if (!(RestStatus.OK.equals(response.status()))) {
            throw new CodedException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("Es doc update response:[%s]", response.status())
            );
        }
        if (response.hasFailures()) {
            log.error(response.buildFailureMessage());
        }
    }

    protected void deleteDoc(String index, List<K> docIds) throws IOException {
        DeleteByQueryRequest request = new DeleteByQueryRequest(index).setTimeout(EsUtil.TIME_OUT);
        request.setConflicts("proceed");
        request.setQuery(QueryBuilders.idsQuery().addIds(docIds.stream().map(String::valueOf).toArray(String[]::new)));
        BulkByScrollResponse response = es.deleteByQuery(request, EsUtil.REQUEST_OPTIONS);
        if (response.getVersionConflicts() > 0) {
            log.warn("There are [{}] version conflicts for ids:[{s}]!", response.getVersionConflicts(), docIds);
        }
    }

    protected void deleteDoc(String index, K docId) throws IOException {
        DeleteRequest request = new DeleteRequest(index, String.valueOf(docId)).timeout(EsUtil.TIME_OUT);
        es.delete(request, EsUtil.REQUEST_OPTIONS);
    }

    protected void deleteDocByQuery(String index, List<Query> queries) throws IOException {
        DeleteByQueryRequest request = new DeleteByQueryRequest(index).setTimeout(EsUtil.TIME_OUT);
        request.setConflicts("proceed");
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        queryBuilders(queries).forEach(queryBuilder -> {
            bool.filter(queryBuilder);
        });
        request.setQuery(bool);
        BulkByScrollResponse response = es.deleteByQuery(request, EsUtil.REQUEST_OPTIONS);
        if (response.getVersionConflicts() > 0) {
            log.warn("There are [{}] version conflicts!", response.getVersionConflicts());
        }
    }

    protected <T> TotalBean<T> search(SearchRequest request, Function<SearchHit, T> function) throws IOException {
        SearchResponse response = es.search(request, EsUtil.REQUEST_OPTIONS);
        SearchHits hits = response.getHits();
        return TotalBean.<T>builder()
                .total(hits.getTotalHits().value)
                .list(Arrays.stream(hits.getHits()).map(function).collect(Collectors.toList()))
                .build();
    }

    protected Map<K, O> searchAsMap(SearchRequest request, Function<SearchHit, K> keyConvert, Function<SearchHit, O> function) throws IOException {
        SearchResponse response = es.search(request, EsUtil.REQUEST_OPTIONS);
        SearchHits hits = response.getHits();
        Map<K, O> result = new HashMap<>();
        Arrays.stream(hits.getHits()).forEach(hit -> {
            result.put(keyConvert.apply(hit), function.apply(hit));
        });
        return result;
    }

    protected long count(String index, QueryBuilder query) throws IOException {
        CountRequest request = new CountRequest()
                .indices(index)
                .source(new SearchSourceBuilder().query(query));
        CountResponse response = es.count(request, EsUtil.REQUEST_OPTIONS);
        return response.getCount();
    }

    protected List<Long> searchAllIds(String index, QueryBuilder query) throws IOException {
        List<Long> ids = new ArrayList<>();
        SearchRequest request = new SearchRequest()
                .indices(index)
                .source(new SearchSourceBuilder()
                        .query(query)
                        .size(1000)
                        .sort(SortBuilders.fieldSort("_doc").order(SortOrder.DESC))
                        .fetchSource(false));
        request.scroll(TimeValue.timeValueSeconds(10));
        SearchResponse response = es.search(request, EsUtil.REQUEST_OPTIONS);
        String scrollId = response.getScrollId();
        SearchHits hits = response.getHits();

        try {
            while (hits.getHits().length > 0) {
                hits.forEach(hit -> ids.add(NumberUtils.toLong(hit.getId())));
                if (ids.size() >= hits.getTotalHits().value) {
                    break;
                }
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(TimeValue.timeValueSeconds(10));
                response = es.scroll(scrollRequest, EsUtil.REQUEST_OPTIONS);
                scrollId = response.getScrollId();
                hits = response.getHits();
            }
        } finally {
            try {
                ClearScrollRequest clear = new ClearScrollRequest();
                clear.addScrollId(scrollId);
                es.clearScroll(clear, EsUtil.REQUEST_OPTIONS);
            } catch (Exception e) {
                log.warn("clear scroll exception!", e);
            }
        }

        return ids;
    }

    protected void scrollAll(String index, List<Query> queries, Consumer<SearchHit> consumer, String... includes) throws IOException {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        queryBuilders(queries).forEach(queryBuilder -> bool.filter(queryBuilder));
        SearchRequest request = new SearchRequest()
                .indices(index)
                .source(new SearchSourceBuilder()
                        .query(bool)
                        .size(1000)
                        .fetchSource(includes != null && includes.length > 0)
                        .fetchSource(includes, null));
        request.scroll(TimeValue.timeValueSeconds(10));
        SearchResponse response = es.search(request, EsUtil.REQUEST_OPTIONS);
        String scrollId = response.getScrollId();
        SearchHits hits = response.getHits();
        try {
            while (hits.getHits().length > 0) {
                hits.forEach(consumer::accept);
                if (hits.getTotalHits().value < 1000) {
                    break;
                }
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(TimeValue.timeValueSeconds(10));
                response = es.scroll(scrollRequest, EsUtil.REQUEST_OPTIONS);
                scrollId = response.getScrollId();
                hits = response.getHits();
            }
        } finally {
            try {
                ClearScrollRequest clear = new ClearScrollRequest();
                clear.addScrollId(scrollId);
                es.clearScroll(clear, EsUtil.REQUEST_OPTIONS);
            } catch (Exception e) {
                log.warn("clear scroll exception!", e);
            }
        }
    }

    protected List<QueryBuilder> queryBuilders(List<Query> queries) {
        return queryBuilders(queries, Function.identity());
    }

    protected List<QueryBuilder> queryBuilders(List<Query> queries, Function<String, String> fieldConverter) {
        List<QueryBuilder> queryBuilders = new ArrayList<>();
        if (CollectionUtils.isEmpty(queries)) {
            return queryBuilders;
        }
        queries.forEach(query -> {
            QueryBuilder queryBuilder = query.queryBuilder(fieldConverter);
            if (null != queryBuilder) {
                queryBuilders.add(queryBuilder);
            }
        });
        return queryBuilders;
    }

    protected SearchSourceBuilder searchSourceBuilder(SearchEsRequest searchEsRequest) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        queryBuilders(searchEsRequest.getQueries()).forEach(queryBuilder -> bool.filter(queryBuilder));
        if (CollectionUtils.isNotEmpty(searchEsRequest.getSorts())) {
            searchEsRequest.getSorts().forEach(sort ->
                    searchSourceBuilder.sort(new FieldSortBuilder(sort.getField()).order(sort.getOrder()))
            );
        }
        searchSourceBuilder.query(bool);
        searchSourceBuilder.fetchSource(searchEsRequest.getFields(), null);
        return searchSourceBuilder;
    }

    protected SearchSourceBuilder searchSourceBuilder(List<Query> queries, String... includes) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        queryBuilders(queries).forEach(queryBuilder -> bool.filter(queryBuilder));
        searchSourceBuilder.query(bool);
        searchSourceBuilder.fetchSource(includes, null);
        return searchSourceBuilder;
    }

    /**
     * update props; 注意：更新请求es，传的是Date.toString，直接传Date日期格式有问题
     */
    protected void updateDoc(String index, List<K> docIds, Map<String, Object> map, List<K> subDocIds, Map<String, Object> subAddCardProps) throws CodedException, IOException {
        BulkRequest request = new BulkRequest();
        docIds.forEach(docId -> {
            if (subDocIds != null && subDocIds.contains(docId) && subAddCardProps != null) {
                map.putAll(subAddCardProps);
            }
            request.add(new UpdateRequest(index, string(docId)).doc(map));
        });
        request.timeout(EsUtil.TIME_OUT);
        BulkResponse response = es.bulk(request, EsUtil.REQUEST_OPTIONS);
        if (!(RestStatus.OK.equals(response.status()))) {
            throw new CodedException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("Es doc update response:[%s]", response.status())
            );
        }
        if (response.hasFailures()) {
            log.error(response.buildFailureMessage());
        }
    }
}
