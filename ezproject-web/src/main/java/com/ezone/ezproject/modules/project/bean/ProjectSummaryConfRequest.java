package com.ezone.ezproject.modules.project.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * @author yinchengfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProjectSummaryConfRequest {
    List<String> charts;
    List<String> rightCharts;
}
