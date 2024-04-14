package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.CardAlarmNoticePlan;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtCardAlarmNoticePlanMapper extends CardAlarmNoticePlanMapper{
    void batchInsert(@Param("list") List<CardAlarmNoticePlan> list);
}