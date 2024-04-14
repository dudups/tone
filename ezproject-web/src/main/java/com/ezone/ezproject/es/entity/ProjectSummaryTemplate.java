package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.modules.project.service.ProjectSummaryService;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSummaryTemplate {
    public static final ProjectSummaryTemplate DEFAULT = ProjectSummaryTemplate.builder()
            .charts(ProjectSummaryService.CHARTS)
            .rightCharts(ProjectSummaryService.RIGHT_CHARTS)
            .build();

    @ApiModelProperty(value = "报表")
    private List<String> charts;

    @ApiModelProperty(value = "右侧报表")
    private List<String> rightCharts;
}
