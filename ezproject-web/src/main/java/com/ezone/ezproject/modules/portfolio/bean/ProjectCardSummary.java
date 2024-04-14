package com.ezone.ezproject.modules.portfolio.bean;


import com.ezone.ezproject.dal.entity.Project;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCardSummary {
    private long projectId;

    @ApiModelProperty(value = "项目名称等信息")
    private Project project;

    @ApiModelProperty(value = "计划数")
    private long planCount;

    @ApiModelProperty(value = "卡片总数")
    private long cardCount;

    @ApiModelProperty(value = "结束状态卡片总数")
    private long endCardCount;

    @ApiModelProperty(value = "非结束状态卡片总数")
    private long notEndCardCount;

    @ApiModelProperty(value = "新建状态卡片总数")
    private long newCardCount;

    @ApiModelProperty(value = "进行中卡片总数：endCardCount/cardCount * 100")
    private double completion;

    @ApiModelProperty(value = "项目完成度（")
    private long processCardCount;

    @ApiModelProperty(value = "实际工时")
    private long actualWorkload;
}
