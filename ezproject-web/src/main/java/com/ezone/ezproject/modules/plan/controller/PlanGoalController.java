package com.ezone.ezproject.modules.plan.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.PlanGoal;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.permission.PermissionService;
import com.ezone.ezproject.modules.plan.bean.PlanGoalRequest;
import com.ezone.ezproject.modules.plan.service.PlanGoalService;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApiOperation("计划里程碑目标")
@RestController
@RequestMapping("/project/plan/{planId:[0-9]+}")
@Slf4j
@AllArgsConstructor
public class PlanGoalController extends AbstractController {
    private PlanQueryService planQueryService;

    private PlanGoalService planGoalService;

    private PermissionService permissionService;

    private UserService userService;

    @ApiOperation("新建计划目标")
    @PostMapping("goal")
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<PlanGoal> create(@ApiParam(value = "计划ID", example = "1") @PathVariable Long planId,
                                         @ApiParam(value = "计划目标详情") @RequestBody PlanGoalRequest request) {
        Plan plan = planQueryService.select(planId);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        checkPermission(plan.getProjectId(), OperationType.PLAN_UPDATE);
        return success(planGoalService.create(planId, request));
    }

    @ApiOperation("更新计划目标")
    @PutMapping("goal/{goalId:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<PlanGoal> update(@ApiParam(value = "计划ID", example = "1") @PathVariable Long planId,
                                         @ApiParam(value = "计划目标ID", example = "1") @PathVariable Long goalId,
                                         @ApiParam(value = "计划目标详情") @RequestBody PlanGoalRequest request) {
        Plan plan = planQueryService.select(planId);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        checkPermission(plan.getProjectId(), OperationType.PLAN_UPDATE);
        return success(planGoalService.update(goalId, request));
    }

    @ApiOperation("删除计划目标")
    @DeleteMapping("goal/{goalId:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse update(@ApiParam(value = "计划ID", example = "1") @PathVariable Long planId,
                               @ApiParam(value = "计划目标ID", example = "1") @PathVariable Long goalId) {
        Plan plan = planQueryService.select(planId);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        checkPermission(plan.getProjectId(), OperationType.PLAN_UPDATE);
        planGoalService.delete(goalId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询计划目标")
    @GetMapping("goal")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<PlanGoal>> selectByPlanId(@ApiParam(value = "计划ID", example = "1") @PathVariable Long planId) {
        Plan plan = planQueryService.select(planId);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(plan.getProjectId());
        return success(planGoalService.selectByPlanId(planId));
    }

    @ApiOperation("查询计划目标：使用场景是查子计划的目标")
    @GetMapping("goal/descendant")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<Map<Long, List<PlanGoal>>> selectByAncestorPlanId(@ApiParam(value = "计划ID", example = "1") @PathVariable Long planId) {
        Plan plan = planQueryService.select(planId);
        String user = userService.currentUserName();
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(plan.getProjectId());
        List<Plan> descendants = planQueryService.selectDescendant(plan, null);
        if (CollectionUtils.isEmpty(descendants)) {
            return success(MapUtils.EMPTY_MAP);
        }
        return success(planGoalService.selectByPlanId(descendants.stream().map(Plan::getId).collect(Collectors.toList())));
    }
}
