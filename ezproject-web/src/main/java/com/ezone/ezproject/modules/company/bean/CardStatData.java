package com.ezone.ezproject.modules.company.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class CardStatData {

    private long companyId;
    /**
     * 完成卡片
     */
    private long completedCards;
    /**
     * 完成工时
     */
    private double completedWorkload;

}
