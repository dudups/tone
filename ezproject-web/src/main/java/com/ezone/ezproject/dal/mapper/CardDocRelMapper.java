package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.CardDocRel;
import com.ezone.ezproject.dal.entity.CardDocRelExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CardDocRelMapper {
    long countByExample(CardDocRelExample example);

    int deleteByExample(CardDocRelExample example);

    int deleteByPrimaryKey(Long id);

    int insert(CardDocRel record);

    int insertSelective(CardDocRel record);

    List<CardDocRel> selectByExample(CardDocRelExample example);

    CardDocRel selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") CardDocRel record, @Param("example") CardDocRelExample example);

    int updateByExample(@Param("record") CardDocRel record, @Param("example") CardDocRelExample example);

    int updateByPrimaryKeySelective(CardDocRel record);

    int updateByPrimaryKey(CardDocRel record);
}