package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.StoryMap;
import com.ezone.ezproject.dal.entity.StoryMapExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface StoryMapMapper {
    long countByExample(StoryMapExample example);

    int deleteByExample(StoryMapExample example);

    int deleteByPrimaryKey(Long id);

    int insert(StoryMap record);

    int insertSelective(StoryMap record);

    List<StoryMap> selectByExample(StoryMapExample example);

    StoryMap selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") StoryMap record, @Param("example") StoryMapExample example);

    int updateByExample(@Param("record") StoryMap record, @Param("example") StoryMapExample example);

    int updateByPrimaryKeySelective(StoryMap record);

    int updateByPrimaryKey(StoryMap record);
}