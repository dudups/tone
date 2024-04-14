package com.ezone.ezproject.modules.event.events;

import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.event.IEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CardRecoveryEvent implements IEvent {
    private String user;
    private Card card;
    private Map<String, Object> cardDetail;

    @Getter(lazy = true)
    private final String title = FieldUtil.getTitle(cardDetail);
}
