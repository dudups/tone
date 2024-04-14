package com.ezone.ezproject.modules.chart.data;

import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.modules.card.bean.CardBean;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GanttChart {
    private List<Plan> plans;
    private List<CardBean> cardBeans;
}
