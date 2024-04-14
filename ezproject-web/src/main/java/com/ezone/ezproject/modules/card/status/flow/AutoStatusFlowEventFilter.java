package com.ezone.ezproject.modules.card.status.flow;

import com.ezone.ezproject.es.entity.CardType;

public interface AutoStatusFlowEventFilter {
    default boolean match(CardType.AutoStatusFlowConf flow) {
        return true;
    }
}
