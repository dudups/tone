package com.ezone.ezproject.modules.company.controller;

import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.ez.context.SystemUserService;
import com.ezone.ezproject.modules.card.bean.CardBean;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.card.service.CardReferenceValueHelper;
import com.ezone.ezproject.modules.card.service.CardSearchService;
import com.ezone.ezproject.modules.company.service.CompanySummaryService;
import com.ezone.ezproject.modules.project.bean.ChartDataRequest;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import com.ezone.galaxy.framework.common.bean.PageResult;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@ApiOperation("公司概览")
@RestController
@RequestMapping("/project/company/{companyId:[0-9]+}/summary")
@Slf4j
@AllArgsConstructor
public class CompanySummaryController extends AbstractController {
    private CompanySummaryService companySummaryService;

    private CardSearchService cardSearchService;

    private CardReferenceValueHelper cardReferenceValueHelper;

    private SystemUserService systemUserService;

    @ApiOperation("查询公司下卡片")
    @PostMapping("searchCard")
    public BaseResponse<TotalBean<CardBean>> searchCard(@ApiParam(value = "公司ID", example = "1") @PathVariable Long companyId,
                                                        @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
                                                        @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                                        @RequestBody SearchEsRequest searchCardRequest,
                                                        HttpServletResponse response) throws IOException {
        systemUserService.checkSystemUser();
        TotalBean<CardBean> totalBean = cardSearchService.searchByCompany(companyId, searchCardRequest, false, pageNumber, pageSize);
        cardReferenceValueHelper.tryLoadProjectSchemas(totalBean.getRefs(), searchCardRequest.getFields());
        cardReferenceValueHelper.tryLoadProjectSchemas(totalBean.getRefs());
        response.addHeader(PageResult.PAGE_TOTAL_COUNT, String.valueOf(totalBean.getTotal()));
        return success(totalBean);
    }

    @ApiOperation("查询公司概览对应图表的数据")
    @PostMapping("bugTrend")
    public BaseResponse chartBugTrend(@ApiParam(value = "公司ID", example = "1") @PathVariable Long companyId,
                                      @RequestBody ChartDataRequest request)
            throws Exception {
        systemUserService.checkSystemUser();
        return success(companySummaryService.chartBugTrend(companyId, request));
    }

    @ApiOperation("查询公司概览对应图表的数据")
    @PostMapping("cardTrend")
    public BaseResponse chartCardTrend(@ApiParam(value = "公司ID", example = "1") @PathVariable Long companyId,
                                       @RequestBody ChartDataRequest request)
            throws Exception {
        systemUserService.checkSystemUser();
        return success(companySummaryService.chartCardTrend(companyId, request.getRange(), request.getQueries()));
    }

    @ApiOperation("查询公司概览对应图表的数据")
    @PostMapping("cardScatter")
    public BaseResponse chartCardScatter(@ApiParam(value = "公司ID", example = "1") @PathVariable Long companyId,
                                         @RequestBody ChartDataRequest request)
            throws Exception {
        systemUserService.checkSystemUser();
        return success(companySummaryService.chartCardScatter(companyId, request.getRange(), request.getQueries()));
    }
}
