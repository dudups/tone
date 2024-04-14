package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.EsUtil;
import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.util.EsIndexUtil;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.card.event.model.CardIncrWorkload;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
@Slf4j
@AllArgsConstructor
public class CardIncrWorkloadDao extends AbstractEsDocDao<Long, CardIncrWorkload> {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    protected String index() {
        return EsIndexUtil.indexForCardIncrWorkload();
    }

    @Override
    public void saveOrUpdate(Long id, CardIncrWorkload workload) throws IOException {
        setDocSourceJson(id, JSON_MAPPER.writeValueAsString(workload));
    }

    public void saveOrUpdate(CardIncrWorkload workload) throws IOException {
        setDocSourceJson(workload.getId(), JSON_MAPPER.writeValueAsString(workload));
    }

    public List<Long> searchIds(Query query) throws IOException {
        return searchAllIds(index(), query.queryBuilder());
    }

    public List<CardIncrWorkload> searchByCardId(Long cardId) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder
                .query(QueryBuilders.termQuery(CardIncrWorkload.CARD_ID, cardId))
                .sort(SortBuilders.fieldSort("_doc").order(SortOrder.DESC));
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        return searchAll(searchSourceBuilder, 1000).getList();
    }

    public CardIncrWorkload searchByFlowId(Long flowId) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termQuery(CardIncrWorkload.FLOW_ID, flowId));
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        List<CardIncrWorkload> flows = searchAll(searchSourceBuilder, 10).getList();
        if (CollectionUtils.isEmpty(flows)) {
            return null;
        }
        return flows.get(0);
    }

    public CardIncrWorkload searchByRevertFlowId(Long flowId) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termQuery(CardIncrWorkload.REVERT_FLOW_ID, flowId));
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        List<CardIncrWorkload> flows = searchAll(searchSourceBuilder, 10).getList();
        if (CollectionUtils.isEmpty(flows)) {
            return null;
        }
        return flows.get(0);
    }

    public @Nullable List<CardIncrWorkload> searchByFlowIds(List<Long> flowIds, int pageSize) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termsQuery(CardIncrWorkload.FLOW_ID, flowIds));
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        List<CardIncrWorkload> flows = searchAll(searchSourceBuilder, pageSize).getList();
        if (CollectionUtils.isEmpty(flows)) {
            return null;
        }
        return flows;
    }

    public void deleteByCardIds(List<Long> cardIds) throws IOException {
        DeleteByQueryRequest request = new DeleteByQueryRequest(index());
        request.setConflicts("proceed");
        request.setQuery(QueryBuilders.termsQuery(CardIncrWorkload.CARD_ID, cardIds));
        BulkByScrollResponse response = es.deleteByQuery(request, EsUtil.REQUEST_OPTIONS);
        if (response.getVersionConflicts() > 0) {
            log.warn("There are [{}] version conflicts when delete by card-ids:[{s}]!", response.getVersionConflicts(), cardIds);
        }
    }

    private TotalBean<CardIncrWorkload> searchAll(SearchSourceBuilder searchSourceBuilder, int firstPageSize) throws IOException {
        searchSourceBuilder.from(0).size(firstPageSize);
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        TotalBean<CardIncrWorkload> total = search(request, hit -> deserializer().apply(hit.getSourceAsString()));
        if (total.getTotal() > firstPageSize) {
            List<CardIncrWorkload> list = new ArrayList<>(total.getList());
            searchSourceBuilder.from(firstPageSize).size((int) total.getTotal() - firstPageSize);
            list.addAll(search(request, hit -> deserializer().apply(hit.getSourceAsString())).getList());
            total.setList(list);
        }
        return total;
    }

    @Override
    protected @NotNull Function<String, CardIncrWorkload> deserializer() {
        return s -> {
            try {
                return JSON_MAPPER.readValue(s, CardIncrWorkload.class);
            } catch (JsonProcessingException e) {
                log.error("Deserialize CardIncrWorkload json exception!", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };
    }
}
