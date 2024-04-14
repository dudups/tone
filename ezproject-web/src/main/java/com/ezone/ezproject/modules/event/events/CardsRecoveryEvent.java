package com.ezone.ezproject.modules.event.events;

import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.modules.event.IEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CardsRecoveryEvent implements IEvent {
    private Project project;
    private String user;
    private Map<Long, Map<String, Object>> cardDetails;
}
