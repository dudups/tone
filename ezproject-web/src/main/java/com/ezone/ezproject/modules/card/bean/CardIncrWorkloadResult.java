package com.ezone.ezproject.modules.card.bean;

import com.ezone.ezproject.modules.card.event.model.CardIncrWorkload;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CardIncrWorkloadResult {
    private CardIncrWorkload workload;
    private Float successActualWorkload;
}
