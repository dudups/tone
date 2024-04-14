package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.ProjectAlarm;
import com.ezone.ezproject.dal.entity.ProjectAlarmExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProjectAlarmMapper {
    long countByExample(ProjectAlarmExample example);

    int deleteByExample(ProjectAlarmExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProjectAlarm record);

    int insertSelective(ProjectAlarm record);

    List<ProjectAlarm> selectByExample(ProjectAlarmExample example);

    ProjectAlarm selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProjectAlarm record, @Param("example") ProjectAlarmExample example);

    int updateByExample(@Param("record") ProjectAlarm record, @Param("example") ProjectAlarmExample example);

    int updateByPrimaryKeySelective(ProjectAlarm record);

    int updateByPrimaryKey(ProjectAlarm record);
}