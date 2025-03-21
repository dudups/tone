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
 *   项目关联k8s集群
 *
 * This class was generated by MyBatis Generator.
 * This class corresponds to the database table project_k8s_group
 * @Author mybatis-code-generator
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProjectK8sGroup implements Serializable {
    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "项目ID")
    private Long projectId;

    @ApiModelProperty(value = "k8s 集群ID")
    private Long k8sGroupId;

    @ApiModelProperty(value = "公司ID")
    private Long companyId;

    @ApiModelProperty(value = "添加人")
    private String createUser;

    @ApiModelProperty(value = "添加时间")
    private Date createTime;

    private static final long serialVersionUID = 1L;
}