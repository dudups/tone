package com.ezone.ezproject.modules.plan.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.es.entity.PlanSummary;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import com.ezone.ezproject.modules.plan.service.PlanSummaryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Size;
import java.io.IOException;

@ApiOperation("计划回顾纪要")
@RestController
@RequestMapping("/project/plan/{planId:[0-9]+}")
@Slf4j
@AllArgsConstructor
public class PlanSummaryController extends AbstractController {
    private PlanQueryService planQueryService;

    private PlanSummaryService planSummaryService;

    @ApiOperation("保存回顾纪要")
    @PutMapping("summary")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse save(@ApiParam(value = "计划ID", example = "1") @PathVariable Long planId,
                                         @ApiParam(value = "回顾纪要内容") @Size(min = 1) @RequestBody String content)
            throws IOException {
        Plan plan = planQueryService.select(planId);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        checkPermission(plan.getProjectId(), OperationType.PLAN_UPDATE);
        planSummaryService.saveOrUpdate(planId, content);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询计划回顾纪要")
    @GetMapping("summary")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<PlanSummary> selectByPlanId(@ApiParam(value = "计划ID", example = "1") @PathVariable Long planId)
            throws IOException {
        Plan plan = planQueryService.select(planId);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(plan.getProjectId());
        return success(planSummaryService.find(planId));
    }
}
