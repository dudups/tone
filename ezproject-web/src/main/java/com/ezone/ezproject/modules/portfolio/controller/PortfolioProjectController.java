package com.ezone.ezproject.modules.portfolio.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.modules.card.bean.CardBean;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.card.service.CardSearchService;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import com.ezone.ezproject.modules.portfolio.bean.PortfolioProjectPlan;
import com.ezone.ezproject.modules.portfolio.bean.TotalPortfolioProjectSummary;
import com.ezone.ezproject.modules.portfolio.service.RelPortfolioProjectService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import com.ezone.galaxy.framework.common.bean.PageResult;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@ApiOperation("项目集操作")
@RestController
@RequestMapping("/portfolio/{portfolioId:[0-9]+}/project")
@Slf4j
@AllArgsConstructor
@Validated
public class PortfolioProjectController extends AbstractController {

    private RelPortfolioProjectService portfolioProjectService;
    private CardSearchService cardSearchService;
    private PlanQueryService planQueryService;
    private ProjectSchemaQueryService schemaQueryService;

    @ApiOperation("添加项目集关联项目（覆盖更新）")
    @PutMapping()
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse updateBindProjects(@ApiParam(value = "项目集ID", example = "1") @PathVariable Long portfolioId,
                            @ApiParam(value = "项目ID", example = "1") @RequestParam List<Long> projectIds) throws IOException {
        checkHasPortfolioManager(portfolioId);
        portfolioProjectService.updateBindProjects(portfolioId, projectIds);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("移除项目集关联项目")
    @DeleteMapping("{projectId:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse removeProject(@ApiParam(value = "项目集ID", example = "1") @PathVariable Long portfolioId,
                                      @ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId) throws IOException {
        checkHasPortfolioManager(portfolioId);
        portfolioProjectService.removeProject(portfolioId, projectId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("获取项目集关联的项目及项目统计数据")
    @GetMapping("list")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<TotalPortfolioProjectSummary> list(@ApiParam(value = "项目集ID", example = "1") @PathVariable Long portfolioId,
                                                           @RequestParam(defaultValue = "true") Boolean containSubPortfolio,  @RequestParam(defaultValue = "false") Boolean excludeNoPlan) throws IOException {
        checkHasPortfolioRead(portfolioId);
        return success(portfolioProjectService.listProjectAndSummary(portfolioId, containSubPortfolio, excludeNoPlan));
    }

    @ApiOperation("查询项目计划下卡片")
    @PostMapping("searchByPlan")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<TotalBean<CardBean>> searchByPlan(
            @ApiParam(value = "项目集ID", example = "1") @PathVariable Long portfolioId,
            @ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId,
            @ApiParam(value = "计划ID，查未计划卡片传0", example = "1") @RequestParam(required = false, defaultValue = "0") Long planId,
            @ApiParam(value = "是否包含子孙计划") @RequestParam(required = false) boolean containsDescendantPlan,
            @RequestBody SearchEsRequest searchCardRequest,
            HttpServletResponse response) throws IOException {
        checkHasPortfolioRead(portfolioId);
        checkProjectInPortfolio(portfolioId, projectId);
        TotalBean<CardBean> totalBean;
        if (planId.equals(0L)) {
            totalBean = cardSearchService.searchNoPlan(projectId, searchCardRequest);
        } else {
            Plan plan = planQueryService.select(planId);
            if (null == plan) {
                throw CodedException.NOT_FOUND;
            }
            if (!plan.getProjectId().equals(projectId)) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "计划不属于项目!");
            }
            totalBean = cardSearchService.search(plan, containsDescendantPlan, searchCardRequest);
        }
        response.addHeader(PageResult.PAGE_TOTAL_COUNT, String.valueOf(totalBean.getTotal()));
        return success(totalBean);
    }

    @ApiOperation("查询项目集下某项目的计划进度")
    @GetMapping("projectActivePlanProgress")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<PortfolioProjectPlan> projectActivePlanProgress(@ApiParam(value = "项目集ID", example = "1") @PathVariable Long portfolioId,
                                                                        @RequestParam Long projectId) throws IOException {
        checkHasPortfolioRead(portfolioId);
        checkProjectInPortfolio(portfolioId, projectId);
        return success(portfolioProjectService.getProjectActivePlansProgress(projectId));
    }


    @ApiOperation("获取项目卡片Schema")
    @GetMapping("{projectId:[0-9]+}/schema")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<ProjectCardSchema> getProjectCardSchema(@PathVariable Long portfolioId,@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId)
            throws IOException {
        checkHasPortfolioRead(portfolioId);
        checkProjectInPortfolio(portfolioId, projectId);
        return success(schemaQueryService.getProjectCardSchema(projectId));
    }

    private void checkProjectInPortfolio(Long portfolioId, Long projectId) {
        List<Long> projectIds = portfolioProjectService.queryRelationProjectIds(portfolioId, true);
        if (!projectIds.contains(projectId)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "项目ID不在当前项目集！");
        }
    }
}