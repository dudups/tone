package com.ezone.ezproject.modules.card.bean;

import com.ezone.ezproject.modules.card.event.model.CardIncrWorkload;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CardWorkloadBpmFlowsBean {
    private List<CardIncrWorkload> workloads;
    private Object details;
}
