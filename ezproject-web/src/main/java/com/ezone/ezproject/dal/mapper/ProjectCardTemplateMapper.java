package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.ProjectCardTemplate;
import com.ezone.ezproject.dal.entity.ProjectCardTemplateExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProjectCardTemplateMapper {
    long countByExample(ProjectCardTemplateExample example);

    int deleteByExample(ProjectCardTemplateExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProjectCardTemplate record);

    int insertSelective(ProjectCardTemplate record);

    List<ProjectCardTemplate> selectByExample(ProjectCardTemplateExample example);

    ProjectCardTemplate selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProjectCardTemplate record, @Param("example") ProjectCardTemplateExample example);

    int updateByExample(@Param("record") ProjectCardTemplate record, @Param("example") ProjectCardTemplateExample example);

    int updateByPrimaryKeySelective(ProjectCardTemplate record);

    int updateByPrimaryKey(ProjectCardTemplate record);
}