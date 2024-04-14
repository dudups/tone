package com.ezone.ezproject.modules.card.bean;

import com.ezone.ezproject.modules.card.bpm.bean.CardBpmFlow;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CardBpmFlowBean {
    private CardBpmFlow flow;
    private Object detail;
}
