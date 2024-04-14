package com.ezone.ezproject.modules.card.status.flow;

import com.ezone.ezproject.es.entity.CardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoStatusFlowMatcher {
    private AutoStatusFlowEventFilter eventFilter;

    public CardType.AutoStatusFlowConf matchAutoStatusFlow(CardType cardType, String fromStatus) {
        if (CollectionUtils.isEmpty(cardType.getAutoStatusFlows())) {
            return null;
        }
        return cardType.getAutoStatusFlows().stream()
                .filter(eventFilter::match)
                .filter(f -> !f.isCheck() || cardType.findStatusFlow(fromStatus, f.getTargetStatus()) != null)
                .findAny().orElse(null);
    }
}
