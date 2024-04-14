package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.ProjectWikiSpace;
import com.ezone.ezproject.dal.entity.ProjectWikiSpaceExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProjectWikiSpaceMapper {
    long countByExample(ProjectWikiSpaceExample example);

    int deleteByExample(ProjectWikiSpaceExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProjectWikiSpace record);

    int insertSelective(ProjectWikiSpace record);

    List<ProjectWikiSpace> selectByExample(ProjectWikiSpaceExample example);

    ProjectWikiSpace selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProjectWikiSpace record, @Param("example") ProjectWikiSpaceExample example);

    int updateByExample(@Param("record") ProjectWikiSpace record, @Param("example") ProjectWikiSpaceExample example);

    int updateByPrimaryKeySelective(ProjectWikiSpace record);

    int updateByPrimaryKey(ProjectWikiSpace record);
}