package com.ezone.ezproject.modules.card.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.CardRelateRel;
import com.ezone.ezproject.dal.entity.CardRelateRelExample;
import com.ezone.ezproject.dal.mapper.CardMapper;
import com.ezone.ezproject.dal.mapper.CardRelateRelMapper;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.event.service.CardEventCmdService;
import com.ezone.ezproject.modules.event.EventDispatcher;
import com.ezone.ezproject.modules.event.events.RelateCardAddEvent;
import com.ezone.ezproject.modules.event.events.RelateCardRmEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Transactional
@Service
@Slf4j
@AllArgsConstructor
public class CardRelateRelCmdService {
    private CardMapper cardMapper;
    private CardDao cardDao;
    private CardRelateRelMapper cardRelateRelMapper;
    private CardEventCmdService cardEventCmdService;
    private UserService userService;
    private EventDispatcher eventDispatcher;

    public void create(Card card, Card relateCard) throws IOException {
        if (card.getId().equals(relateCard.getId())) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "Relate same card!");
        }
        CardRelateRelExample example = new CardRelateRelExample();
        example.createCriteria().andCardIdEqualTo(card.getId()).andRelatedCardIdEqualTo(relateCard.getId());
        List<CardRelateRel> rels = cardRelateRelMapper.selectByExample(example);
        if (CollectionUtils.isNotEmpty(rels)) {
            return;
        }
        String user = userService.currentUserName();
        addCardRelateRel(user, card.getId(), relateCard.getId());
        addCardRelateRel(relateCard.getId(), card.getId());
        eventDispatcher.asyncDispatch(() -> RelateCardAddEvent.builder()
                .user(user)
                .card(card)
                .cardDetail(cardDao.findAsMap(card.getId()))
                .relateCard(relateCard)
                .build());
    }

    public void initCardRelateRel(String user, Long cardId, List<Long> relateCardIds)
            throws IOException {
        if (CollectionUtils.isEmpty(relateCardIds)) {
            return;
        }
        relateCardIds.forEach(relateCardId -> {
            cardRelateRelMapper.insert(CardRelateRel.builder()
                    .id(IdUtil.generateId())
                    .cardId(cardId)
                    .relatedCardId(relateCardId)
                    .build());
            cardRelateRelMapper.insert(CardRelateRel.builder()
                    .id(IdUtil.generateId())
                    .cardId(relateCardId)
                    .relatedCardId(cardId)
                    .build());
        });

        cardDao.updateSelective(cardId,
                CardHelper.generatePropsForUpdate(user, CardField.RELATED_CARD_IDS, relateCardIds));
        Map<Long, Map<String, Object>> cards = cardDao.findAsMap(relateCardIds, CardField.RELATED_CARD_IDS);
        Map<Long, Map<String, Object>> cardsProps = new HashMap<>();
        cards.entrySet().forEach(e -> cardsProps.put(e.getKey(), CardHelper.generatePropsForUpdate(
                CardField.RELATED_CARD_IDS, CardHelper.addRelateCardId(e.getValue(), cardId))));
        cardDao.updateSelective(cardsProps);
    }

    public void delete(Card card, Long relateCardId) throws IOException {
        CardRelateRelExample example = new CardRelateRelExample();
        example.createCriteria().andCardIdEqualTo(card.getId()).andRelatedCardIdEqualTo(relateCardId);
        example.or().andCardIdEqualTo(relateCardId).andRelatedCardIdEqualTo(card.getId());
        cardRelateRelMapper.deleteByExample(example);

        Map<String, Object> cardProps = cardDao.findAsMap(card.getId(), CardField.RELATED_CARD_IDS);
        List<Long> relateCardIds = CardHelper.rmRelateCardId(cardProps, relateCardId);
        String user = userService.currentUserName();
        cardDao.updateSelective(card.getId(),
                CardHelper.generatePropsForUpdate(user, CardField.RELATED_CARD_IDS, relateCardIds));

        Card relateCard = cardMapper.selectByPrimaryKey(relateCardId);
        if (null == relateCard) {
            return;
        }

        cardProps = cardDao.findAsMap(relateCardId, CardField.RELATED_CARD_IDS);
        relateCardIds = CardHelper.rmRelateCardId(cardProps, card.getId());
        cardProps.put(CardField.RELATED_CARD_IDS, relateCardIds);
        cardDao.updateSelective(relateCardId, cardProps);
        eventDispatcher.asyncDispatch(() -> RelateCardRmEvent.builder()
                .user(user)
                .card(card)
                .cardDetail(cardDao.findAsMap(card.getId()))
                .relateCard(relateCard)
                .build());
    }

    public void deleteAll(Long cardId) {
        CardRelateRelExample example = new CardRelateRelExample();
        example.or().andCardIdEqualTo(cardId);
        example.or().andRelatedCardIdEqualTo(cardId);
        cardRelateRelMapper.deleteByExample(example);
    }

    public void deleteAll(List<Long> cardIds) {
        CardRelateRelExample example = new CardRelateRelExample();
        example.or().andCardIdIn(cardIds);
        example.or().andRelatedCardIdIn(cardIds);
        cardRelateRelMapper.deleteByExample(example);
    }

    private void addCardRelateRel(String user, Long cardId, Long relateCardId)
            throws IOException {
        cardRelateRelMapper.insert(CardRelateRel.builder()
                .id(IdUtil.generateId())
                .cardId(cardId)
                .relatedCardId(relateCardId)
                .build());
        Map<String, Object> cardProps = cardDao.findAsMap(cardId, CardField.RELATED_CARD_IDS);
        List<Long> relateCardIds = CardHelper.addRelateCardId(cardProps, relateCardId);
        cardDao.updateSelective(cardId,
                CardHelper.generatePropsForUpdate(user, CardField.RELATED_CARD_IDS, relateCardIds));
    }

    private void addCardRelateRel(Long cardId, Long relateCardId)
            throws IOException {
        cardRelateRelMapper.insert(CardRelateRel.builder()
                .id(IdUtil.generateId())
                .cardId(cardId)
                .relatedCardId(relateCardId)
                .build());
        Map<String, Object> cardProps = cardDao.findAsMap(cardId, CardField.RELATED_CARD_IDS);
        List<Long> relateCardIds = CardHelper.addRelateCardId(cardProps, relateCardId);
        cardDao.updateSelective(cardId,
                CardHelper.generatePropsForUpdate(CardField.RELATED_CARD_IDS, relateCardIds));
    }
}
