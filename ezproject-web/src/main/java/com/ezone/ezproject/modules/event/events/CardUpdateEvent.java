package com.ezone.ezproject.modules.event.events;

import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.field.bean.FieldChange;
import com.ezone.ezproject.modules.event.IEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CardUpdateEvent implements IEvent {
    private String user;
    private Card card;
    private CardEvent cardEvent;

    @Getter(lazy = true)
    private final String title = FieldUtil.getTitle(cardEvent.getCardDetail());
}
