package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.AttachmentCopy1;
import com.ezone.ezproject.dal.entity.AttachmentCopy1Example;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AttachmentCopy1Mapper {
    long countByExample(AttachmentCopy1Example example);

    int deleteByExample(AttachmentCopy1Example example);

    int deleteByPrimaryKey(Long id);

    int insert(AttachmentCopy1 record);

    int insertSelective(AttachmentCopy1 record);

    List<AttachmentCopy1> selectByExample(AttachmentCopy1Example example);

    AttachmentCopy1 selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") AttachmentCopy1 record, @Param("example") AttachmentCopy1Example example);

    int updateByExample(@Param("record") AttachmentCopy1 record, @Param("example") AttachmentCopy1Example example);

    int updateByPrimaryKeySelective(AttachmentCopy1 record);

    int updateByPrimaryKey(AttachmentCopy1 record);
}