package com.ezone.ezproject.modules.card.copy;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.CardAttachmentRel;
import com.ezone.ezproject.dal.entity.CardAttachmentRelExample;
import com.ezone.ezproject.dal.entity.CardRelateRel;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.mapper.CardAttachmentRelMapper;
import com.ezone.ezproject.dal.mapper.CardMapper;
import com.ezone.ezproject.dal.mapper.CardRelateRelMapper;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.modules.card.bpm.service.CardBpmCmdService;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.service.CardHelper;
import com.ezone.ezproject.modules.common.OperationContext;
import com.ezone.ezproject.modules.event.EventDispatcher;
import com.ezone.ezproject.modules.event.events.CardsCreateEvent;
import com.ezone.ezproject.modules.event.events.CardsDeleteEvent;
import com.ezone.ezproject.modules.log.service.OperationLogCmdService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Slf4j
public abstract class AbstractCardCopy {
    protected OperationContext opContext;
    protected Consumer<Map<String, Object>> setCalcFields;
    protected List<Card> cards;
    protected Set<Long> deleteIds;
    protected List<String> ranks;
    protected Project targetProject;
    protected ProjectCardSchema targetSchema;
    protected Long targetPlanId;
    protected boolean planIsActive;
    protected boolean isDeleteSourceCards;
    protected Map<Long, Card> reParentMap;

    protected CardMapper cardMapper;
    protected CardAttachmentRelMapper cardAttachmentRelMapper;
    protected CardRelateRelMapper cardRelateRelMapper;
    protected CardDao cardDao;

    protected CardHelper cardHelper;
    protected EventDispatcher eventDispatcher;
    protected OperationLogCmdService operationLogCmdService;
    protected CardBpmCmdService cardBpmCmdService;

    public Project fromProject() {
        return targetProject;
    }

