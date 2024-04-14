package com.ezone.ezproject.modules.plan.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.card.rank.RankLocation;
import com.ezone.ezproject.modules.chart.service.PlanDataService;
import com.ezone.ezproject.modules.plan.bean.CreatePlanRequest;
import com.ezone.ezproject.modules.plan.bean.CreatePlanTreeRequest;
import com.ezone.ezproject.modules.plan.bean.PlanMilestone;
import com.ezone.ezproject.modules.plan.bean.PlansAndProgresses;
import com.ezone.ezproject.modules.plan.bean.UpdatePlanRequest;
import com.ezone.ezproject.modules.plan.service.PlanCmdService;
import com.ezone.ezproject.modules.plan.service.PlanGoalService;
import com.ezone.ezproject.modules.plan.service.PlanNoticeService;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import com.ezone.galaxy.framework.common.bean.PageResult;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApiOperation("计划")
@RestController
@RequestMapping("/project/plan")
@Slf4j
@AllArgsConstructor
public class PlanController extends AbstractController {
    private PlanQueryService planQueryService;

    private PlanCmdService planCmdService;

    private PlanGoalService planGoalService;

    private PlanNoticeService planNoticeService;

    private PlanDataService planDataService;

    @ApiOperation("新建计划")
    @PostMapping
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<Plan> create(@Valid @RequestBody CreatePlanRequest createPlanRequest) throws IOException {
        checkPermission(createPlanRequest.getProjectId(), OperationType.PLAN_CREATE);
        return success(planCmdService.create(createPlanRequest));
    }

    @ApiOperation("新建计划")
    @PostMapping("tree")
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<Plan> create(@Valid @RequestBody CreatePlanTreeRequest createPlanTreeRequest) throws IOException {
        checkPermission(createPlanTreeRequest.getProjectId(), OperationType.PLAN_CREATE);
        return success(planCmdService.create(createPlanTreeRequest));
    }

    @ApiOperation("更新计划")
    @PutMapping("{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<Plan> update(@ApiParam(value = "计划ID", example = "1") @PathVariable Long id,
                                     @Valid @RequestBody UpdatePlanRequest updatePlanRequest) throws IOException {
        Plan plan = planQueryService.select(id);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        checkPermission(plan.getProjectId(), OperationType.PLAN_UPDATE);
        return success(planCmdService.update(id, updatePlanRequest));
    }

    @ApiOperation("查询计划")
    @GetMapping("{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<Plan> select(@ApiParam(value = "计划ID", example = "1") @PathVariable Long id) {
        Plan plan = planQueryService.select(id);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(plan.getProjectId());
        return success(plan);
    }

    @ApiOperation("查询活跃的项目计划")
    @GetMapping("active")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<Plan>> selectActive(@RequestParam(value = "projectId") Long projectId) {
        checkHasProjectRead(projectId);
        return success(planQueryService.selectByProjectId(projectId, true));
    }

    @ApiOperation("查询活跃的项目计划")
    @GetMapping("activeWithProgress")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<PlansAndProgresses> selectActiveWithProgress(@RequestParam(value = "projectId") Long projectId) throws Exception {
        checkHasProjectRead(projectId);
        List<Plan> plans = planQueryService.selectByProjectId(projectId, true);
        Map<Long, PlansAndProgresses.Progress> progresses = planDataService.planProgress(plans.stream().map(Plan::getId).collect(Collectors.toList()));
        return success(PlansAndProgresses.builder()
                .plans(plans)
                .progresses(plans.stream().map(plan -> progresses.get(plan.getId())).collect(Collectors.toList()))
                .build()
                .includeDescendantProgress());
    }

    @ApiOperation("查询指定的项目计划")
    @PostMapping("plansProgress")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<Map<Long, PlansAndProgresses.Progress>> plansProgress(@RequestParam(value = "projectId") Long projectId, @RequestBody List<Long> planIds) throws Exception {
        checkHasProjectRead(projectId);
        List<Plan> plans = planQueryService.select(projectId, planIds);
        Map<Long, PlansAndProgresses.Progress> result = new HashMap<>();
        if (CollectionUtils.isEmpty(plans)) {
            return success(result);
        }
        Map<Long, PlansAndProgresses.Progress> progresses = planDataService.planProgress(plans.stream().map(Plan::getId).collect(Collectors.toList()));
        return success(progresses);
    }

