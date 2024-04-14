package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.CardToken;
import com.ezone.ezproject.dal.entity.CardTokenExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CardTokenMapper {
    long countByExample(CardTokenExample example);

    int deleteByExample(CardTokenExample example);

    int deleteByPrimaryKey(Long id);

    int insert(CardToken record);

    int insertSelective(CardToken record);

    List<CardToken> selectByExample(CardTokenExample example);

    CardToken selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") CardToken record, @Param("example") CardTokenExample example);

    int updateByExample(@Param("record") CardToken record, @Param("example") CardTokenExample example);

    int updateByPrimaryKeySelective(CardToken record);

    int updateByPrimaryKey(CardToken record);
}