package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.ProjectDocSpace;
import com.ezone.ezproject.dal.entity.ProjectDocSpaceExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProjectDocSpaceMapper {
    long countByExample(ProjectDocSpaceExample example);

    int deleteByExample(ProjectDocSpaceExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProjectDocSpace record);

    int insertSelective(ProjectDocSpace record);

    List<ProjectDocSpace> selectByExample(ProjectDocSpaceExample example);

    ProjectDocSpace selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProjectDocSpace record, @Param("example") ProjectDocSpaceExample example);

    int updateByExample(@Param("record") ProjectDocSpace record, @Param("example") ProjectDocSpaceExample example);

    int updateByPrimaryKeySelective(ProjectDocSpace record);

    int updateByPrimaryKey(ProjectDocSpace record);
}