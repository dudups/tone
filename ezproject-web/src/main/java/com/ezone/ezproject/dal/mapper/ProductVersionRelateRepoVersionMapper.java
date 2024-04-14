package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.ProductVersionRelateRepoVersion;
import com.ezone.ezproject.dal.entity.ProductVersionRelateRepoVersionExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ProductVersionRelateRepoVersionMapper {
    long countByExample(ProductVersionRelateRepoVersionExample example);

    int deleteByExample(ProductVersionRelateRepoVersionExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ProductVersionRelateRepoVersion record);

    int insertSelective(ProductVersionRelateRepoVersion record);

    List<ProductVersionRelateRepoVersion> selectByExample(ProductVersionRelateRepoVersionExample example);

    ProductVersionRelateRepoVersion selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProductVersionRelateRepoVersion record, @Param("example") ProductVersionRelateRepoVersionExample example);

    int updateByExample(@Param("record") ProductVersionRelateRepoVersion record, @Param("example") ProductVersionRelateRepoVersionExample example);

    int updateByPrimaryKeySelective(ProductVersionRelateRepoVersion record);

    int updateByPrimaryKey(ProductVersionRelateRepoVersion record);
}