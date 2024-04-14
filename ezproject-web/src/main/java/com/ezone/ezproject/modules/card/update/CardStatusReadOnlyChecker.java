package com.ezone.ezproject.modules.card.update;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.modules.card.field.bean.FieldChange;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CardStatusReadOnlyChecker {
    private CardStatusReadOnlyChecker() {
    }

    private static Set<String> ignoreFieldKeys = new HashSet<>();
    static {
        ignoreFieldKeys.add(CardField.PLAN_ID);
        ignoreFieldKeys.add(CardField.TITLE);
    }

    public static void check(CardType cardType, String statusKey, List<FieldChange> fieldChanges) {
        if (CollectionUtils.isEmpty(fieldChanges)) {
            return;
        }
        //状态变化时不限制只读
        if (fieldChanges.stream().anyMatch(fieldChange -> CardField.STATUS.equals(fieldChange.getField().getKey()))) {
            return;
        }

        CardType.StatusConf statusConf = cardType.findStatusConf(statusKey);
        if (statusConf == null) {
            throw new CodedException(HttpStatus.BAD_REQUEST, "非法状态");
        }

        // 只读状态的字段中只允许修改状态、所属计划、标题
        if (statusConf.isReadOnly() && fieldChanges.stream().anyMatch(fieldChange -> !ignoreFieldKeys.contains(fieldChange.getField().getKey()))) {
            throw new CodedException(HttpStatus.FORBIDDEN, "此卡片当前状态为只读状态，不允许编辑");
        }
    }

}
