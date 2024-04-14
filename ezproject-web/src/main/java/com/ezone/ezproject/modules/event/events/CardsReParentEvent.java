package com.ezone.ezproject.modules.event.events;

import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.event.IEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CardsReParentEvent implements IEvent {
    private Project project;
    private String user;
    private List<CardEvent> cardEvents;
}
