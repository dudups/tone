package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.ProjectMember;
import com.ezone.ezproject.dal.entity.ProjectMemberExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProjectMemberMapper {
    long countByExample(ProjectMemberExample example);

    int deleteByExample(ProjectMemberExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProjectMember record);

    int insertSelective(ProjectMember record);

    List<ProjectMember> selectByExample(ProjectMemberExample example);

    ProjectMember selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProjectMember record, @Param("example") ProjectMemberExample example);

    int updateByExample(@Param("record") ProjectMember record, @Param("example") ProjectMemberExample example);

    int updateByPrimaryKeySelective(ProjectMember record);

    int updateByPrimaryKey(ProjectMember record);
}