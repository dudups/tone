package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.ProjectFavourite;
import com.ezone.ezproject.dal.entity.ProjectFavouriteExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProjectFavouriteMapper {
    long countByExample(ProjectFavouriteExample example);

    int deleteByExample(ProjectFavouriteExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProjectFavourite record);

    int insertSelective(ProjectFavourite record);

    List<ProjectFavourite> selectByExample(ProjectFavouriteExample example);

    ProjectFavourite selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProjectFavourite record, @Param("example") ProjectFavouriteExample example);

    int updateByExample(@Param("record") ProjectFavourite record, @Param("example") ProjectFavouriteExample example);

    int updateByPrimaryKeySelective(ProjectFavourite record);

    int updateByPrimaryKey(ProjectFavourite record);
}