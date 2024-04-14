package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.PortfolioChart;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtPortfolioChartMapper extends PortfolioChartMapper {
    int batchInsert(@Param("list") List<PortfolioChart> list);
}