    public int run() throws IOException {
        if (CollectionUtils.isEmpty(cards)) {
            return 0;
        }
        Long seqNum = cardHelper.seqNums(targetProject.getId(), cards.size());
        Map<Long, List<CardAttachmentRel>> cardAttachmentRels = cardAttachmentRels();
        Map<Long, Map<String, Object>> cardDetails = cardDetails();
        Map<Long, Map<String, Object>> newCardDetails = new HashMap<>();
        List<Card> ascSortedCards = cards.stream().sorted(Comparator.comparing(Card::getRank)).collect(Collectors.toList());
        Map<String, Object> inactiveCardProps = inactiveRelateFieldProps(opContext.getUserName());
        List<Long> cancelFlowIds = new ArrayList<>();
        for (int i = 0; i < ascSortedCards.size(); i++) {
            Card card = ascSortedCards.get(i);
            Map<String, Object> cardDetail = cardDetails.get(card.getId());

            String type = FieldUtil.getType(cardDetail);
            CardType cardType = targetSchema.findCardType(type);
            if (null == cardType) {
                throw CodedException.ERROR_CARD_TYPE;
            }

            Card newCard = newCard(card, seqNum, CollectionUtils.isEmpty(ranks) ? card.getRank() : ranks.get(i));
            cardMapper.insert(newCard);
            List<CardAttachmentRel> rels = newCardAttachmentRels(cardAttachmentRels.get(card.getId()), newCard.getId());
            rels.forEach(rel -> cardAttachmentRelMapper.insert(rel));
            Map<String, Object> newCardDetail = newCardDetail(cardDetail, newCard);
            setCalcFields.accept(newCardDetail);
            newCardDetails.put(newCard.getId(), newCardDetail);

            if (isDeleteSourceCards && deleteIds.contains(card.getId())) {
                inactiveRelateField(card);
                cardMapper.updateByPrimaryKey(card);
                cardDetail.putAll(inactiveCardProps);
                operationLogCmdService.deleteCard(opContext, card, cardDetail);
                Long flowId = FieldUtil.getBpmFlowId(cardDetail);
                if (flowId != null && flowId > 0) {
                    cancelFlowIds.add(flowId);
                }
            } else {
                updateRelateRel(card, newCard, cardDetail, newCardDetail);
            }
            seqNum++;
        }
        cardDao.saveOrUpdate(newCardDetails);
        // related_card_ids || deleted
        cardDao.saveOrUpdate(cardDetails);
        eventDispatcher.dispatch(CardsCreateEvent.builder().user(opContext.getUserName()).cardDetails(newCardDetails).build());
        if (isDeleteSourceCards && CollectionUtils.isNotEmpty(deleteIds)) {
            eventDispatcher.dispatch(CardsDeleteEvent.builder()
                    .user(opContext.getUserName())
                    .project(fromProject())
                    .cardDetails(cardDetails.entrySet().stream()
                            .filter(e -> deleteIds.contains(e.getKey()))
                            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())))
                    .build());
        }
        cardBpmCmdService.asyncCancelFlows(cancelFlowIds, opContext.getUserName());
        return newCardDetails.size();
    }

    private void inactiveRelateField(Card card) {
        card.setDeleted(true);
        card.setParentId(0L);
        card.setAncestorId(0L);
        card.setPlanId(0L);
        card.setStoryMapNodeId(0L);
    }

    private Map<String, Object> inactiveRelateFieldProps(String user) {
        Map<String, Object> cardProps = CardHelper.generatePropsForUpdate(user, CardField.DELETED, true);
        cardProps.put(CardField.RELATED_CARD_IDS, null);
        cardProps.put(CardField.PARENT_ID, 0L);
        cardProps.put(CardField.PLAN_ID, 0L);
        cardProps.put(CardField.PLAN_IS_ACTIVE, true);
        cardProps.put(CardField.STORY_MAP_NODE_ID, 0L);
        cardProps.put(CardField.BPM_FLOW_ID, null);
        cardProps.put(CardField.BPM_FLOW_TO_STATUS, null);
        return cardProps;
    }

    private void updateRelateRel(Card card, Card newCard, Map<String, Object> cardDetail, Map<String, Object> newCardDetail) {
        cardRelateRelMapper.insert(CardRelateRel.builder()
                .id(IdUtil.generateId())
                .cardId(card.getId())
                .relatedCardId(newCard.getId())
                .build());
        cardRelateRelMapper.insert(CardRelateRel.builder()
                .id(IdUtil.generateId())
                .cardId(newCard.getId())
                .relatedCardId(card.getId())
                .build());
        newCardDetail.put(CardField.RELATED_CARD_IDS, Arrays.asList(card.getId()));
        CardHelper.addRelateCardId(cardDetail, newCard.getId());
    }

    protected abstract Card newCard(Card card, Long seqNum, String rank);

    protected abstract Map<String, Object> newCardDetail(Map<String, Object> cardDetail, Card card);

    protected Map<Long, List<CardAttachmentRel>> cardAttachmentRels() {
        CardAttachmentRelExample relExample = new CardAttachmentRelExample();
        relExample.createCriteria().andCardIdIn(cards.stream().map(Card::getId).collect(Collectors.toList()));
        List<CardAttachmentRel> rels =cardAttachmentRelMapper.selectByExample(relExample);
        return rels.stream().collect(Collectors.groupingBy(CardAttachmentRel::getCardId));
    }

    protected List<CardAttachmentRel> newCardAttachmentRels(List<CardAttachmentRel> rels, Long newCardId) {
        if (null == rels) {
            return ListUtils.EMPTY_LIST;
        }
        return rels.stream()
                .map(rel -> CardAttachmentRel.builder()
                        .id(IdUtil.generateId())
                        .cardId(newCardId)
                        .attachmentId(rel.getAttachmentId())
                        .build())
                .collect(Collectors.toList());
    }

    protected Map<Long, Map<String, Object>> cardDetails() throws IOException {
        return cardDao.findAsMap(cards.stream().map(Card::getId).collect(Collectors.toList()));
    }

}
