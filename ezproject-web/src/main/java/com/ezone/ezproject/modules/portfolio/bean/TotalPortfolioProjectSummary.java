package com.ezone.ezproject.modules.portfolio.bean;

import com.ezone.ezproject.dal.entity.Portfolio;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.RelPortfolioProject;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotalPortfolioProjectSummary {
    @ApiModelProperty(value = "项目集")
    private List<Portfolio> portfolios;

    @ApiModelProperty(value = "关联的项目，key-projectID")
    private Map<Long, Project> projects;

    @ApiModelProperty(value = "当前页面返回对象集合,key-PortfolioId")
    private Map<Long, List<RelPortfolioProject>> relPortfolioProjects;

    @ApiModelProperty(value = "项目统计信息，key-projectID")
    private Map<Long, ProjectCardSummary> projectCardSummaryMap;
}
