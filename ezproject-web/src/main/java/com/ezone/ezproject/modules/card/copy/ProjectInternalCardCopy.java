package com.ezone.ezproject.modules.card.copy;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.modules.card.field.limit.SysFieldOpLimit;
import com.ezone.ezproject.modules.card.service.CardHelper;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@SuperBuilder
@Slf4j
public class ProjectInternalCardCopy extends AbstractCardCopy {

    @Override
    protected Card newCard(Card card, Long seqNum, String rank) {
        Long parentId = card.getParentId();
        Long ancestorId = card.getAncestorId();
        if (isDeleteSourceCards) {
            Card newParent = reParentMap.get(parentId);
            if (newParent == null) {
                if (reParentMap.containsKey(parentId)) {
                    parentId = 0L;
                    ancestorId = 0L;
                }
            } else {
                parentId = newParent.getId();
                ancestorId = newParent.getAncestorId() > 0 ? newParent.getAncestorId() : parentId;
            }
        }
        return Card.builder()
                .id(IdUtil.generateId())
                .projectId(targetProject.getId())
                .projectKey(targetProject.getKey())
                .seqNum(seqNum)
                .planId(targetPlanId)
                .parentId(parentId)
                .ancestorId(ancestorId)
                .rank(rank)
                .companyId(card.getCompanyId())
                .deleted(false)
                .maxCommentSeqNum(0L)
                .storyMapNodeId(0L)
                .latestEventId(0L)
                .build();
    }

    @Override
    protected Map<String, Object> newCardDetail(Map<String, Object> cardDetail, Card card) {
        Map<String, Object> newCardDetail = new HashMap<>();
        for (Map.Entry<String, Object> entry : cardDetail.entrySet()) {
            if (SysFieldOpLimit.canOp(entry.getKey(), SysFieldOpLimit.Op.CREATE)) {
                newCardDetail.put(entry.getKey(), entry.getValue());
            }
        }
        CardHelper.setCardCreatedProps(newCardDetail, opContext, card);
        return newCardDetail;
    }

}
