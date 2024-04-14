package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.ProjectArtifactRepo;
import com.ezone.ezproject.dal.entity.ProjectArtifactRepoExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProjectArtifactRepoMapper {
    long countByExample(ProjectArtifactRepoExample example);

    int deleteByExample(ProjectArtifactRepoExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProjectArtifactRepo record);

    int insertSelective(ProjectArtifactRepo record);

    List<ProjectArtifactRepo> selectByExample(ProjectArtifactRepoExample example);

    ProjectArtifactRepo selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProjectArtifactRepo record, @Param("example") ProjectArtifactRepoExample example);

    int updateByExample(@Param("record") ProjectArtifactRepo record, @Param("example") ProjectArtifactRepoExample example);

    int updateByPrimaryKeySelective(ProjectArtifactRepo record);

    int updateByPrimaryKey(ProjectArtifactRepo record);
}