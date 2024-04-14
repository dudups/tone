package com.ezone.ezproject.modules.card.event.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum EventType {
    CREATE, UPDATE, DELETE, RECOVERY,
    ADD_ATTACHMENT, RM_ATTACHMENT,
    AUTO_STATUS_FLOW,
    CALC_IS_END, PLAN_IS_ACTIVE, PROJECT_IS_ACTIVE,
    ADD_RELATE_CARD, RM_RELATE_CARD;

    /**
     * ！！！新版ES-client运行时异常不支持枚举值，需改用EVENT_STR_FOR_STAT_CHART
     */
    public static final List<EventType> EVENT_FOR_STAT_CHART = Arrays.asList(
            CREATE, UPDATE, DELETE, RECOVERY,
            AUTO_STATUS_FLOW,
            CALC_IS_END, PLAN_IS_ACTIVE, PROJECT_IS_ACTIVE);

    public static final List<String> EVENT_STR_FOR_STAT_CHART = EVENT_FOR_STAT_CHART.stream().map(Enum::name).collect(Collectors.toList());
}
