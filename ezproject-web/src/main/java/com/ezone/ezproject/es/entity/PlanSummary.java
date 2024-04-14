package com.ezone.ezproject.es.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PlanSummary {
    @ApiModelProperty(value = "最近修改时间")
    private Date lastModifyTime;

    @ApiModelProperty(value = "最近修改人")
    private String lastModifyUser;

    @ApiModelProperty(value = "回顾纪要内容")
    private String content;
}
