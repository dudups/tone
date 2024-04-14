package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.Portfolio;
import com.ezone.ezproject.dal.entity.PortfolioExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PortfolioMapper {
    long countByExample(PortfolioExample example);

    int deleteByExample(PortfolioExample example);

    int deleteByPrimaryKey(Long id);

    int insert(Portfolio record);

    int insertSelective(Portfolio record);

    List<Portfolio> selectByExample(PortfolioExample example);

    Portfolio selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") Portfolio record, @Param("example") PortfolioExample example);

    int updateByExample(@Param("record") Portfolio record, @Param("example") PortfolioExample example);

    int updateByPrimaryKeySelective(Portfolio record);

    int updateByPrimaryKey(Portfolio record);
}