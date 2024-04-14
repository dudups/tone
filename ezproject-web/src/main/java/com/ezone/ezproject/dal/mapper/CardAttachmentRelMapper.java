package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.CardAttachmentRel;
import com.ezone.ezproject.dal.entity.CardAttachmentRelExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CardAttachmentRelMapper {
    long countByExample(CardAttachmentRelExample example);

    int deleteByExample(CardAttachmentRelExample example);

    int deleteByPrimaryKey(Long id);

    int insert(CardAttachmentRel record);

    int insertSelective(CardAttachmentRel record);

    List<CardAttachmentRel> selectByExample(CardAttachmentRelExample example);

    CardAttachmentRel selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") CardAttachmentRel record, @Param("example") CardAttachmentRelExample example);

    int updateByExample(@Param("record") CardAttachmentRel record, @Param("example") CardAttachmentRelExample example);

    int updateByPrimaryKeySelective(CardAttachmentRel record);

    int updateByPrimaryKey(CardAttachmentRel record);
}