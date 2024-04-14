package com.ezone.ezproject.modules.card.service;

import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.mapper.CardMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author cf
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(CardMapper.class)
class CardQueryServiceTest {
    private static final Long PROJECT_ID = 665821422703099904L;

    CardQueryService cardQueryService;
    HashMap<Long, Card> allCards = new HashMap<>();
    List<Card> testCards = new ArrayList<>();
    List<Card> queriedCards = new ArrayList<>();

    @BeforeEach
    void initService() throws Exception {

        for (long id = 0; id < 100; id++) {
            Card card = Card.builder().id(id)
                    .projectId(PROJECT_ID)
                    .deleted(false)
                    .ancestorId(0L)
                    .parentId(0L).build();
            allCards.put(id, card);
        }



        CardMapper cardMapper = PowerMockito.mock(CardMapper.class);
        Mockito.when(cardMapper.selectByExample(Mockito.any())).thenReturn(queriedCards);
//        PowerMockito.whenNew(CardMapper.class).withNoArguments();

        cardQueryService = PowerMockito.mock(CardQueryService.class);
        PowerMockito.when(cardQueryService.selectDescendant(Mockito.anyList())).thenCallRealMethod();
        PowerMockito.when(cardQueryService.selectDescendantByIds(Mockito.any())).thenCallRealMethod();

        PowerMockito.field(CardQueryService.class, "cardMapper").set(cardQueryService, cardMapper);

    }

    @Test
    void selectDescendantByCards() {
        //设置原始上下级关系
        allCards.get(21L).setParentId(2L);
        allCards.get(22L).setParentId(2L);
        allCards.get(23L).setParentId(21L);
        allCards.get(24L).setParentId(23L);

        allCards.get(11L).setParentId(1L);
        allCards.get(12L).setParentId(1L);
        allCards.get(13L).setParentId(11L);
        allCards.get(14L).setParentId(13L);

        allCards.get(31L).setParentId(3L);
        allCards.get(32L).setParentId(3L);
        allCards.get(33L).setParentId(31L);

        testCards.add(copy(allCards.get(2L)));
        testCards.add(copy(allCards.get(3L)));

        queriedCards.add(copy(allCards.get(2L)));
        queriedCards.add(copy(allCards.get(3L)));

        queriedCards.add(copy(allCards.get(21L)));
        queriedCards.add(copy(allCards.get(22L)));
        queriedCards.add(copy(allCards.get(23L)));
        queriedCards.add(copy(allCards.get(24L)));

        queriedCards.add(copy(allCards.get(11L)));
        queriedCards.add(copy(allCards.get(12L)));
        queriedCards.add(copy(allCards.get(13L)));
        queriedCards.add(copy(allCards.get(14L)));

        queriedCards.add(copy(allCards.get(31L)));
        queriedCards.add(copy(allCards.get(32L)));
        queriedCards.add(copy(allCards.get(33L)));
        queriedCards.add(copy(allCards.get(34L)));

        Map<Long, List<Card>> resultMap = cardQueryService.selectDescendant(testCards);
        assertEquals(resultMap.get(2L).size(), 4);
        assertEquals(resultMap.get(3L).size(), 3);
    }

    @Test
    void selectDescendantByCards2() {
        //设置原始上下级关系
        allCards.get(11L).setParentId(1L);
        allCards.get(12L).setParentId(1L);
        allCards.get(13L).setParentId(11L);
        allCards.get(14L).setParentId(13L);
        allCards.get(11L).setAncestorId(1L);
        allCards.get(12L).setAncestorId(1L);
        allCards.get(13L).setAncestorId(1L);
        allCards.get(14L).setAncestorId(1L);

        allCards.get(21L).setParentId(2L);
        allCards.get(22L).setParentId(2L);
        allCards.get(23L).setParentId(21L);
        allCards.get(24L).setParentId(23L);
        allCards.get(21L).setAncestorId(2L);
        allCards.get(22L).setAncestorId(2L);
        allCards.get(23L).setAncestorId(2L);
        allCards.get(24L).setAncestorId(2L);
//        3 <- 31 <- 33;
//        3 <- 32
        allCards.get(31L).setParentId(3L);
        allCards.get(32L).setParentId(3L);
        allCards.get(33L).setParentId(31L);
        allCards.get(31L).setAncestorId(3L);
        allCards.get(32L).setAncestorId(3L);
        allCards.get(33L).setAncestorId(3L);

        testCards.add(copy(allCards.get(2L)));
        testCards.add(copy(allCards.get(32L)));
        testCards.add(copy(allCards.get(31L)));

        queriedCards.add(copy(allCards.get(2L)));
        queriedCards.add(copy(allCards.get(3L)));

        queriedCards.add(copy(allCards.get(21L)));
        queriedCards.add(copy(allCards.get(22L)));
        queriedCards.add(copy(allCards.get(23L)));
        queriedCards.add(copy(allCards.get(24L)));

        queriedCards.add(copy(allCards.get(31L)));
        queriedCards.add(copy(allCards.get(32L)));
        queriedCards.add(copy(allCards.get(33L)));
        queriedCards.add(copy(allCards.get(34L)));
//      testCards: 2, 31, 32
//        3 <- 31 <- 33;    3 <- 32
        Map<Long, List<Card>> descendantMap = cardQueryService.selectDescendant(testCards);
        assertEquals(4, descendantMap.get(2L).size());
        assertEquals(0, descendantMap.get(32L).size());
        assertEquals(1, descendantMap.get(31L).size());

        Map<Long, List<Card>> ancestorMap = cardQueryService.selectAncestor(testCards);
        assertEquals(3L, ancestorMap.get(32L).get(0).getId());
    }

    static Card copy(Card card){

        return Card.builder().id(card.getId())
                .projectId(PROJECT_ID)
                .deleted(card.getDeleted())
                .ancestorId(card.getAncestorId())
                .parentId(card.getParentId())
                .build();
    }
}