package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.RelPortfolioProject;
import com.ezone.ezproject.dal.entity.RelPortfolioProjectExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface RelPortfolioProjectMapper {
    long countByExample(RelPortfolioProjectExample example);

    int deleteByExample(RelPortfolioProjectExample example);

    int deleteByPrimaryKey(Long id);

    int insert(RelPortfolioProject record);

    int insertSelective(RelPortfolioProject record);

    List<RelPortfolioProject> selectByExample(RelPortfolioProjectExample example);

    RelPortfolioProject selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") RelPortfolioProject record, @Param("example") RelPortfolioProjectExample example);

    int updateByExample(@Param("record") RelPortfolioProject record, @Param("example") RelPortfolioProjectExample example);

    int updateByPrimaryKeySelective(RelPortfolioProject record);

    int updateByPrimaryKey(RelPortfolioProject record);
}