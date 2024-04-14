package com.ezone.ezproject.modules.card.status.flow;

import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.bean.AutoStatusFlowEventFilterConf;
import com.ezone.ezproject.es.entity.bean.CodeEventFilterConf;
import com.ezone.ezproject.external.ci.bean.AutoStatusFlowEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeEventFilter implements AutoStatusFlowEventFilter {
    private AutoStatusFlowEventType eventType;
    private String branch;

    @Override
    public boolean match(CardType.AutoStatusFlowConf flow) {
        if (eventType != flow.getEventType()) {
            return false;
        }
        AutoStatusFlowEventFilterConf eventFilterConf = flow.getEventFilterConf();
        if (eventFilterConf instanceof CodeEventFilterConf) {
            return eventFilterConf.match(branch);
        }
        return true;
    }
}
