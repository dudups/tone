package com.ezone.ezproject.modules.card.copy;

import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.es.entity.CardField;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@AllArgsConstructor
@SuperBuilder
@Slf4j
public class PlanInternalCardCopy extends ProjectInternalCardCopy {
    @Override
    protected Map<String, Object> newCardDetail(Map<String, Object> cardDetail, Card card) {
        Map<String, Object> newCardDetail = super.newCardDetail(cardDetail, card);
        newCardDetail.put(CardField.TITLE, String.format("%s-copy", cardDetail.get(CardField.TITLE)));
        return newCardDetail;
    }
}
