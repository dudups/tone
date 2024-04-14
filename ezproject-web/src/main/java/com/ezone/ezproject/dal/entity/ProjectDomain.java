package com.ezone.ezproject.dal.entity;

import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Database Table Remarks:
 *   project domain
 *
 * This class was generated by MyBatis Generator.
 * This class corresponds to the database table project_domain
 * @Author mybatis-code-generator
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProjectDomain implements Serializable {
    @ApiModelProperty(value = "项目id")
    private Long id;

    @ApiModelProperty(value = "项目下卡片最大编号")
    private Long maxSeqNum;

    @ApiModelProperty(value = "优先级排序地位，字典序")
    private String maxRank;

    private static final long serialVersionUID = 1L;
}