package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.ProductVersion;
import com.ezone.ezproject.dal.entity.ProductVersionExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProductVersionMapper {
    long countByExample(ProductVersionExample example);

    int deleteByExample(ProductVersionExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProductVersion record);

    int insertSelective(ProductVersion record);

    List<ProductVersion> selectByExample(ProductVersionExample example);

    ProductVersion selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProductVersion record, @Param("example") ProductVersionExample example);

    int updateByExample(@Param("record") ProductVersion record, @Param("example") ProductVersionExample example);

    int updateByPrimaryKeySelective(ProductVersion record);

    int updateByPrimaryKey(ProductVersion record);
}