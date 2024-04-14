package com.ezone.ezproject.modules.chart.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.ProjectChart;
import com.ezone.ezproject.dal.entity.ProjectChartGroup;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.chart.bean.ChartGroupRequest;
import com.ezone.ezproject.modules.chart.bean.ChartRequest;
import com.ezone.ezproject.modules.chart.config.Chart;
import com.ezone.ezproject.modules.chart.config.enums.MetricType;
import com.ezone.ezproject.modules.chart.config.range.DateRange;
import com.ezone.ezproject.modules.chart.data.GanttChart;
import com.ezone.ezproject.modules.chart.enums.ChartGroupType;
import com.ezone.ezproject.modules.chart.service.ChartCmdService;
import com.ezone.ezproject.modules.chart.service.ChartDataService;
import com.ezone.ezproject.modules.chart.service.ChartQueryService;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ApiOperation("卡片")
@RestController
@RequestMapping("/project/chart")
@Slf4j
@AllArgsConstructor
public class ChartController extends AbstractController {
    private ChartCmdService chartCmdService;
    private ChartQueryService chartQueryService;
    private ChartDataService chartDataService;

    private PlanQueryService planQueryService;

    @ApiOperation("甘特图数据")
    @GetMapping("/gantt/data")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<GanttChart> ganttChart(@ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId)
            throws IOException {
        checkHasProjectRead(projectId);
        return success(chartDataService.ganttData(projectId));
    }

    @ApiOperation("计划燃烧进度数据")
    @PostMapping("/burn/data")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse planCardsProgressBurn(@ApiParam(value = "计划ID", example = "1") @RequestParam Long planId,
                                              @ApiParam(value = "是否包含子孙计划") @RequestParam(required = false) boolean containsDescendantPlan,
                                              @ApiParam(value = "统计方式") @RequestParam @NotNull MetricType metricType,
                                              @ApiParam(value = "统计字段") @RequestParam(required = false) String metricField,
                                              @RequestBody SearchEsRequest searchCardRequest)
            throws Exception {
        Plan plan = planQueryService.select(planId);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(plan.getProjectId());
        return success(chartDataService.planCardsProgressBurn(plan, containsDescendantPlan, searchCardRequest.getQueries(), metricField, metricType));
    }

    @ApiOperation("新建报表组")
    @PostMapping("group")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<ProjectChartGroup> createGroup(@ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId,
                                                       @ApiParam(value = "分组信息", example = "1") @RequestBody ChartGroupRequest request)
            throws IOException {
        checkCreatePermission(projectId, request.getType());
        return success(chartCmdService.createGroup(projectId, request));
    }

    @ApiOperation("复制报表组及组中报表")
    @PostMapping("group/{id:[0-9]+}/copy")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<ProjectChartGroup> copyGroupAndReport(@ApiParam(value = "报表ID", example = "1") @PathVariable Long id,
                                                               @ApiParam(value = "分组信息", example = "1") @RequestBody ChartGroupRequest request)
            throws IOException {
        ProjectChartGroup sourceGroup = chartQueryService.selectChartGroup(id);
        if (null == sourceGroup) {
            throw CodedException.NOT_FOUND;
        }
        checkCreatePermission(sourceGroup.getProjectId(), request.getType());
        return success(chartCmdService.copyGroupAndReport(sourceGroup, request));
    }

    @ApiOperation("更新")
    @PutMapping("group/{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<ProjectChartGroup> updateGroup(@ApiParam(value = "报表ID", example = "1") @PathVariable Long id,
                                                       @ApiParam(value = "分组信息", example = "1") @RequestBody ChartGroupRequest request) {
        ProjectChartGroup group = chartQueryService.selectChartGroup(id);
        if (null == group) {
            throw CodedException.NOT_FOUND;
        }
        checkUpdatePermission(group);
        return success(chartCmdService.updateGroup(id, request));
    }

