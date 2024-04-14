package com.ezone.ezproject.modules.card.event.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = EventMsg.TYPE)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "ADD_ATTACHMENT", value = AddAttachmentEventMsg.class),
        @JsonSubTypes.Type(name = "RM_ATTACHMENT", value = DelAttachmentEventMsg.class),
        @JsonSubTypes.Type(name = "ADD_RELATE_CARD", value = AddRelateCardEventMsg.class),
        @JsonSubTypes.Type(name = "RM_RELATE_CARD", value = DelRelateCardEventMsg.class),
        @JsonSubTypes.Type(name = "CREATE", value = CreateEventMsg.class),
        @JsonSubTypes.Type(name = "UPDATE", value = UpdateEventMsg.class),
        @JsonSubTypes.Type(name = "AUTO_STATUS_FLOW", value = AutoStatusFlowEventMsg.class)
})
public interface EventMsg {
    String TYPE = "type";
}
