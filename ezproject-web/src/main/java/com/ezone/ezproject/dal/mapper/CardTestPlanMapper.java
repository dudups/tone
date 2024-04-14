package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.CardTestPlan;
import com.ezone.ezproject.dal.entity.CardTestPlanExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CardTestPlanMapper {
    long countByExample(CardTestPlanExample example);

    int deleteByExample(CardTestPlanExample example);

    int deleteByPrimaryKey(Long id);

    int insert(CardTestPlan record);

    int insertSelective(CardTestPlan record);

    List<CardTestPlan> selectByExample(CardTestPlanExample example);

    CardTestPlan selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") CardTestPlan record, @Param("example") CardTestPlanExample example);

    int updateByExample(@Param("record") CardTestPlan record, @Param("example") CardTestPlanExample example);

    int updateByPrimaryKeySelective(CardTestPlan record);

    int updateByPrimaryKey(CardTestPlan record);
}