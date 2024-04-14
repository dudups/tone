package com.ezone.ezproject.modules.card.field.update;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.mapper.CardMapper;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 关系型字段的更新：计划，父卡片id，故事地图节点
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class RelationalFieldUpdateHelper {
    private Long projectId;
    private CardMapper cardMapper;
    private Function<Long, Plan> findPlanById;
    private Function<Long, Card> findCardById;
    private Function<Card, List<Card>> findCardDescendant;

    // for batch update
    /**
     * 查询祖先节点
     */
    protected Function<List<Card>, Map<Long, List<Card>>> findCardsAncestor;
    /**
     * 查询子孙节点
     */
    protected Function<List<Card>, Map<Long, List<Card>>> findCardsDescendant;

    public void update(Card card, String field, Object value) {
        switch (field) {
            case CardField.PLAN_ID:
                Long planId = FieldUtil.toLong(value);
                card.setPlanId(planId);
                cardMapper.updateByPrimaryKey(card);
                break;
            case CardField.PARENT_ID:
                Long parentId = FieldUtil.toLong(value);
                Long ancestorId = 0L;
                if (parentId > 0L) {
                    if (parentId.equals(card.getId())) {
                        throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "不能设置自己为父卡片！");
                    }
                    Card parent = cardMapper.selectByPrimaryKey(parentId);
                    if (null == parent || BooleanUtils.isTrue(parent.getDeleted())) {
                        throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "绑定的父卡片不存在或已删除！");
                    }
                    ancestorId = parent.getAncestorId() > 0 ? parent.getAncestorId() : parentId;
                }
                if (!card.getAncestorId().equals(ancestorId)) {
                    Long rootId = parentId > 0 ? ancestorId : card.getId();
                    List<Card> descendant = findCardDescendant.apply(card);
                    if (CollectionUtils.isNotEmpty(descendant)) {
                        if (parentId.equals(card.getId())) {
                            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("[%s]存在父子层级循环！", card.getSeqNum()));
                        }
                        for (Card c : descendant) {
                            if (c.getId().equals(parentId)) {
                                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "父子层级循环！");
                            }
                            c.setAncestorId(rootId);
                            cardMapper.updateByPrimaryKey(c);
                        }
                    }
                }
                card.setParentId(parentId);
                card.setAncestorId(ancestorId);
                cardMapper.updateByPrimaryKey(card);
                break;
            case CardField.STORY_MAP_NODE_ID:
                card.setStoryMapNodeId(FieldUtil.toLong(value));
                cardMapper.updateByPrimaryKey(card);
                break;
            default:
        }
    }


    public void batchUpdateParentId(List<Card> cards, Long parentId) {
        if (CollectionUtils.isEmpty(cards)) {
            return;
        }
        Long ancestorId = 0L;
        if (parentId > 0L) {
            final Card parentCard = getAndCheckParentCard(cards, parentId);
            ancestorId = parentCard.getAncestorId() > 0 ? parentCard.getAncestorId() : parentId;
        }
        List<Long> ids = cards.stream().map(Card::getId).collect(Collectors.toList());
        Map<Long, List<Card>> descendantMap = findCardsDescendant.apply(cards);
        final Long ancestorIdFinal = ancestorId;
        descendantMap.entrySet().stream()
                .filter(e -> ids.contains(e.getKey()) && CollectionUtils.isNotEmpty(e.getValue()))
                .forEach(e -> e.getValue().forEach(card -> {
                    Long rootId = parentId > 0 ? ancestorIdFinal : e.getKey();
                    card.setAncestorId(rootId);
                    cardMapper.updateByPrimaryKey(card);
                }));

        for (Card card : cards) {
            card.setParentId(parentId);
            card.setAncestorId(ancestorId);
            cardMapper.updateByPrimaryKey(card);
        }
    }

    private Card getAndCheckParentCard(List<Card> cards, Long parentId) {
        final Card parentCard = parentId > 0L ? cardMapper.selectByPrimaryKey(parentId) : null;
        for (Card card : cards) {
            if (parentId.equals(card.getId())) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("%s不能设置自己为父卡片！", card.getSeqNum() == null ? "" : card.getSeqNum()));
            }
        }
        if (null == parentCard || BooleanUtils.isTrue(parentCard.getDeleted())) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "绑定的父卡片不存在或已删除！");
        }

        Map<Long, List<Card>> targetCardAncestor = findCardsAncestor.apply(Collections.singletonList(parentCard));
        List<Card> ancestor = targetCardAncestor.get(parentId);
        if (ancestor != null && !ancestor.isEmpty()) {
            List<Long> cycleRefs = cards.stream().filter(ancestor::contains).map(Card::getSeqNum).collect(Collectors.toList());
            if (!cycleRefs.isEmpty()) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("卡片%s存在循环引用！", StringUtils.join(cycleRefs, "、")));
            }
        }
        return parentCard;
    }
}