    @ApiOperation("查询指定的项目计划")
    @PostMapping("selectPlans")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<Plan>> selectActive(@RequestParam(value = "projectId") Long projectId, @RequestBody List<Long> planIds) {
        checkHasProjectRead(projectId);
        return success(planQueryService.select(projectId, planIds));
    }

    @ApiOperation("查询项目计划")
    @GetMapping("searchAll")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<Plan>> searchAll(@RequestParam(value = "projectId") Long projectId,
                                                   @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
                                                   @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                                   @RequestParam(required = false, defaultValue = "") String q,
                                                   HttpServletResponse response) {
        checkHasProjectRead(projectId);
        TotalBean<Plan> totalBean = planQueryService.searchAll(projectId, q, pageNumber, pageSize);
        response.addHeader(PageResult.PAGE_TOTAL_COUNT, String.valueOf(totalBean.getTotal()));
        return success(totalBean.getList());
    }

    @ApiOperation("查询已归档项目计划")
    @GetMapping("inactive")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<Plan>> selectInactive(@RequestParam(value = "projectId") Long projectId,
                                                   @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
                                                   @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                                   @RequestParam(required = false, defaultValue = "") String q,
                                                   HttpServletResponse response) {
        checkHasProjectRead(projectId);
        TotalBean<Plan> totalBean = planQueryService.searchInactive(projectId, q, pageNumber, pageSize);
        response.addHeader(PageResult.PAGE_TOTAL_COUNT, String.valueOf(totalBean.getTotal()));
        return success(totalBean.getList());
    }

    @ApiOperation("查询项目计划里程碑，按结束时间倒序")
    @GetMapping("milestone")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<PlanMilestone>> milestones(@RequestParam(value = "projectId") Long projectId,
                                                        @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
                                                        @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                                        HttpServletResponse response) {
        checkHasProjectRead(projectId);
        TotalBean<PlanMilestone> milestones = planGoalService.milestone(projectId, pageNumber, pageSize);
        response.addHeader(PageResult.PAGE_TOTAL_COUNT, String.valueOf(milestones.getTotal()));
        return success(milestones.getList());
    }

    @ApiOperation("归档")
    @PutMapping("{id:[0-9]+}/inactive")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse inactive(@ApiParam(value = "计划ID", example = "1") @PathVariable Long id,
                                 @ApiParam(value = "计划内卡片迁移到哪个卡片下", example = "1") @RequestParam(required = false, defaultValue = "0") Long targetPlanId)
            throws IOException {
        Plan plan = planQueryService.select(id);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        checkPermission(plan.getProjectId(), OperationType.PLAN_INACTIVE);
        planCmdService.inactive(plan, targetPlanId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("取消归档")
    @PutMapping("{id:[0-9]+}/active")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse active(@ApiParam(value = "计划ID", example = "1") @PathVariable Long id) throws IOException {
        Plan plan = planQueryService.select(id);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        checkPermission(plan.getProjectId(), OperationType.PLAN_RECOVERY);
        plan.setIsActive(true);
        planCmdService.active(plan);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("更新卡片交付线")
    @PutMapping("{id:[0-9]+}/updateDeliverLineRank")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<String> rankByPlanDeliverLineRank(@ApiParam(value = "计划ID", example = "1") @PathVariable Long id,
                                                          @NotNull @ApiParam(value = "参照卡片", example = "1") @RequestParam Long referenceCardId,
                                                          @NotNull @ApiParam(value = "计划交付线排序位置更高/更低") @RequestParam RankLocation location) {
        Plan plan = planQueryService.select(id);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        checkPermission(plan.getProjectId(), OperationType.PLAN_UPDATE);
        return success(planCmdService.updateDeliverLineRank(plan, referenceCardId, location));
    }

    @ApiOperation("删除")
    @DeleteMapping("{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse delete(@ApiParam(value = "计划ID", example = "1") @PathVariable Long id,
                               @ApiParam(value = "计划内卡片迁移到哪个卡片下", example = "1") @RequestParam(required = false, defaultValue = "0") Long targetPlanId)
            throws IOException {
        Plan plan = planQueryService.select(id);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        checkPermission(plan.getProjectId(), OperationType.PLAN_DELETE);
        planCmdService.delete(plan, targetPlanId);
        return SUCCESS_RESPONSE;
    }
}
