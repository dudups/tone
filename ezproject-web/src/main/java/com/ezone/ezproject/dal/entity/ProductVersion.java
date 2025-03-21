package com.ezone.ezproject.dal.entity;

import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Database Table Remarks:
 *   项目版本管理
 *
 * This class was generated by MyBatis Generator.
 * This class corresponds to the database table product_version
 * @Author mybatis-code-generator
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProductVersion implements Serializable {
    @ApiModelProperty(value = "版本id")
    private Long id;

    @ApiModelProperty(value = "所属公司id")
    private Long companyId;

    @ApiModelProperty(value = "版本名称")
    private String title;

    @ApiModelProperty(value = "所属项目ID")
    private Long projectId;

    @ApiModelProperty(value = "创建人")
    private String createUser;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "活跃或已经归档")
    private Boolean isActive;

    @ApiModelProperty(value = "逻辑删除 0-正常 1-删除")
    private Boolean deleted;

    @ApiModelProperty(value = "归档人")
    private String inactiveUser;

    @ApiModelProperty(value = "归档时间")
    private Date inactiveTime;

    private static final long serialVersionUID = 1L;
}