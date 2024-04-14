package com.ezone.ezproject.modules.portfolio.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.transactional.AfterCommit;
import com.ezone.ezproject.dal.entity.Portfolio;
import com.ezone.ezproject.dal.entity.PortfolioChart;
import com.ezone.ezproject.dal.entity.PortfolioChartExample;
import com.ezone.ezproject.dal.mapper.ExtPortfolioChartMapper;
import com.ezone.ezproject.es.dao.PortfolioChartConfigDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.enums.InnerCardType;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.chart.config.enums.DateInterval;
import com.ezone.ezproject.modules.chart.config.enums.MetricType;
import com.ezone.ezproject.modules.chart.config.range.DateBeforeRange;
import com.ezone.ezproject.modules.chart.ezinsight.config.BugTrend;
import com.ezone.ezproject.modules.chart.ezinsight.config.CardDelayList;
import com.ezone.ezproject.modules.chart.ezinsight.config.CardSummary;
import com.ezone.ezproject.modules.chart.ezinsight.config.CardTrend;
import com.ezone.ezproject.modules.chart.ezinsight.config.EzInsightChart;
import com.ezone.ezproject.modules.chart.ezinsight.config.EzInsightOneDimensionTable;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService;
import com.ezone.ezproject.modules.portfolio.bean.PortfolioChartRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
@AllArgsConstructor
public class PortfolioChartCmdService {
    private ExtPortfolioChartMapper chartMapper;
    private PortfolioChartConfigDao chartConfigDao;
    private CompanyProjectSchemaQueryService companyProjectSchemaQueryService;
    private PortfolioCharQueryService chartQueryService;

    private UserService userService;

    public PortfolioChart createChart(Portfolio portfolio, PortfolioChartRequest request) throws IOException {
        String user = userService.currentUserName();
        PortfolioChart chart = PortfolioChart.builder()
                .id(IdUtil.generateId())
                .portfolioId(portfolio.getId())
                .chartType(request.getConfigType())
                .title(request.getTitle())
                .seqNum(chartQueryService.nextChartSeqNum(portfolio.getId()))
                .createTime(new Date())
                .createUser(user)
                .lastModifyTime(new Date())
                .lastModifyUser(user)
                .build();
        chartMapper.insert(chart);
        chartConfigDao.saveOrUpdate(chart.getId(), request.getConfig());
        return chart;
    }

