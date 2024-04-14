package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.ProjectChartGroup;
import com.ezone.ezproject.dal.entity.ProjectChartGroupExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProjectChartGroupMapper {
    long countByExample(ProjectChartGroupExample example);

    int deleteByExample(ProjectChartGroupExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProjectChartGroup record);

    int insertSelective(ProjectChartGroup record);

    List<ProjectChartGroup> selectByExample(ProjectChartGroupExample example);

    ProjectChartGroup selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProjectChartGroup record, @Param("example") ProjectChartGroupExample example);

    int updateByExample(@Param("record") ProjectChartGroup record, @Param("example") ProjectChartGroupExample example);

    int updateByPrimaryKeySelective(ProjectChartGroup record);

    int updateByPrimaryKey(ProjectChartGroup record);
}