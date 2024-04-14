package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.CardQueryView;
import com.ezone.ezproject.dal.entity.CardQueryViewExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CardQueryViewMapper {
    long countByExample(CardQueryViewExample example);

    int deleteByExample(CardQueryViewExample example);

    int deleteByPrimaryKey(Long id);

    int insert(CardQueryView record);

    int insertSelective(CardQueryView record);

    List<CardQueryView> selectByExample(CardQueryViewExample example);

    CardQueryView selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") CardQueryView record, @Param("example") CardQueryViewExample example);

    int updateByExample(@Param("record") CardQueryView record, @Param("example") CardQueryViewExample example);

    int updateByPrimaryKeySelective(CardQueryView record);

    int updateByPrimaryKey(CardQueryView record);
}