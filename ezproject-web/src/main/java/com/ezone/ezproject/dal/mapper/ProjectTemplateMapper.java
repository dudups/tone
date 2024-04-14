package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.ProjectTemplate;
import com.ezone.ezproject.dal.entity.ProjectTemplateExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProjectTemplateMapper {
    long countByExample(ProjectTemplateExample example);

    int deleteByExample(ProjectTemplateExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProjectTemplate record);

    int insertSelective(ProjectTemplate record);

    List<ProjectTemplate> selectByExample(ProjectTemplateExample example);

    ProjectTemplate selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProjectTemplate record, @Param("example") ProjectTemplateExample example);

    int updateByExample(@Param("record") ProjectTemplate record, @Param("example") ProjectTemplateExample example);

    int updateByPrimaryKeySelective(ProjectTemplate record);

    int updateByPrimaryKey(ProjectTemplate record);
}