    /**
     * 初始化项目集概览报表
     *
     * @param portfolio
     * @throws IOException
     */
    public void initCharts(Portfolio portfolio, String user) {
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(portfolio.getCompanyId());
        Date createTime = new Date();
        List<PortfolioChart> charts = new ArrayList<>();
        PortfolioChart cardSummaryChart = PortfolioChart.builder()
                .id(IdUtil.generateId())
                .portfolioId(portfolio.getId())
                .chartType("cardSummary")
                .title("事项数据概览")
                .seqNum(0)
                .createTime(createTime)
                .createUser(user)
                .lastModifyTime(createTime)
                .lastModifyUser(user)
                .build();
        charts.add(cardSummaryChart);
        PortfolioChart cardTrendChart = PortfolioChart.builder()
                .id(IdUtil.generateId())
                .portfolioId(portfolio.getId())
                .chartType("cardTrend")
                .title("工作负载变化趋势")
                .seqNum(1)
                .createTime(createTime)
                .createUser(user)
                .lastModifyTime(createTime)
                .lastModifyUser(user)
                .build();
        charts.add(cardTrendChart);
        PortfolioChart bugTrendChart = PortfolioChart.builder()
                .id(IdUtil.generateId())
                .portfolioId(portfolio.getId())
                .chartType("bugTrend")
                .title("存量缺陷趋势")
                .seqNum(2)
                .createTime(createTime)
                .createUser(user)
                .lastModifyTime(createTime)
                .lastModifyUser(user)
                .build();
        charts.add(bugTrendChart);
        PortfolioChart oneDimensionTableChart = PortfolioChart.builder()
                .id(IdUtil.generateId())
                .portfolioId(portfolio.getId())
                .chartType("oneDimensionTable")
                .title("工时统计")
                .seqNum(3)
                .createTime(createTime)
                .createUser(user)
                .lastModifyTime(createTime)
                .lastModifyUser(user)
                .build();
        charts.add(oneDimensionTableChart);
        PortfolioChart cardDelayListChart = PortfolioChart.builder()
                .id(IdUtil.generateId())
                .portfolioId(portfolio.getId())
                .chartType("cardDelayList")
                .title("超时卡片列表")
                .seqNum(4)
                .createTime(createTime)
                .createUser(user)
                .lastModifyTime(createTime)
                .lastModifyUser(user)
                .build();
        charts.add(cardDelayListChart);
        chartMapper.batchInsert(charts);
        List<String> allCardTypes = new ArrayList<>();
        List<String> bugCardTypes = new ArrayList<>();
        companyCardSchema.getTypes().forEach(type -> {
            allCardTypes.add(type.getKey());
            if (type.getInnerType().equals(InnerCardType.bug.name())) {
                bugCardTypes.add(type.getKey());
            }
        });

        try {
            Map<Long, EzInsightChart> esCharts = new HashMap<>();
            CardSummary cardSummary = CardSummary.builder().cardTypes(allCardTypes).excludeNoPlan(false).build();
            esCharts.put(cardSummaryChart.getId(), cardSummary);
            CardTrend cardTrend = CardTrend.builder().cardTypes(allCardTypes).dateInterval(DateInterval.DAY).dateRange(DateBeforeRange.builder().days(7).build()).build();
            esCharts.put(cardTrendChart.getId(), cardTrend);
            BugTrend bugTrend = BugTrend.builder().bugCardTypes(bugCardTypes).dateInterval(DateInterval.DAY).dateRange(DateBeforeRange.builder().days(7).build()).build();
            esCharts.put(bugTrendChart.getId(), bugTrend);
            CardDelayList cardDelayList = CardDelayList.builder().cardTypes(allCardTypes).isEnd(false).dateRange(DateBeforeRange.builder().days(7).build()).build();
            esCharts.put(cardDelayListChart.getId(), cardDelayList);
            EzInsightOneDimensionTable oneDimensionTable = EzInsightOneDimensionTable.builder().classifyField(CardField.OWNER_USERS).metricType(MetricType.SumCardField)
                    .metricField(CardField.ACTUAL_WORKLOAD).queries(Collections.EMPTY_LIST).build();
            esCharts.put(oneDimensionTableChart.getId(), oneDimensionTable);
            chartConfigDao.saveOrUpdate(esCharts);
        } catch (IOException e) {
            log.error("[initCharts][" + " portfolio :" + portfolio + "][error][" + e.getMessage() + "]", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public PortfolioChart updateChart(Long portfolioId, Long id, PortfolioChartRequest request) throws IOException {
        String user = userService.currentUserName();
        PortfolioChart chart = chartQueryService.selectChart(portfolioId, id);
        if (chart == null) {
            throw CodedException.NOT_FOUND;
        }
        chart.setChartType(request.getConfigType());
        chart.setTitle(request.getTitle());
        chart.setLastModifyTime(new Date());
        chart.setLastModifyUser(user);
        chartMapper.updateByPrimaryKey(chart);
        chartConfigDao.saveOrUpdate(id, request.getConfig());
        return chart;
    }

    public PortfolioChart moveChart(Long portfolioId, Long id, Long afterId) {
        PortfolioChart chart = chartQueryService.selectChart(portfolioId, id);
        if (chart == null) {
            throw CodedException.NOT_FOUND;
        }
        if (chart.getId().equals(afterId)) {
            return chart;
        }
        List<PortfolioChart> charts = chartQueryService.selectChartByPortfolioId(chart.getPortfolioId());
        List<PortfolioChart> sortCharts = new ArrayList<>();
        if (afterId == null || afterId <= 0) {
            sortCharts.add(chart);
            sortCharts.addAll(charts.stream().filter(c -> !c.getId().equals(chart.getId())).collect(Collectors.toList()));
        } else {
            boolean findAfter = false;
            for (int i = 0; i < charts.size(); i++) {
                PortfolioChart item = charts.get(i);
                if (item.getId().equals(chart.getId())) {
                    continue;
                }
                sortCharts.add(item);
                if (!findAfter && item.getId().equals(afterId)) {
                    findAfter = true;
                    sortCharts.add(chart);
                }
            }
            if (!findAfter) {
                sortCharts.add(chart);
            }
        }
        for (int i = 0; i < sortCharts.size(); i++) {
            PortfolioChart item = sortCharts.get(i);
            item.setSeqNum(i);
            chartMapper.updateByPrimaryKey(item);
        }
        return chart;
    }

    public void deleteChart(Long portfolioId, Long id) throws IOException {
        PortfolioChart chart = chartQueryService.selectChart(portfolioId, id);
        if (chart != null) {
            chartMapper.deleteByPrimaryKey(id);
            chartConfigDao.delete(id);
        }
    }

    @AfterCommit
    @Async
    public void deleteCharts(Long portfolioId) {
        List<PortfolioChart> charts = chartQueryService.selectChartByPortfolioId(portfolioId);
        if (CollectionUtils.isNotEmpty(charts)) {
            PortfolioChartExample example = new PortfolioChartExample();
            example.createCriteria().andPortfolioIdEqualTo(portfolioId);
            chartMapper.deleteByExample(example);
            try {
                chartConfigDao.delete(charts.stream().map(PortfolioChart::getId).collect(Collectors.toList()));
            } catch (IOException e) {
                log.error("[deleteCharts][" + " portfolioId :" + portfolioId + "][error][" + e.getMessage() + "]", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }

    }
}
