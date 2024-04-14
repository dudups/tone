package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.Portfolio;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtPortfolioMapper extends PortfolioMapper{
    List<Portfolio> selectRelPortfolioByProjectId(@Param("projectId") Long projectId);
}