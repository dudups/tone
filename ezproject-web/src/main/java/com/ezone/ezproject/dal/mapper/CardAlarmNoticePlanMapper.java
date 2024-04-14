package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.CardAlarmNoticePlan;
import com.ezone.ezproject.dal.entity.CardAlarmNoticePlanExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CardAlarmNoticePlanMapper {
    long countByExample(CardAlarmNoticePlanExample example);

    int deleteByExample(CardAlarmNoticePlanExample example);

    int deleteByPrimaryKey(Long id);

    int insert(CardAlarmNoticePlan record);

    int insertSelective(CardAlarmNoticePlan record);

    List<CardAlarmNoticePlan> selectByExample(CardAlarmNoticePlanExample example);

    CardAlarmNoticePlan selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") CardAlarmNoticePlan record, @Param("example") CardAlarmNoticePlanExample example);

    int updateByExample(@Param("record") CardAlarmNoticePlan record, @Param("example") CardAlarmNoticePlanExample example);

    int updateByPrimaryKeySelective(CardAlarmNoticePlan record);

    int updateByPrimaryKey(CardAlarmNoticePlan record);
}