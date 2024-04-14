package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.CardRelateRel;
import com.ezone.ezproject.dal.entity.CardRelateRelExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CardRelateRelMapper {
    long countByExample(CardRelateRelExample example);

    int deleteByExample(CardRelateRelExample example);

    int deleteByPrimaryKey(Long id);

    int insert(CardRelateRel record);

    int insertSelective(CardRelateRel record);

    List<CardRelateRel> selectByExample(CardRelateRelExample example);

    CardRelateRel selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") CardRelateRel record, @Param("example") CardRelateRelExample example);

    int updateByExample(@Param("record") CardRelateRel record, @Param("example") CardRelateRelExample example);

    int updateByPrimaryKeySelective(CardRelateRel record);

    int updateByPrimaryKey(CardRelateRel record);
}