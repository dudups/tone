package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.StoryMapNode;
import com.ezone.ezproject.dal.entity.StoryMapNodeExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface StoryMapNodeMapper {
    long countByExample(StoryMapNodeExample example);

    int deleteByExample(StoryMapNodeExample example);

    int deleteByPrimaryKey(Long id);

    int insert(StoryMapNode record);

    int insertSelective(StoryMapNode record);

    List<StoryMapNode> selectByExample(StoryMapNodeExample example);

    StoryMapNode selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") StoryMapNode record, @Param("example") StoryMapNodeExample example);

    int updateByExample(@Param("record") StoryMapNode record, @Param("example") StoryMapNodeExample example);

    int updateByPrimaryKeySelective(StoryMapNode record);

    int updateByPrimaryKey(StoryMapNode record);
}