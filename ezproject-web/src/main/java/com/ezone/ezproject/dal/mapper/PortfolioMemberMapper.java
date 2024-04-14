package com.ezone.ezproject.dal.mapper;

import com.ezone.ezproject.dal.entity.PortfolioMember;
import com.ezone.ezproject.dal.entity.PortfolioMemberExample;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PortfolioMemberMapper {
    long countByExample(PortfolioMemberExample example);

    int deleteByExample(PortfolioMemberExample example);

    int deleteByPrimaryKey(Long id);

    int insert(PortfolioMember record);

    int insertSelective(PortfolioMember record);

    List<PortfolioMember> selectByExample(PortfolioMemberExample example);

    PortfolioMember selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") PortfolioMember record, @Param("example") PortfolioMemberExample example);

    int updateByExample(@Param("record") PortfolioMember record, @Param("example") PortfolioMemberExample example);

    int updateByPrimaryKeySelective(PortfolioMember record);

    int updateByPrimaryKey(PortfolioMember record);
}