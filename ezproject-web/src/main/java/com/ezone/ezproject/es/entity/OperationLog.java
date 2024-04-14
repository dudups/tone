package com.ezone.ezproject.es.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

/**
 * @author cf
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OperationLog {
    private Long id;
    @ApiModelProperty(value = "创建时间")
    private Date createTime;
    @ApiModelProperty(value = "操作人")
    private String operator;
    @ApiModelProperty(value = "操作IP")
    private String ip;
    @ApiModelProperty(value = "变更详情")
    private String detail;
    @ApiModelProperty(value = "所属项目")
    private Long projectId;
    @ApiModelProperty(value = "操作类型")
    private String operateType;
    @ApiModelProperty(value = "操作资源ID",hidden = true)
    private String resourceId;
    @ApiModelProperty(value = "变更后内容",hidden = true)
    private String value;
}
