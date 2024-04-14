package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.ProjectChart;
import com.ezone.ezproject.dal.entity.ProjectChartExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProjectChartMapper {
    long countByExample(ProjectChartExample example);

    int deleteByExample(ProjectChartExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProjectChart record);

    int insertSelective(ProjectChart record);

    List<ProjectChart> selectByExample(ProjectChartExample example);

    ProjectChart selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProjectChart record, @Param("example") ProjectChartExample example);

    int updateByExample(@Param("record") ProjectChart record, @Param("example") ProjectChartExample example);

    int updateByPrimaryKeySelective(ProjectChart record);

    int updateByPrimaryKey(ProjectChart record);
}