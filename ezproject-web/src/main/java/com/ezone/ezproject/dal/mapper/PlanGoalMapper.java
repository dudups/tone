package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.PlanGoal;
import com.ezone.ezproject.dal.entity.PlanGoalExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PlanGoalMapper {
    long countByExample(PlanGoalExample example);

    int deleteByExample(PlanGoalExample example);

    int deleteByPrimaryKey(Long id);

    int insert(PlanGoal record);

    int insertSelective(PlanGoal record);

    List<PlanGoal> selectByExample(PlanGoalExample example);

    PlanGoal selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") PlanGoal record, @Param("example") PlanGoalExample example);

    int updateByExample(@Param("record") PlanGoal record, @Param("example") PlanGoalExample example);

    int updateByPrimaryKeySelective(PlanGoal record);

    int updateByPrimaryKey(PlanGoal record);
}