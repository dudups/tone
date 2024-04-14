package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.ProjectRepo;
import com.ezone.ezproject.dal.entity.ProjectRepoExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProjectRepoMapper {
    long countByExample(ProjectRepoExample example);

    int deleteByExample(ProjectRepoExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProjectRepo record);

    int insertSelective(ProjectRepo record);

    List<ProjectRepo> selectByExample(ProjectRepoExample example);

    ProjectRepo selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProjectRepo record, @Param("example") ProjectRepoExample example);

    int updateByExample(@Param("record") ProjectRepo record, @Param("example") ProjectRepoExample example);

    int updateByPrimaryKeySelective(ProjectRepo record);

    int updateByPrimaryKey(ProjectRepo record);
}