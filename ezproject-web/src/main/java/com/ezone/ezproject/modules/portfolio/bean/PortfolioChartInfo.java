package com.ezone.ezproject.modules.portfolio.bean;

import com.ezone.ezproject.dal.entity.PortfolioChart;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioChartInfo {

    private PortfolioChart chart;

    private Map<String, Object> config;
}
