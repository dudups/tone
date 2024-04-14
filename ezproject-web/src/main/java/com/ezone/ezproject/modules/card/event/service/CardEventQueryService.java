package com.ezone.ezproject.modules.card.event.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.dao.CardEventDao;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class CardEventQueryService {
    private CardEventDao cardEventDao;

    /**
     * date desc
     * @param cardId
     * @return
     */
    public List<CardEvent> selectByCardId(Long cardId) {
        try {
            return cardEventDao.searchWithDateDesc(cardId);
        } catch (IOException e) {
            log.error("Card-event selectByCardId from es exception!", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public CardEvent select(Long id) throws IOException {
        return cardEventDao.find(id);
    }

    public List<CardEvent> searchForChart(Date start, Date end, List<Query> cardPropQueries, List<String> cardProps)
            throws IOException {
        return cardEventDao.searchForChart(start, end, cardPropQueries, cardProps);
    }

    public List<CardEvent> searchForChart(Date start, Date end, List<Query> cardPropQueries, String... cardProps) {
        try {
            return cardEventDao.searchForChart(start, end, cardPropQueries, Arrays.asList(cardProps));
        } catch (Exception e) {
            log.error("searchForChart exception!", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public List<CardEvent> searchForChart(List<Long> cardIds, List<String> cardProps) throws IOException {
        return cardEventDao.searchForChart(cardIds, cardProps);
    }

    public List<CardEvent> searchForChart(List<Long> cardIds, String... cardProps){
        try {
            return cardEventDao.searchForChart(cardIds, Arrays.asList(cardProps));
        } catch (IOException e) {
            log.error("searchForChart exception!", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
