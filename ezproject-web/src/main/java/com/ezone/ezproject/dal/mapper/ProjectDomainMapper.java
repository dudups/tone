package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.ProjectDomain;
import com.ezone.ezproject.dal.entity.ProjectDomainExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProjectDomainMapper {
    long countByExample(ProjectDomainExample example);

    int deleteByExample(ProjectDomainExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProjectDomain record);

    int insertSelective(ProjectDomain record);

    List<ProjectDomain> selectByExample(ProjectDomainExample example);

    ProjectDomain selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProjectDomain record, @Param("example") ProjectDomainExample example);

    int updateByExample(@Param("record") ProjectDomain record, @Param("example") ProjectDomainExample example);

    int updateByPrimaryKeySelective(ProjectDomain record);

    int updateByPrimaryKey(ProjectDomain record);
}