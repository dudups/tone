package com.ezone.ezproject.modules.project.controller;

import com.ezone.devops.ezcode.base.enums.Dimension;
import com.ezone.devops.ezcode.sdk.service.InternalStatService;
import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.dal.entity.ProjectRepo;
import com.ezone.ezproject.es.entity.ProjectSummary;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.chart.config.range.DateRange;
import com.ezone.ezproject.modules.cli.EzPipelineCliService;
import com.ezone.ezproject.modules.project.bean.ChartDataRequest;
import com.ezone.ezproject.modules.project.service.ProjectRepoService;
import com.ezone.ezproject.modules.project.service.ProjectSummaryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@ApiOperation("项目概览")
@RestController
@RequestMapping("/project/project/{projectId:[0-9]+}/summary")
@Slf4j
@AllArgsConstructor
public class ProjectSummaryController extends AbstractController {
    private ProjectSummaryService projectSummaryService;
    private ProjectRepoService projectRepoService;

    private InternalStatService repoStatService;
    private EzPipelineCliService ezPipelineCliService;

    @ApiOperation("保存项目概览设置")
    @PutMapping
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse save(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId,
                             @ApiParam(value = "chart列表") @NotEmpty @RequestBody List<String> charts)
            throws IOException {
        checkPermission(projectId, OperationType.PROJECT_MANAGE_UPDATE);
        projectSummaryService.reOrderProjectSummary(projectId, charts, null);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("保存右侧项目概览设置")
    @PutMapping("saveSummaryRight")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<Object> saveSummaryRight(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId,
                                                 @ApiParam(value = "chart列表") @NotEmpty @RequestBody List<String> rightCharts)
            throws IOException {
        checkPermission(projectId, OperationType.PROJECT_MANAGE_UPDATE);
        projectSummaryService.reOrderProjectSummary(projectId, null, rightCharts);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询项目概览设置")
    @GetMapping
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<ProjectSummary> selectByPlanId(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId)
            throws IOException {
        checkHasProjectRead(projectId);
        return success(projectSummaryService.find(projectId));
    }

    @ApiOperation("查询项目概览对应bug图表的数据")
    @PostMapping("bugTrend")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse chartBugTrend(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId, @RequestBody ChartDataRequest request
    )
            throws Exception {
        checkHasProjectRead(projectId);
        return success(projectSummaryService.chartBugTrend(projectId, request));
    }

    @ApiOperation("工作负载图表的数据")
    @PostMapping("cardTrend")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse chartCardTrend(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId,
                                       @RequestBody ChartDataRequest request)
            throws Exception {
        checkHasProjectRead(projectId);
        return success(projectSummaryService.chartCardTrend(projectId, request.getRange(), request.getQueries()));
    }

    @ApiOperation("任务完成情况分布及完成效率散点图")
    @PostMapping("cardScatter")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse chartCardScatter(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId,
                                         @RequestBody ChartDataRequest request)
            throws Exception {
        checkHasProjectRead(projectId);
        return success(projectSummaryService.chartCardScatter(projectId, request.getRange(), request.getQueries()));
    }

    @ApiOperation("活跃任务分布")
    @PostMapping("activeCards")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse chartActiveCards(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId,
                                         @RequestParam(required = false) boolean containsNoPlan,
                                         @RequestBody List<Query> queries)
            throws Exception {
        checkHasProjectRead(projectId);
        return success(projectSummaryService.chartActiveCards(projectId, containsNoPlan, queries));
    }

    @ApiOperation("完成任务分布")
    @PostMapping("endCards")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse chartEndCards(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId,
                                      @RequestParam(required = false) boolean containsNotActive,
                                      @RequestBody ChartDataRequest request)
            throws Exception {
        checkHasProjectRead(projectId);
        return success(projectSummaryService.chartEndCards(projectId, containsNotActive, request.getRange(), request.getQueries()));
    }

    @ApiOperation("代码库统计数据")
    @GetMapping("relateRepoOverview")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<Object> relateRepoOverview(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId,
                                                   @RequestParam int year,
                                                   @ApiParam(value = "类型来自于code:如：QUARTER, MONTH, WEEK", example = "QUARTER") @RequestParam Dimension dimension,
                                                   @RequestParam int value) {
        checkHasProjectRead(projectId);
        List<ProjectRepo> repos = projectRepoService.selectByProjectId(projectId);
        if (CollectionUtils.isEmpty(repos)) {
            return success(null);
        }
        return success(repoStatService.projectOverview(
                companyService.currentCompany(),
                companyService.currentCompanyName(),
                repos.stream().map(ProjectRepo::getRepoId).collect(Collectors.toList()),
                year, dimension, value,
                userService.currentUserName()
        ));
    }


    @ApiOperation("构建的统计数据")
    @GetMapping("relateRepoBuildOverview")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<Object> relateRepoBuildOverview(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId,
                                                        @RequestParam Long year,
                                                        @ApiParam(value = "类型来自于pipeline:如：QUARTER, MONTH, WEEK", example = "QUARTER") @RequestParam String dimension,
                                                        @RequestParam Long value) {
        checkHasProjectRead(projectId);
        List<ProjectRepo> repos = projectRepoService.selectByProjectId(projectId);
        if (CollectionUtils.isEmpty(repos)) {
            return success(null);
        }
        return success(ezPipelineCliService.relateRepoBuildOverview(
                companyService.currentCompany(),
                repos.stream().map(ProjectRepo::getRepoId).collect(Collectors.toList()),
                year, dimension, value));
    }

    @ApiOperation("查询右侧分类卡片统计图表的数据")
    @GetMapping("cardCountByType")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<Object> cardCountByType(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId) throws Exception {
        checkHasProjectRead(projectId);
        return success(projectSummaryService.chartCardCountByType(projectId));
    }

    @ApiOperation("查询项目计划的统计数据")
    @PostMapping("planData")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse planData(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId,
                                 @RequestBody DateRange range)
            throws IOException {
        checkHasProjectRead(projectId);
        return success(projectSummaryService.planData(companyService.currentCompany(), projectId, range));
    }
}
