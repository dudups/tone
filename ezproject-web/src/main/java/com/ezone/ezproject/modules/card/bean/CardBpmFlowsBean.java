package com.ezone.ezproject.modules.card.bean;

import com.ezone.ezproject.modules.card.bpm.bean.CardBpmFlow;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CardBpmFlowsBean {
    private List<CardBpmFlow> flows;
    private Object details;
}
