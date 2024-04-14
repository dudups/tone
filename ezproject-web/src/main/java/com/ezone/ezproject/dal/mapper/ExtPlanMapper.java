package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.PlanExample;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ExtPlanMapper extends PlanMapper{
    public List<Map> planCountGroupByProject(@Param("projectIds") List<Long> projectIds);
}