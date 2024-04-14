package com.ezone.ezproject.modules.portfolio.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.PortfolioChart;
import com.ezone.ezproject.dal.entity.PortfolioChartExample;
import com.ezone.ezproject.dal.mapper.ExtPortfolioChartMapper;
import com.ezone.ezproject.es.dao.PortfolioChartConfigDao;
import com.ezone.ezproject.modules.portfolio.bean.PortfolioChartInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class PortfolioCharQueryService {

    private ExtPortfolioChartMapper chartMapper;
    private PortfolioChartConfigDao chartConfigDao;

    public List<PortfolioChart> selectChartByPortfolioId(Long portfolioId) {
        PortfolioChartExample example = new PortfolioChartExample();
        example.createCriteria().andPortfolioIdEqualTo(portfolioId);
        example.setOrderByClause("id");
        return chartMapper.selectByExample(example);
    }

    public int nextChartSeqNum(Long portfolioId) {
        PortfolioChartExample example = new PortfolioChartExample();
        example.createCriteria().andPortfolioIdEqualTo(portfolioId);
        return (int) chartMapper.countByExample(example);
    }

    public PortfolioChart selectChart(Long portfolioId, Long chartId) {
        PortfolioChart chart = chartMapper.selectByPrimaryKey(chartId);
        if (chart == null || !chart.getPortfolioId().equals(portfolioId)) {
            return null;
        }
        return chart;
    }

    public PortfolioChartInfo selectChartInfo(Long portfolioId, Long chartId) throws IOException {
        PortfolioChart chart = selectChart(portfolioId, chartId);
        if (chart == null) {
            throw CodedException.NOT_FOUND;
        }
        return PortfolioChartInfo.builder()
                .chart(chart)
                .config(chartConfigDao.find(chartId))
                .build();
    }
}
