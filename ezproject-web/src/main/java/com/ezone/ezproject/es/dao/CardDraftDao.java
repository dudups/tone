package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.CardDraft;
import com.ezone.ezproject.es.util.EsIndexUtil;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

@Component
@Slf4j
@AllArgsConstructor
public class CardDraftDao extends AbstractEsDocDao<Long, CardDraft> {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    protected String index() {
        return EsIndexUtil.indexForCardDraft();
    }

    @Override
    public void saveOrUpdate(Long id, CardDraft cardDraft) throws IOException {
        setDocSourceJson(id, JSON_MAPPER.writeValueAsString(cardDraft));
    }

    public List<Long> searchIds(Query query) throws IOException {
        return searchAllIds(index(), query.queryBuilder());
    }

    @Override
    protected @NotNull Function<String, CardDraft> deserializer() {
        return s -> {
            try {
                return JSON_MAPPER.readValue(s, CardDraft.class);
            } catch (JsonProcessingException e) {
                log.error("Deserialize CardDraft json exception!", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };
    }
}
