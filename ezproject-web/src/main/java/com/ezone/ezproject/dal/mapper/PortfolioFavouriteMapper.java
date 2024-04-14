package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.PortfolioFavourite;
import com.ezone.ezproject.dal.entity.PortfolioFavouriteExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PortfolioFavouriteMapper {
    long countByExample(PortfolioFavouriteExample example);

    int deleteByExample(PortfolioFavouriteExample example);

    int deleteByPrimaryKey(Long id);

    int insert(PortfolioFavourite record);

    int insertSelective(PortfolioFavourite record);

    List<PortfolioFavourite> selectByExample(PortfolioFavouriteExample example);

    PortfolioFavourite selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") PortfolioFavourite record, @Param("example") PortfolioFavouriteExample example);

    int updateByExample(@Param("record") PortfolioFavourite record, @Param("example") PortfolioFavouriteExample example);

    int updateByPrimaryKeySelective(PortfolioFavourite record);

    int updateByPrimaryKey(PortfolioFavourite record);
}