package com.ezone.ezproject.dal.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * Database Table Remarks:
 * 项目中的预警规则
 * <p>
 * This class was generated by MyBatis Generator.
 * This class corresponds to the database table project_alarm
 *
 * @Author mybatis-code-generator
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProjectAlarm implements Serializable {
    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "项目ID")
    private Long projectId;

    @ApiModelProperty(value = "规则名称")
    private String name;

    private static final long serialVersionUID = 1L;
}