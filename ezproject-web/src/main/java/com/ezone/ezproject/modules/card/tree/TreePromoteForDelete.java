package com.ezone.ezproject.modules.card.tree;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.mapper.CardMapper;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.card.event.model.EventType;
import com.ezone.ezproject.modules.card.event.model.UpdateEventMsg;
import com.ezone.ezproject.modules.card.service.CardHelper;
import com.ezone.ezproject.modules.card.service.CardQueryService;
import com.ezone.ezproject.modules.event.EventDispatcher;
import com.ezone.ezproject.modules.event.events.CardsReParentEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class TreePromoteForDelete {
    private String user;
    private List<Card> cards;

    private CardField cardFieldParentId;

    private CardQueryService cardQueryService;

    private CardMapper cardMapper;
    private CardDao cardDao;

    private Project project;
    private EventDispatcher eventDispatcher;

    /**
     * @return Map<Long: FromParent, Card{id, ancestorId}: ToParent>
     * @throws IOException
     */
    public Map<Long, Card> promote() throws IOException {
        Map<Long, Set<Long>> deletes = cards.stream().collect(Collectors.groupingBy(
                card -> card.getAncestorId() > 0 ? card.getAncestorId() : card.getId(),
                Collectors.mapping(Card::getId, Collectors.toSet())));
        Map<Long, Card> ancestors = cardQueryService.select(new ArrayList<>(deletes.keySet())).stream().collect(Collectors.toMap(Card::getId, c -> c));
        Map<Long, List<Card>> descendats = cardQueryService.selectDescendants(new ArrayList<>(deletes.keySet()));
        List<Promote> promotes = new ArrayList<>();
        Map<Long, Card> reParents = new HashMap<>();
        descendats.entrySet().stream().filter(e -> CollectionUtils.isNotEmpty(e.getValue())).forEach(e -> {
            Long rootId = e.getKey();
            Card root = ancestors.get(rootId);
            if (root == null) {
                return;
            }
            Set<Long> deleteIds = deletes.get(rootId);
            if (CollectionUtils.isEmpty(deleteIds)) {
                return;
            }
            List<Card> descendat = e.getValue();
            Map<Long, List<Card>> childrens = descendat.stream().collect(Collectors.groupingBy(Card::getParentId));
            promote(ancestors.get(rootId), null, null, deleteIds, childrens, promotes, reParents);
        });
        if (CollectionUtils.isEmpty(promotes)) {
            return reParents;
        }
        Map<Long, Map<String, Object>> cardsProps = promotes.stream().collect(Collectors.toMap(Promote::getCardId, promote -> {
            Card toParent = promote.getToParent();
            Long toParentId = toParent == null ? 0L : toParent.getId();
            return CardHelper.generatePropsForUpdate(user, CardField.PARENT_ID, toParentId);
        }));
        cardDao.updateSelective(cardsProps);
        saveEvents(promotes);
        return reParents;
    }

    private void promote(Card card, Card fromParent, Card toParent, Set<Long> deleteIds, Map<Long, List<Card>> childrens,
                         List<Promote> promotes, Map<Long, Card> reParents) {
        boolean delete = deleteIds.contains(card.getId());
        if (delete) {
            reParents.put(card.getId(), toParent);
        } else {
            boolean update = false;
            Long fromParentId = fromParent == null ? 0L : fromParent.getId();
            Long toParentId = toParent == null ? 0L : toParent.getId();
            if (!fromParentId.equals(toParentId)) {
                card.setParentId(toParentId);
                update = true;
                promotes.add(Promote.builder()
                        .cardId(card.getId())
                        .fromParent(fromParent)
                        .toParent(toParent)
                        .build());
            }
            Long fromAncestorId = card.getAncestorId();
            Long toAncestorId = 0L;
            if (toParent != null) {
                toAncestorId = toParent.getParentId() > 0 ? toParent.getAncestorId() : toParentId;
            }
            if (!fromAncestorId.equals(toAncestorId)) {
                card.setAncestorId(toAncestorId);
                update = true;
                reParents.put(card.getId(), card);
            }
            if (update) {
                cardMapper.updateByPrimaryKey(card);
            }
        }
        List<Card> children = childrens.get(card.getId());
        if (CollectionUtils.isNotEmpty(children)) {
            children.forEach(child -> promote(
                    child,
                    card,
                    delete ? toParent : card,
                    deleteIds,
                    childrens,
                    promotes,
                    reParents));
        }
    }

    private void saveEvents(List<Promote> promotes) throws IOException {
        Map<Long, Map<String, Object>> cardDetails = cardDao.findAsMap(promotes.stream().map(Promote::getCardId).collect(Collectors.toList()));
        List<CardEvent> cardEvents = promotes.stream().map(promote -> {
            Long cardId = promote.getCardId();
            Card fromParent = promote.getFromParent();
            Card toParent = promote.getToParent();
            String fromParentName = null == fromParent ? null : String.format("%s-%s", fromParent.getProjectKey(), fromParent.getSeqNum());
            String toParentName = null == toParent ? null : String.format("%s-%s", toParent.getProjectKey(), toParent.getSeqNum());
            return CardEvent.builder()
                    .id(IdUtil.generateId())
                    .cardId(cardId)
                    .date(new Date())
                    .user(user)
                    .eventType(EventType.UPDATE)
                    .eventMsg(UpdateEventMsg.builder()
                            .fieldDetailMsg(UpdateEventMsg.FieldDetailMsg.builder()
                                    .fieldKey(CardField.PARENT_ID)
                                    .fieldMsg(cardFieldParentId.getName())
                                    .fromMsg(fromParentName)
                                    .toMsg(toParentName)
                                    .build())
                            .build())
                    .cardDetail(cardDetails.get(cardId))
                    .build();
        }).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(cardEvents)) {
            eventDispatcher.dispatch(CardsReParentEvent.builder()
                    .project(project)
                    .user(user)
                    .cardEvents(cardEvents)
                    .build());
        }
    }

    private Map<Long, Card> promoteReParentMap(List<Promote> promotes) {
        return promotes.stream()
                .filter(p -> p != null && p.getFromParent() != null && p.getToParent() != null)
                .collect(Collectors.toMap(p -> p.getFromParent().getId(), p -> p.getToParent()));
    }

    @AllArgsConstructor
    @Builder
    @Data
    public static class Promote {
        private Long cardId;
        private Card fromParent;
        private Card toParent;
    }
}
