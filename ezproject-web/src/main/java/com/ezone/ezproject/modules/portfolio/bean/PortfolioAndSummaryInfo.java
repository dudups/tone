package com.ezone.ezproject.modules.portfolio.bean;

import com.ezone.ezproject.dal.entity.Portfolio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PortfolioAndSummaryInfo {
    private Portfolio portfolio;
    private boolean favorite;
    private long projectCount;
    private long planCount;
}
