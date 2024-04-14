package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.EsUtil;
import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.template.JsTemplate;
import com.ezone.ezproject.common.transactional.AfterCommit;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.util.EsIndexUtil;
import com.ezone.ezproject.modules.card.bean.CardBean;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.project.data.CardTypesParser;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.InnerHitBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.collapse.CollapseBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.xcontent.ToXContent;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;

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

@Component
@Slf4j
@AllArgsConstructor
public class CardDao extends AbstractEsBaseDao<Long, String> {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static final ObjectMapper AGG_MAPPER = Jackson2ObjectMapperBuilder.json()
            .serializerByType(Long.class, ToStringSerializer.instance)
            .serializerByType(Long.TYPE, ToStringSerializer.instance)
            .failOnUnknownProperties(false)
            .build();

    public static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<Map<String, Object>>() {};

    public static String toJson(Map<String, Object> cardDetail) {
        try {
            return JSON_MAPPER.writeValueAsString(cardDetail);
        } catch (JsonProcessingException var2) {
            log.error("Serialize {} error", cardDetail, var2);
            return null;
        }
    }

    @AfterCommit
    public void refresh() {
        refresh(index());
    }

    @Override
    protected @NotNull Function<String, String> deserializer() {
        return Function.identity();
    }

    public void saveOrUpdate(Long cardId, String cardJson) throws IOException {
        setDocSourceJson(index(), cardId, cardJson);
    }

    public void saveOrUpdate(Long cardId, Map<String, Object> cardJson) throws IOException {
        setDocSourceJson(index(), cardId, JSON_MAPPER.writeValueAsString(cardJson));
    }

    public void saveOrUpdate(Map<Long, Map<String, Object>> cardJsons) throws IOException {
        setDocsSources(index(), cardJsons);
    }

    /**
     * 注意：更新请求es，传的是Date.toString，直接传Date日期格式有问题
     */
    public void updateSelective(Long cardId, Map<String, Object> cardProps) throws IOException {
        updateDoc(index(), cardId, cardProps);
    }

    /**
     * 注意：更新请求es，传的是Date.toString，直接传Date日期格式有问题
     */
    public void updateSelective(List<Long> cardIds, Map<String, Object> cardProps) throws IOException {
        updateDoc(index(), cardIds, cardProps);
    }

    /**
     * 注意：更新请求es，传的是Date.toString，直接传Date日期格式有问题
     */
    public void updateSelective(Map<Long, Map<String, Object>> cardProps) throws IOException {
        updateDoc(index(), cardProps);
    }

    public void updateByQuery(List<Query> queries, Map<String, Object> props) throws IOException {
        List<Long> ids = searchIds(queries);
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        updateDoc(index(), ids, props);
    }

    public void updateByQuery(List<Query> queries, String field, Object value) throws IOException {
        List<Long> ids = searchIds(queries);
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        Map<String, Object> props = new HashMap<>();
        props.put(field, value);
        updateDoc(index(), ids, props);
    }

    public void updateByQuery(Query query, String field, Object value) throws IOException {
        updateByQuery(Arrays.asList(query), field, value);
    }

    public void updateByQuery(Query query, Map<String, Object> props) throws IOException {
        updateByQuery(Arrays.asList(query), props);
    }


    public String find(Long cardId) throws IOException {
        return find(index(), cardId);
    }

