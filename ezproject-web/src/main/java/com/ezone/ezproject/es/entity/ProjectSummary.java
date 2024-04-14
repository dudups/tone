package com.ezone.ezproject.es.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProjectSummary {
    @ApiModelProperty(value = "最近修改时间")
    private Date lastModifyTime;

    @ApiModelProperty(value = "最近修改人")
    private String lastModifyUser;

    @ApiModelProperty(value = "报表")
    private List<String> charts;

    @ApiModelProperty(value = "右侧报表")
    private List<String> rightCharts;

}
