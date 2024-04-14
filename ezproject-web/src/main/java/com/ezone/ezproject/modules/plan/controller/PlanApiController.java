package com.ezone.ezproject.modules.plan.controller;

import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.modules.common.InternalApiAuthentication;
import com.ezone.ezproject.modules.plan.bean.ProjectPlan;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@ApiOperation("计划")
@RestController
@RequestMapping("/project/api/plan")
@Slf4j
@AllArgsConstructor
public class PlanApiController extends AbstractController {
    private PlanQueryService planQueryService;

    @ApiOperation("检查权限")
    @GetMapping("{id:[0-9]+}/checkExist")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<Boolean> checkExist(@ApiParam(value = "计划ID", example = "1") @PathVariable Long id,
                                            @ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId) {
        Plan plan = planQueryService.select(id);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        if (!plan.getProjectId().equals(projectId)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "计划不在项目下!");
        }
        return success(true);
    }

    @ApiOperation("检查权限")
    @PostMapping("{id:[0-9]+}/checkExistAndInProjects")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<Boolean> checkExistAndInProjects(@ApiParam(value = "计划ID", example = "1") @PathVariable Long id,
                                                         @ApiParam(value = "项目ID", example = "1") @RequestBody List<Long> projectIds) {
        Plan plan = planQueryService.select(id);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        if (projectIds == null || !projectIds.contains(plan.getProjectId())) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "计划不在项目下!");
        }
        return success(true);
    }

    @ApiOperation("检查权限")
    @GetMapping("{id:[0-9]+}/checkRead")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<Boolean> checkRead(@ApiParam(value = "计划ID", example = "1") @PathVariable Long id,
                                           @ApiParam(value = "用户名", example = "1") @RequestParam String user) {
        Plan plan = planQueryService.select(id);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        return success(permissionService.isRole(user, plan.getProjectId()));
    }

    @ApiOperation("检查权限")
    @GetMapping("{id:[0-9]+}/checkWrite")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<Boolean> checkWrite(@ApiParam(value = "计划ID", example = "1") @PathVariable Long id,
                                            @ApiParam(value = "用户名", example = "1") @RequestParam String user) {
        Plan plan = planQueryService.select(id);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        return success(permissionService.isMember(user, plan.getProjectId()));
    }

    @ApiOperation("查询计划")
    @GetMapping("{id:[0-9]+}")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<ProjectPlan> getProjectPlan(@ApiParam(value = "计划ID", example = "1") @PathVariable Long id) {
        return success(planQueryService.selectProjectPlan(id));
    }

    @ApiOperation("查询活跃的项目计划")
    @GetMapping("active")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<List<Plan>> selectActive(@RequestParam Long projectId) {
        return success(planQueryService.selectByProjectId(projectId, true));
    }
}
