package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.ProjectHostGroup;
import com.ezone.ezproject.dal.entity.ProjectHostGroupExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProjectHostGroupMapper {
    long countByExample(ProjectHostGroupExample example);

    int deleteByExample(ProjectHostGroupExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProjectHostGroup record);

    int insertSelective(ProjectHostGroup record);

    List<ProjectHostGroup> selectByExample(ProjectHostGroupExample example);

    ProjectHostGroup selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProjectHostGroup record, @Param("example") ProjectHostGroupExample example);

    int updateByExample(@Param("record") ProjectHostGroup record, @Param("example") ProjectHostGroupExample example);

    int updateByPrimaryKeySelective(ProjectHostGroup record);

    int updateByPrimaryKey(ProjectHostGroup record);
}