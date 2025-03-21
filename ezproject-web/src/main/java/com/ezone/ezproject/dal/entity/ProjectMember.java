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
 *   项目成员
 *
 * This class was generated by MyBatis Generator.
 * This class corresponds to the database table project_member
 * @Author mybatis-code-generator
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProjectMember implements Serializable {
    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "项目ID")
    private Long projectId;

    @ApiModelProperty(value = "用户类型")
    private String userType;

    @ApiModelProperty(value = "成员用户名")
    private String user;

    @ApiModelProperty(value = "成员角色")
    private String role;

    @ApiModelProperty(value = "公司ID")
    private Long companyId;

    @ApiModelProperty(value = "角色来源")
    private String roleSource;

    @ApiModelProperty(value = "角色类型")
    private String roleType;

    private static final long serialVersionUID = 1L;
}