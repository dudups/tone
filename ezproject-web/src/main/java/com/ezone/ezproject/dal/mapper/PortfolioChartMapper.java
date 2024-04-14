package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.PortfolioChart;
import com.ezone.ezproject.dal.entity.PortfolioChartExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PortfolioChartMapper {
    long countByExample(PortfolioChartExample example);

    int deleteByExample(PortfolioChartExample example);

    int deleteByPrimaryKey(Long id);

    int insert(PortfolioChart record);

    int insertSelective(PortfolioChart record);

    List<PortfolioChart> selectByExample(PortfolioChartExample example);

    PortfolioChart selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") PortfolioChart record, @Param("example") PortfolioChartExample example);

    int updateByExample(@Param("record") PortfolioChart record, @Param("example") PortfolioChartExample example);

    int updateByPrimaryKeySelective(PortfolioChart record);

    int updateByPrimaryKey(PortfolioChart record);
}