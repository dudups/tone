package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.ProjectK8sGroup;
import com.ezone.ezproject.dal.entity.ProjectK8sGroupExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProjectK8sGroupMapper {
    long countByExample(ProjectK8sGroupExample example);

    int deleteByExample(ProjectK8sGroupExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProjectK8sGroup record);

    int insertSelective(ProjectK8sGroup record);

    List<ProjectK8sGroup> selectByExample(ProjectK8sGroupExample example);

    ProjectK8sGroup selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProjectK8sGroup record, @Param("example") ProjectK8sGroupExample example);

    int updateByExample(@Param("record") ProjectK8sGroup record, @Param("example") ProjectK8sGroupExample example);

    int updateByPrimaryKeySelective(ProjectK8sGroup record);

    int updateByPrimaryKey(ProjectK8sGroup record);
}