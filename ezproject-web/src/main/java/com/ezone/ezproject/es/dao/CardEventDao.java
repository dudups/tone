package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.EsUtil;
import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.util.EsIndexUtil;
import com.ezone.ezproject.modules.card.bean.query.In;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.card.event.model.EventType;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
@Slf4j
@AllArgsConstructor
public class CardEventDao extends AbstractEsDocDao<Long, CardEvent> {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    protected String index() {
        return EsIndexUtil.indexForCardEvent();
    }

    public void saveOrUpdate(Long id, String eventJson) throws IOException {
        setDocSourceJson(id, eventJson);
    }

    @Override
    public void saveOrUpdate(Long id, CardEvent cardEvent) throws IOException {
        setDocSourceJson(id, JSON_MAPPER.writeValueAsString(cardEvent));
    }

    public void saveOrUpdate(CardEvent cardEvent) throws IOException {
        setDocSourceJson(cardEvent.getId(), JSON_MAPPER.writeValueAsString(cardEvent));
    }

    public void saveOrUpdate(Map<Long, String> cardEventJsons) throws IOException {
        setDocsSources(index(), cardEventJsons, XContentType.JSON);
    }

    public void updateSelective(Long id, Map<String, Object> cardEventProps) throws IOException {
        updateDoc(index(), id, cardEventProps);
    }

    public void updateSelective(Map<Long, Map<String, Object>> cardEventsProps) throws IOException {
        updateDoc(index(), cardEventsProps);
    }

    public void setCalcIsEnd(List<Query> cardPropQueries, boolean isEnd) throws CodedException, IOException {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        queryBuilders(cardPropQueries, this::cardProp).forEach(bool::filter);
        UpdateByQueryRequest request = new UpdateByQueryRequest(index());
        request.setQuery(bool);
        request.setScript(FieldUtil.setCardDetailFieldScript(CardField.CALC_IS_END, isEnd));
        request.setTimeout(EsUtil.TIME_OUT);
        es.updateByQuery(request, EsUtil.REQUEST_OPTIONS);
    }

    public void setDeleted(List<Query> cardPropQueries, boolean deleted) throws CodedException, IOException {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        queryBuilders(cardPropQueries, this::cardProp).forEach(bool::filter);
        UpdateByQueryRequest request = new UpdateByQueryRequest(index());
        request.setQuery(bool);
        request.setScript(FieldUtil.setCardDetailFieldScript(CardField.DELETED, deleted));
        request.setTimeout(EsUtil.TIME_OUT);
        es.updateByQuery(request, EsUtil.REQUEST_OPTIONS);
    }

    public void setPlanId(List<Query> cardPropQueries, Long planId) throws CodedException, IOException {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        queryBuilders(cardPropQueries, this::cardProp).forEach(bool::filter);
        UpdateByQueryRequest request = new UpdateByQueryRequest(index());
        request.setQuery(bool);
        request.setScript(FieldUtil.setCardDetailFieldScript(CardField.PLAN_ID, planId));
        request.setTimeout(EsUtil.TIME_OUT);
        es.updateByQuery(request, EsUtil.REQUEST_OPTIONS);
    }

    public void setNextTime(List<Long> ids, Date nextTime) throws CodedException, IOException {
        UpdateByQueryRequest request = new UpdateByQueryRequest(index());
        request.setQuery(QueryBuilders.idsQuery().addIds(ids.stream().map(String::valueOf).toArray(String[]::new)));
        request.setScript(FieldUtil.setFieldScript(CardEvent.NEXT_DATE, nextTime));
        request.setTimeout(EsUtil.TIME_OUT);
        es.updateByQuery(request, EsUtil.REQUEST_OPTIONS);
    }

    public void setInnerType(List<Long> ids, String innerType) throws CodedException, IOException {
        UpdateByQueryRequest request = new UpdateByQueryRequest(index());
        request.setQuery(QueryBuilders.idsQuery().addIds(ids.stream().map(String::valueOf).toArray(String[]::new)));
        request.setScript(FieldUtil.setCardDetailFieldScript(CardField.INNER_TYPE, innerType));
        request.setTimeout(EsUtil.TIME_OUT);
        es.updateByQuery(request, EsUtil.REQUEST_OPTIONS);
    }

    public Map<String, Object> findAsMap(Long id) throws IOException {
        return getDocSourceAsMap(index(), id);
    }