    public Map<String, Object> findAsMap(Long cardId) {
        try {
            return getDocSourceAsMap(index(), cardId);
        } catch (IOException e) {
            log.error(String.format("Get card:[%s] exception!", cardId), e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public Map<String, Object> findAsMap(Long cardId, String... fields) {
        try {
            return getDocSourceAsMap(index(), cardId, fields);
        } catch (IOException e) {
            log.error(String.format("Get card:[%s] exception!", cardId), e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public Map<Long, Map<String, Object>> findAsMap(List<Long> cardIds) throws IOException {
        return findAsMap(cardIds, null);
    }

    public Map<Long, Map<String, Object>> findAsMap(List<Long> cardIds, String... fields) throws IOException {
        if (CollectionUtils.isEmpty(cardIds)) {
            return MapUtils.EMPTY_MAP;
        }
        MultiGetResponse multiGetResponse = getDocSources(index(), cardIds, fields);
        return Arrays.stream(multiGetResponse.getResponses())
                .filter(r -> r.getResponse().isExists())
                .collect(Collectors.toMap(r -> NumberUtils.toLong(r.getId()), r -> r.getResponse().getSource()));
    }

    public List<String> find(List<Long> cardIds) throws IOException {
        return find(index(), cardIds);
    }

    public void delete(Long cardId) throws IOException {
        deleteDoc(index(), cardId);
    }

    public void delete(List<Long> cardIds) throws IOException {
        deleteDoc(index(), cardIds);
    }

    protected String index() {
        return EsIndexUtil.indexForCard();
    }

    @Deprecated
    protected String index(Long projectId) {
        return EsIndexUtil.indexForCard(projectId);
    }

    public TotalBean<CardBean> search(List<Long> ids, SearchEsRequest searchCardRequest, Integer pageNumber, Integer pageSize) throws IOException {
        if (CollectionUtils.isEmpty(ids)) {
            return TotalBean.<CardBean>builder().build();
        }
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        bool.filter(QueryBuilders.idsQuery().addIds(ids.stream().map(String::valueOf).toArray(String[]::new)));
        queryBuilders(searchCardRequest.getQueries()).forEach(queryBuilder -> {
            bool.filter(queryBuilder);
        });
        searchSourceBuilder.query(bool);
        if (CollectionUtils.isNotEmpty(searchCardRequest.getSorts())) {
            searchCardRequest.getSorts().forEach(sort -> {
                searchSourceBuilder.sort(new FieldSortBuilder(sort.getField()).order(sort.getOrder()));
            });
        }
        searchSourceBuilder.fetchSource(searchCardRequest.getFields(), null);
        searchSourceBuilder.from((pageNumber - 1) * pageSize).size(pageSize);
        return search(searchSourceBuilder);
    }

    public TotalBean<CardBean> search(SearchEsRequest searchCardRequest, Integer pageNumber, Integer pageSize) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        queryBuilders(searchCardRequest.getQueries()).forEach(queryBuilder -> {
            bool.filter(queryBuilder);
        });
        searchSourceBuilder.query(bool);

        if (CollectionUtils.isNotEmpty(searchCardRequest.getSorts())) {
            searchCardRequest.getSorts().forEach(sort -> {
                searchSourceBuilder.sort(new FieldSortBuilder(sort.getField()).order(sort.getOrder()));
            });
        }

        searchSourceBuilder.fetchSource(searchCardRequest.getFields(), null);

        searchSourceBuilder.from((pageNumber - 1) * pageSize).size(pageSize);

        return search(searchSourceBuilder);
    }

    public List<Long> searchIds(List<Long> ids, List<Query> queries) throws IOException {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        bool.filter(QueryBuilders.idsQuery().addIds(ids.stream().map(String::valueOf).toArray(String[]::new)));
        queryBuilders(queries).forEach(queryBuilder -> {
            bool.filter(queryBuilder);
        });
        return searchAllIds(index(), bool);
    }

    public List<Long> searchIds(List<Query> queries) throws IOException {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        queryBuilders(queries).forEach(bool::filter);
        return searchAllIds(index(), bool);
    }

    public List<Long> searchIds(Query... queries) throws IOException {
        return searchIds(Arrays.asList(queries));
    }

    public long count(List<Query> queries) throws IOException {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        queryBuilders(queries).forEach(queryBuilder -> {
            bool.filter(queryBuilder);
        });
        return count(index(), bool);
    }

    public Map<String, Long> countGroupBy(String groupByField, int groupSize, Query... queries) throws Exception {
        String agg = agg(Arrays.asList(queries), builder -> builder.aggregation(AggregationBuilders.terms("group_by").field(groupByField).size(groupSize)));
        String json = JsTemplate.render("/js/CountGroupBy.js").render(agg);
        return JSON_MAPPER.readValue(json, new TypeReference<Map<String, Long>>() {});
    }

    public long countNotEnd(List<Long> planIds) throws IOException {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        bool.filter(QueryBuilders.termsQuery(CardField.PLAN_ID, planIds));
        bool.filter(QueryBuilders.termQuery(CardField.DELETED, false));
        bool.filter(QueryBuilders.termQuery(CardField.CALC_IS_END, false));
        return count(index(), bool);
    }

    private TotalBean<CardBean> search(SearchSourceBuilder searchSourceBuilder) throws IOException {
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        return search(request);
    }

    private TotalBean<CardBean> search(SearchRequest request) throws IOException {
        return search(request, hit -> CardBean.builder().id(NumberUtils.toLong(hit.getId())).card(hit.getSourceAsMap()).build());
    }

    public String agg(List<Query> queries, Consumer<SearchSourceBuilder> aggregation) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        queryBuilders(queries).forEach(queryBuilder -> bool.filter(queryBuilder));
        searchSourceBuilder.query(bool);
        searchSourceBuilder.fetchSource(false);
        searchSourceBuilder.size(0);

        aggregation.accept(searchSourceBuilder);

        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        SearchResponse response = es.search(request, EsUtil.REQUEST_OPTIONS);
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        response.getAggregations().toXContent(builder, ToXContent.EMPTY_PARAMS);
        builder.endObject();
        String json = BytesReference.bytes(builder).utf8ToString();
        // 无论后续后端js提取数据还是最终返回给前端，都要处理long类型超长问题，故此处统一处理
        return AGG_MAPPER.writeValueAsString(JSON_MAPPER.readValue(json, Map.class));
    }

    public Aggregations aggs(List<Query> queries, Consumer<SearchSourceBuilder> aggregation) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        queryBuilders(queries).forEach(queryBuilder -> bool.filter(queryBuilder));
        searchSourceBuilder.query(bool);
        searchSourceBuilder.fetchSource(false);
        searchSourceBuilder.size(0);

        aggregation.accept(searchSourceBuilder);

        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        SearchResponse response = es.search(request, EsUtil.REQUEST_OPTIONS);
        return response.getAggregations();
    }

    public List<Map<String, Object>> searchAsMap(List<Query> queries, String... includes) throws IOException {
        List<Map<String, Object>> cards = new ArrayList<>();
        scrollAll(index(), queries, hit -> cards.add(hit.getSourceAsMap()), includes);
        return cards;
    }

    public List<Map<String, Object>> searchAsMap(List<Query> queries, List<String> includes) throws IOException {
        return searchAsMap(queries, includes.stream().toArray(String[]::new));
    }

    /**
     * 批量更新
     *
     * @param cardIds         卡片Id集合
     * @param cardProps       所有cardIds共同属性
     * @param subIds          cardIds中的子集
     * @param subAddCardProps 子集subIds中需要额外增加的属性值
     * @throws IOException
     */
    public void updateSelective(List<Long> cardIds, Map<String, Object> cardProps, List<Long> subIds, Map<String, Object> subAddCardProps) throws IOException {
        updateDoc(index(), cardIds, cardProps, subIds, subAddCardProps);
    }

    /**
     * 使用折叠查询，如果search数据量超过es窗口（默认10000），可能会丢失类型。，
     * @param queries
     * @return
     * @throws IOException
     */
    @Deprecated
    public List<String> searchCardTypes2(List<Query> queries) throws IOException {
        CollapseBuilder collapseBuilder = new CollapseBuilder(CardField.TYPE);
        InnerHitBuilder innerHitBuilder = new InnerHitBuilder();
        innerHitBuilder.setName("test");
        innerHitBuilder.setSize(0);
        innerHitBuilder.setTrackScores(true);
        innerHitBuilder.setIgnoreUnmapped(true);
        collapseBuilder.setInnerHits(innerHitBuilder);

        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        queryBuilders(queries).forEach(queryBuilder -> bool.filter(queryBuilder));

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(bool);
        searchSourceBuilder.collapse(collapseBuilder);
        searchSourceBuilder.fetchSource(CardField.TYPE,null);
        TotalBean<CardBean> searchResult = search(searchSourceBuilder);
        return searchResult.getList().stream().map(cardBean -> FieldUtil.getType(cardBean.getCard())).collect(Collectors.toList());
    }

    /**
     * 从query中过滤，查询所有使用过的卡片类型。
     * @param queries
     * @return
     */
    public List<String> searchCardTypes(List<Query> queries) {
        List<Query> all = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(queries)) {
            all.addAll(queries);
        }
        Consumer<SearchSourceBuilder> cardBuilderConsumer = builder -> builder
                .aggregation(AggregationBuilders.terms("cardTypes").field(CardField.TYPE).size(100));
        Aggregations aggs = null;
        try {
            aggs = aggs(all, cardBuilderConsumer);
        } catch (Exception e) {
            log.error("cardSummary exception!", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        return new CardTypesParser().parse(aggs);
    }
}
