package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.CardExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CardMapper {
    long countByExample(CardExample example);

    int deleteByExample(CardExample example);

    int deleteByPrimaryKey(Long id);

    int insert(Card record);

    int insertSelective(Card record);

    List<Card> selectByExample(CardExample example);

    Card selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") Card record, @Param("example") CardExample example);

    int updateByExample(@Param("record") Card record, @Param("example") CardExample example);

    int updateByPrimaryKeySelective(Card record);

    int updateByPrimaryKey(Card record);
}