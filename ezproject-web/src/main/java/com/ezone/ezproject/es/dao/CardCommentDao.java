package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.EsUtil;
import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.CardComment;
import com.ezone.ezproject.es.util.EsIndexUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Component
@Slf4j
@AllArgsConstructor
public class CardCommentDao extends AbstractEsDocDao<Long, CardComment> {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    protected String index() {
        return EsIndexUtil.indexForCardComment();
    }

    @Override
    public void saveOrUpdate(Long id, CardComment cardComment) throws IOException {
        setDocSourceJson(id, JSON_MAPPER.writeValueAsString(cardComment));
    }

    public void saveOrUpdate(CardComment cardComment) throws IOException {
        setDocSourceJson(cardComment.getId(), JSON_MAPPER.writeValueAsString(cardComment));
    }

    public void updateField(Long id, String field, Object value) throws IOException {
        updateDoc(index(), id, Collections.singletonMap(field, value));
    }

    public List<CardComment> searchWithSeqNumDesc(Long cardId) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termQuery(CardComment.CARD_ID, cardId));
        searchSourceBuilder.sort(new FieldSortBuilder(CardComment.SEQ_NUM).order(SortOrder.DESC));
        return searchAll(searchSourceBuilder, 1000).getList();
    }

    private TotalBean<CardComment> searchAll(SearchSourceBuilder searchSourceBuilder, int firstPageSize) throws IOException {
        searchSourceBuilder.from(0).size(firstPageSize);
        SearchRequest request = new SearchRequest();
        request.indices(index());
        request.source(searchSourceBuilder);
        TotalBean<CardComment> total = search(request, hit -> deserializer().apply(hit.getSourceAsString()));
        if (total.getTotal() > firstPageSize) {
            List<CardComment> list = new ArrayList<>(total.getList());
            searchSourceBuilder.from(firstPageSize).size((int) total.getTotal() - firstPageSize);
            list.addAll(search(request, hit -> deserializer().apply(hit.getSourceAsString())).getList());
            total.setList(list);
        }
        return total;
    }

    @Override
    protected @NotNull Function<String, CardComment> deserializer() {
        return s -> {
            try {
                return JSON_MAPPER.readValue(s, CardComment.class);
            } catch (JsonProcessingException e) {
                log.error("Deserialize CardComment json exception!", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };
    }

    public void deleteByCardIds(List<Long> cardIds) throws IOException {
        DeleteByQueryRequest request = new DeleteByQueryRequest(index());
        request.setConflicts("proceed");
        request.setQuery(QueryBuilders.termsQuery(CardComment.CARD_ID, cardIds));
        BulkByScrollResponse response = es.deleteByQuery(request, EsUtil.REQUEST_OPTIONS);
        if (response.getVersionConflicts() > 0) {
            log.warn("There are [{}] version conflicts when delete by card-ids:[{s}]!", response.getVersionConflicts(), cardIds);
        }
    }
}
