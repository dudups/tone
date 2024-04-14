package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.CardWikiPageRel;
import com.ezone.ezproject.dal.entity.CardWikiPageRelExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CardWikiPageRelMapper {
    long countByExample(CardWikiPageRelExample example);

    int deleteByExample(CardWikiPageRelExample example);

    int deleteByPrimaryKey(Long id);

    int insert(CardWikiPageRel record);

    int insertSelective(CardWikiPageRel record);

    List<CardWikiPageRel> selectByExample(CardWikiPageRelExample example);

    CardWikiPageRel selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") CardWikiPageRel record, @Param("example") CardWikiPageRelExample example);

    int updateByExample(@Param("record") CardWikiPageRel record, @Param("example") CardWikiPageRelExample example);

    int updateByPrimaryKeySelective(CardWikiPageRel record);

    int updateByPrimaryKey(CardWikiPageRel record);
}