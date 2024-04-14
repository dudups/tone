package com.ezone.ezproject.modules.portfolio.bean;

import com.ezone.ezproject.dal.entity.Portfolio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;


@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TotalPortfolioInfo {
    private List<PortfolioAndSummaryInfo> list;
    private Map<Long, Portfolio> ancestors;
}
