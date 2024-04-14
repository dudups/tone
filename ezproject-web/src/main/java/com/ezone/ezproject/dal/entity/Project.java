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
 *   project
 *
 * This class was generated by MyBatis Generator.
 * This class corresponds to the database table project
 * @Author mybatis-code-generator
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Project implements Serializable {
    @ApiModelProperty(value = "项目id")
    private Long id;

    @ApiModelProperty(value = "项目名")
    private String name;

    @ApiModelProperty(value = "公司")
    private Long companyId;

    @ApiModelProperty(value = "项目描述")
    private String description;

    @ApiModelProperty(value = "项目标识")
    private String key;

    @ApiModelProperty(value = "项目下卡片最大编号")
    private Long maxSeqNum;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "创建人")
    private String createUser;

    @ApiModelProperty(value = "最近修改时间")
    private Date lastModifyTime;

    @ApiModelProperty(value = "最近修改人")
    private String lastModifyUser;

    @ApiModelProperty(value = "企业内私有/公开")
    private Boolean isPrivate;

    @ApiModelProperty(value = "优先级排序地位，字典序")
    private String maxRank;

    @ApiModelProperty(value = "卡片逻辑删除后保留天数")
    private Long keepDays;

    @ApiModelProperty(value = "计划归档后保留天数")
    private Long planKeepDays;

    @ApiModelProperty(value = "置顶排序值（加入置顶时的时间戳， 0表示不置顶，值越大，排序越靠前。")
    private Long topScore;

    @ApiModelProperty(value = "严格模式")
    private Boolean isStrict;

    @ApiModelProperty(value = "开始时间")
    private Date startTime;

    @ApiModelProperty(value = "结束时间")
    private Date endTime;

    @ApiModelProperty(value = "活跃或已归档")
    private Boolean isActive;

    private static final long serialVersionUID = 1L;
}