    @ApiOperation("删除")
    @DeleteMapping("group/{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse deleteGroup(@ApiParam(value = "报表ID", example = "1") @PathVariable Long id)
            throws IOException {
        ProjectChartGroup group = chartQueryService.selectChartGroup(id);
        if (null == group) {
            throw CodedException.NOT_FOUND;
        }
        checkUpdatePermission(group);
        chartCmdService.deleteGroup(id);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("新建")
    @PostMapping
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<ProjectChart> createChart(@ApiParam(value = "分组ID", example = "1") @RequestParam Long groupId,
                                                  @ApiParam(value = "报表", example = "1") @RequestBody ChartRequest request)
            throws IOException {
        ProjectChartGroup group = chartQueryService.selectChartGroup(groupId);
        if (null == group) {
            throw CodedException.NOT_FOUND;
        }
        checkUpdatePermission(group);
        return success(chartCmdService.createChart(group, request));
    }

    @ApiOperation("更新")
    @PutMapping("/{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<ProjectChart> updateChart(@ApiParam(value = "报表ID", example = "1") @PathVariable Long id,
                                                  @ApiParam(value = "报表", example = "1") @RequestBody ChartRequest request)
            throws IOException {
        ProjectChart chart = chartQueryService.selectProjectChart(id);
        if (null == chart) {
            throw CodedException.NOT_FOUND;
        }
        checkUpdatePermission(chartQueryService.selectChartGroup(chart.getGroupId()));
        return success(chartCmdService.updateChart(id, request));
    }

    @ApiOperation("移动调整顺序")
    @PutMapping("/{id:[0-9]+}/move")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<ProjectChart> updateChart(@ApiParam(value = "报表ID", example = "1") @PathVariable Long id,
                                                  @ApiParam(value = "相对报表ID", example = "1") @RequestParam(required = false, defaultValue = "-1") Long afterId)
            throws IOException {
        ProjectChart chart = chartQueryService.selectProjectChart(id);
        if (null == chart) {
            throw CodedException.NOT_FOUND;
        }
        checkUpdatePermission(chartQueryService.selectChartGroup(chart.getGroupId()));
        return success(chartCmdService.moveChart(chart, afterId));
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse deleteChart(@ApiParam(value = "报表ID", example = "1") @PathVariable Long id)
            throws IOException {
        ProjectChart chart = chartQueryService.selectProjectChart(id);
        if (null == chart) {
            throw CodedException.NOT_FOUND;
        }
        checkDeletePermission(chartQueryService.selectChartGroup(chart.getGroupId()));
        chartCmdService.deleteChart(id);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("获取报表分组列表")
    @GetMapping("group")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<ProjectChartGroup>> listGroup(@ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId) {
        checkHasProjectRead(projectId);
        return success(chartQueryService.selectGroupForUser(projectId));
    }

    @ApiOperation("获取报表列表")
    @GetMapping("group/{groupId:[0-9]+}/chart")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<ProjectChart>> listGroupChart(@ApiParam(value = "项目ID", example = "1") @PathVariable Long groupId) {
        ProjectChartGroup group = chartQueryService.selectChartGroup(groupId);
        if (group == null) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(group.getProjectId());
        return success(chartQueryService.selectChartByGroupId(groupId));
    }

    @Deprecated
    @ApiOperation("获取报表列表")
    @GetMapping
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<ProjectChart>> listChart(@ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId) {
        checkHasProjectRead(projectId);
        return success(chartQueryService.selectByProjectId(projectId));
    }

    @ApiOperation("获取报表详细配置")
    @GetMapping("/{id:[0-9]+}/detail")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<Chart> get(@ApiParam(value = "报表ID", example = "1") @PathVariable Long id)
            throws IOException {
        ProjectChart chart = chartQueryService.selectProjectChart(id);
        if (null == chart) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(chart.getProjectId());
        return success(chartQueryService.selectChart(id));
    }

    @ApiOperation("获取报表数据，不同报表类型返回不同结果")
    @GetMapping("/{id:[0-9]+}/data")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<Object> data(@ApiParam(value = "报表ID", example = "1") @PathVariable Long id)
            throws Exception {
        ProjectChart chart = chartQueryService.selectProjectChart(id);
        if (null == chart) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(chart.getProjectId());
        return success(chartDataService.chartData(chart.getProjectId(), id));
    }

    @ApiOperation("获取已支持的报表类型列表")
    @GetMapping("types")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<String>> types() {
        return success(Arrays.stream(Chart.CONFIG_CLASSES)
                .map(c -> StringUtils.uncapitalize(c.getSimpleName()))
                .collect(Collectors.toList())
        );
    }

    @ApiOperation("查询用户新增bug数据")
    @PostMapping("userBug")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse userNewBug(@RequestParam String user, @RequestBody DateRange range)
            throws Exception {
        return success(chartDataService.userNewBug(user, companyService.currentCompany(), range));
    }

    @ApiOperation("查询用户完成卡片数据")
    @PostMapping("userEndCard")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse userEndCard(@RequestParam String user, @RequestBody DateRange range)
            throws Exception {
        return success(chartDataService.userEndCard(user, companyService.currentCompany(), range));
    }

    private void checkUpdatePermission(ProjectChartGroup group) {
        String user = userService.currentUserName();
        if (ChartGroupType.SHARE.name().equals(group.getType())) {
            checkPermission(group.getProjectId(), OperationType.CHART_UPDATE);
        } else {
            if (!user.equals(group.getCreateUser())) {
                throw CodedException.FORBIDDEN;
            }
        }
    }

    private void checkDeletePermission(ProjectChartGroup group) {
        String user = userService.currentUserName();
        if (ChartGroupType.SHARE.name().equals(group.getType())) {
            checkPermission(group.getProjectId(), OperationType.CHART_DELETE);
        } else {
            if (!user.equals(group.getCreateUser())) {
                throw CodedException.FORBIDDEN;
            }
        }
    }

    private void checkReadPermission(ProjectChartGroup group) {
        String user = userService.currentUserName();
        if (ChartGroupType.SHARE.name().equals(group.getType())) {
            checkHasProjectRead(group.getProjectId());
        } else {
            if (!user.equals(group.getCreateUser())) {
                throw CodedException.FORBIDDEN;
            }
        }
    }

    private void checkCreatePermission(Long projectId, ChartGroupType groupType) {
        checkPermission(projectId, OperationType.CHART_CREATE);
    }
}
