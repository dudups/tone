package com.ezone.ezproject.es.entity.enums;

import com.ezone.ezproject.es.entity.CardField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.compress.utils.Sets;

import java.util.Set;

@AllArgsConstructor
@Getter
public enum OperationType {
    PROJECT_READ(ConfigType.ENABLE),
    PROJECT_MANAGE_READ(ConfigType.ENABLE), PROJECT_MANAGE_UPDATE(ConfigType.ENABLE),
    STORY_MAP_CREATE(ConfigType.ENABLE), STORY_MAP_DELETE(ConfigType.ENABLE),
    PRODUCT_VERSION_CREATE(ConfigType.ENABLE), PRODUCT_VERSION_UPDATE(ConfigType.ENABLE), PRODUCT_VERSION_DELETE(ConfigType.ENABLE),PRODUCT_VERSION_INACTIVE(ConfigType.ENABLE),PRODUCT_VERSION_RECOVERY(ConfigType.ENABLE),
    CHART_CREATE(ConfigType.ENABLE), CHART_UPDATE(ConfigType.ENABLE), CHART_DELETE(ConfigType.ENABLE),
    CARD_VIEW_CREATE(ConfigType.ENABLE), CARD_VIEW_UPDATE(ConfigType.ENABLE), CARD_VIEW_DELETE(ConfigType.ENABLE),
    PLAN_CREATE(ConfigType.ENABLE), PLAN_UPDATE(ConfigType.ENABLE), PLAN_DELETE(ConfigType.ENABLE), PLAN_INACTIVE(ConfigType.ENABLE) , PLAN_RECOVERY(ConfigType.ENABLE),
    CARD_CREATE(ConfigType.CARD_TYPES), CARD_UPDATE(ConfigType.ENABLE), CARD_SORT(ConfigType.ENABLE), CARD_DELETE(ConfigType.CARD_TYPES), CARD_RECOVERY(ConfigType.ENABLE), CARD_COMMENT(ConfigType.ENABLE);

    public static final String[] CARD_OP_LIMIT_FIELDS = new String[] {
            CardField.TYPE,
            CardField.PLAN_ID, CardField.STORY_MAP_NODE_ID,
            CardField.STATUS, CardField.OWNER_USERS,
            CardField.CREATE_USER
    };

    public static final Set<OperationType> PROJECT_ACTIVE_OPS = Sets.newHashSet(
            PROJECT_READ, PROJECT_MANAGE_READ, PROJECT_MANAGE_UPDATE
    );

    private ConfigType configType;

    public enum ConfigType {
        ENABLE, CARD_TYPES
    }
}