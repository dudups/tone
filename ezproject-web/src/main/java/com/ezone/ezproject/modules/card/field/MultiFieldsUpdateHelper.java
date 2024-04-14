package com.ezone.ezproject.modules.card.field;

import com.ezone.ezproject.modules.event.events.CardUpdateEvent;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuperBuilder
@NoArgsConstructor
@Slf4j
public class MultiFieldsUpdateHelper extends CompletelyFieldsUpdateHelper {
    @Override
    public CardUpdateEvent update() throws IOException {
        Map<String, Object> fromCardDetail = cardDao.findAsMap(card.getId());
        Map<String, Object> toCardDetail = new HashMap<>();
        toCardDetail.putAll(fromCardDetail);
        toCardDetail.putAll(this.cardDetail);
        return update(() -> fromCardDetail, toCardDetail);
    }
}
