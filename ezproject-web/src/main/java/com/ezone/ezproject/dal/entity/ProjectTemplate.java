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
 *   项目模版
 *
 * This class was generated by MyBatis Generator.
 * This class corresponds to the database table project_template
 * @Author mybatis-code-generator
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProjectTemplate implements Serializable {
    @ApiModelProperty(value = "项目模版id")
    private Long id;

    @ApiModelProperty(value = "项目模版名")
    private String name;

    @ApiModelProperty(value = "公司")
    private Long companyId;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "创建人")
    private String createUser;

    @ApiModelProperty(value = "最近修改时间")
    private Date lastModifyTime;

    @ApiModelProperty(value = "最近修改人")
    private String lastModifyUser;

    @ApiModelProperty(value = "启用")
    private Boolean enable;

    @ApiModelProperty(value = "来源于系统/自定义")
    private String source;

    private static final long serialVersionUID = 1L;
}