    public List<CardEvent> searchWithDateDesc(Long cardId) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery(CardEvent.CARD_ID, cardId))
                .filter(QueryBuilders.boolQuery().mustNot(QueryBuilders.termsQuery(CardEvent.EVENT_TYPE, EventType.CALC_IS_END.name(), EventType.PLAN_IS_ACTIVE.name()))));
        searchSourceBuilder.sort(new FieldSortBuilder(CardEvent.DATE).order(SortOrder.DESC));
        searchSourceBuilder.fetchSource(null, CardEvent.CARD_DETAIL);
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        return searchAll(searchSourceBuilder, 100).getList();
    }

    @Override
    protected @NotNull Function<String, CardEvent> deserializer() {
        return s -> {
            try {
                return JSON_MAPPER.readValue(s, CardEvent.class);
            } catch (JsonProcessingException e) {
                log.error("Deserialize CardEvent json exception!", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };
    }

    public void deleteByCardIds(List<Long> cardIds) throws IOException {
        DeleteByQueryRequest request = new DeleteByQueryRequest(index());
        request.setConflicts("proceed");
        request.setQuery(QueryBuilders.termsQuery(CardEvent.CARD_ID, cardIds));
        BulkByScrollResponse response = es.deleteByQuery(request, EsUtil.REQUEST_OPTIONS);
        if (response.getVersionConflicts() > 0) {
            log.warn("There are [{}] version conflicts when delete by card-ids:[{s}]!", response.getVersionConflicts(), cardIds);
        }
    }

    public List<CardEvent> searchForChart(Date start, Date end, List<Query> cardPropQueries, List<String> cardProps) throws IOException {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
//        BoolQueryBuilder should = QueryBuilders.boolQuery();
//        should.should(QueryBuilders.rangeQuery(CardEvent.DATE).from(start).to(end));
//        should.should(QueryBuilders.rangeQuery(CardEvent.NEXT_DATE).from(start).to(end));
//        should.should(QueryBuilders.boolQuery()
//                .filter(QueryBuilders.rangeQuery(CardEvent.DATE).lte(start))
//                .filter(QueryBuilders.rangeQuery(CardEvent.NEXT_DATE).gte(end)));
//        should.should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(CardEvent.NEXT_DATE)));
//        should.minimumShouldMatch(1);
//        bool.filter(should);
        bool.filter(QueryBuilders.rangeQuery(CardEvent.DATE).lte(end));
        bool.filter(QueryBuilders.boolQuery()
                .should(QueryBuilders.rangeQuery(CardEvent.NEXT_DATE).gte(start))
                .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(CardEvent.NEXT_DATE)))
                .minimumShouldMatch(1)
        );


        if (CollectionUtils.isNotEmpty(cardPropQueries)) {
            BoolQueryBuilder filter = QueryBuilders.boolQuery();
            queryBuilders(cardPropQueries, this::cardProp).forEach(filter::filter);
            bool.filter(filter);
        }
        bool.filter(QueryBuilders.termsQuery(CardEvent.EVENT_TYPE, EventType.EVENT_STR_FOR_STAT_CHART));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(bool);
        searchSourceBuilder.sort(new FieldSortBuilder(CardEvent.DATE).order(SortOrder.ASC));
        List<String> fields = new ArrayList<>();
        fields.add(CardEvent.CARD_ID);
        fields.add(CardEvent.DATE);
        fields.add(CardEvent.NEXT_DATE);
        fields.add(CardEvent.EVENT_TYPE);
        if (CollectionUtils.isNotEmpty(cardProps)) {
            cardProps.forEach(p -> fields.add(cardProp(p)));
        }
        searchSourceBuilder.fetchSource(fields.stream().toArray(String[]::new), null);
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        return searchAll(searchSourceBuilder, 10000).getList();
    }

    public List<CardEvent> searchForChart(List<Long> cardIds, List<String> cardProps) throws IOException {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        bool.filter(QueryBuilders.termsQuery(CardEvent.CARD_ID, cardIds));
        bool.filter(QueryBuilders.termsQuery(CardEvent.EVENT_TYPE, EventType.EVENT_STR_FOR_STAT_CHART));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(bool);
        searchSourceBuilder.sort(new FieldSortBuilder(CardEvent.DATE).order(SortOrder.ASC));
        List<String> fields = new ArrayList<>();
        fields.add(CardEvent.CARD_ID);
        fields.add(CardEvent.DATE);
        fields.add(CardEvent.NEXT_DATE);
        if (CollectionUtils.isNotEmpty(cardProps)) {
            cardProps.forEach(p -> fields.add(cardProp(p)));
        }
        searchSourceBuilder.fetchSource(fields.stream().toArray(String[]::new), null);
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        return searchAll(searchSourceBuilder, 10000).getList();
    }

    public List<CardEvent> searchEventIdAndType(List<Long> cardIds) throws IOException {
        return searchEvent(cardIds, Arrays.asList("id"), Arrays.asList(CardField.TYPE), CardEvent.DATE, SortOrder.ASC);
    }

    public List<CardEvent> searchEvent(List<Long> cardIds, List<String> eventFields, List<String> cardProperties, String sortField, SortOrder order) throws IOException {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        bool.filter(QueryBuilders.termsQuery(CardEvent.CARD_ID, cardIds));
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(bool);
        List<String> fields = new ArrayList<>();
        if (!fields.contains("id")) {
            fields.add("id");
        }
        if (CollectionUtils.isNotEmpty(eventFields)) {
            fields.addAll(eventFields);
        }
        if (CollectionUtils.isNotEmpty(cardProperties)) {
            cardProperties.forEach(cardProperty -> {
                        fields.add(cardProp(CardField.TYPE));
                    }
            );
        }
        searchSourceBuilder.fetchSource(fields.stream().toArray(String[]::new), null);
        searchSourceBuilder.sort(new FieldSortBuilder(sortField).order(order));
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        return searchAll(searchSourceBuilder, 10000).getList();
    }

    private TotalBean<CardEvent> searchAll(SearchSourceBuilder searchSourceBuilder, int firstPageSize) throws IOException {
        searchSourceBuilder.from(0).size(firstPageSize);
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        TotalBean<CardEvent> total = search(request, hit -> deserializer().apply(hit.getSourceAsString()));
        if (total.getTotal() > firstPageSize) {
            List<CardEvent> list = new ArrayList<>(total.getList());
            searchSourceBuilder.from(firstPageSize).size((int) total.getTotal() - firstPageSize);
            list.addAll(search(request, hit -> deserializer().apply(hit.getSourceAsString())).getList());
            total.setList(list);
        }
        return total;
    }

    private String cardProp(String prop) {
        return CardEvent.cardProp(prop);
    }

    public Aggregations aggs(List<Query> cardPropQueries, Consumer<SearchSourceBuilder> aggregation) throws IOException {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        if (CollectionUtils.isNotEmpty(cardPropQueries)) {
            BoolQueryBuilder filter = QueryBuilders.boolQuery();
            queryBuilders(cardPropQueries, this::cardProp).forEach(filter::filter);
            bool.filter(filter);
        }
        bool.filter(In.builder().field(CardEvent.EVENT_TYPE).values(EventType.EVENT_STR_FOR_STAT_CHART).build().queryBuilder());
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
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

    public long countByCardField(Long projectId, String cardFieldKey) throws IOException {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        bool.filter(QueryBuilders.termQuery(cardProp(CardField.PROJECT_ID), projectId));
        BoolQueryBuilder should = QueryBuilders.boolQuery();
        should.should(QueryBuilders.boolQuery().must(QueryBuilders.existsQuery(cardProp(cardFieldKey))));
        bool.filter(should);
        return count(index(), bool);
    }

    public void updateByQuery(List<Query> queries, String updateField, Object updateValue) throws CodedException, IOException {
        updateByQuery(queries, updateField, updateValue, EsUtil.TIME_OUT);
    }

    /**
     * @param queries     查询字段，如内容是事件的卡片明细字段，如卡片类型，使用type
     * @param updateField 卡片明细字段，如卡片类型，传type
     * @param updateValue 更新后的值。
     * @throws CodedException
     * @throws IOException
     */
    public void updateByQuery(List<Query> queries, String updateField, Object updateValue, int timeOutSeconds) throws CodedException, IOException {
        updateByQuery(queries, updateField, updateValue, TimeValue.timeValueSeconds(timeOutSeconds));
    }

    private void updateByQuery(List<Query> queries, String updateField, Object updateValue, TimeValue timeValue) throws CodedException, IOException {
        UpdateByQueryRequest request = new UpdateByQueryRequest(index());
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        if (CollectionUtils.isNotEmpty(queries)) {
            queryBuilders(queries).forEach(queryBuilder -> {
                bool.filter(queryBuilder);
            });
        }
        request.setQuery(bool);
        request.setScript(FieldUtil.setCardDetailFieldScript(updateField, updateValue));
        request.setTimeout(timeValue);
        es.updateByQuery(request, EsUtil.REQUEST_OPTIONS);
    }
}
