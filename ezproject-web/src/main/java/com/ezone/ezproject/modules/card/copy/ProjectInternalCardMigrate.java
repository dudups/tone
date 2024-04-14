package com.ezone.ezproject.modules.card.copy;

import com.ezone.ezproject.es.entity.CardField;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Deprecated
@AllArgsConstructor
@SuperBuilder
@Slf4j
public class ProjectInternalCardMigrate extends AbstractCardMigrate {
    @Override
    public void run() throws IOException {
        if (CollectionUtils.isEmpty(cards)) {
            return;
        }
        List<Long> ids = new ArrayList<>();
        cards.forEach(card -> {
            card.setPlanId(targetPlan.getId());
            cardMapper.updateByPrimaryKey(card);
            ids.add(card.getId());
        });
        Map<String, Object> cardProps = new HashMap<>();
        cardProps.put(CardField.PLAN_ID, targetPlan.getId());
        cardProps.put(CardField.PLAN_IS_ACTIVE, targetPlan.getIsActive());
        cardProps.put(CardField.LAST_MODIFY_USER, user);
        cardProps.put(CardField.LAST_MODIFY_TIME, System.currentTimeMillis());
        cardDao.updateSelective(ids, cardProps);
    }
}
