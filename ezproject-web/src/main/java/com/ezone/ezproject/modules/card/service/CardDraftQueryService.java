package com.ezone.ezproject.modules.card.service;

import com.ezone.ezproject.es.dao.CardDraftDao;
import com.ezone.ezproject.es.entity.CardDraft;
import com.ezone.ezproject.modules.card.bean.query.Ids;
import com.ezone.ezproject.modules.card.bean.query.Query;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class CardDraftQueryService {
    private CardDraftDao cardDraftDao;

    public CardDraft select(Long id) throws IOException {
        return cardDraftDao.find(id);
    }

    public List<Long> searchIds(Query query) throws IOException {
        return cardDraftDao.searchIds(query);
    }

    public Set<Long> validDraftIds(Collection<Long> draftIds) throws IOException {
        return new HashSet<>(cardDraftDao
                .searchIds(Ids.builder()
                        .ids(draftIds.stream().map(String::valueOf).collect(Collectors.toList())).build())
        );
    }

}
