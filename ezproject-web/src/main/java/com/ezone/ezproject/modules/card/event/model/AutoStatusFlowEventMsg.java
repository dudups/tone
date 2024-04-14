package com.ezone.ezproject.modules.card.event.model;

import com.ezone.ezproject.external.ci.bean.AutoStatusFlowEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AutoStatusFlowEventMsg implements EventMsg {
    private AutoStatusFlowEventType eventType;
    private String fromMsg;
    private String toMsg;
}
