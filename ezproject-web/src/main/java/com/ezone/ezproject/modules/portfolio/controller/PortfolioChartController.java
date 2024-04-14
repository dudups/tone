package com.ezone.ezproject.modules.portfolio.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Portfolio;
import com.ezone.ezproject.dal.entity.PortfolioChart;
import com.ezone.ezproject.es.entity.enums.PortfolioOperationType;
import com.ezone.ezproject.modules.portfolio.bean.PortfolioChartInfo;
import com.ezone.ezproject.modules.portfolio.bean.PortfolioChartRequest;
import com.ezone.ezproject.modules.portfolio.service.PortfolioCharQueryService;
import com.ezone.ezproject.modules.portfolio.service.PortfolioChartCmdService;
import com.ezone.ezproject.modules.portfolio.service.PortfolioChartDataService;
import com.ezone.ezproject.modules.portfolio.service.PortfolioQueryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@ApiOperation("项目集操作")
@RestController
@RequestMapping("/portfolio/{portfolioId:[0-9]+}/chart")
@Slf4j
@AllArgsConstructor
public class PortfolioChartController extends AbstractController {

    private PortfolioChartCmdService chartCmdService;
    private PortfolioCharQueryService chartQueryService;
    private PortfolioQueryService portfolioQueryService;
    private PortfolioChartDataService chartDataService;

    @ApiOperation("新建")
    @PostMapping
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<PortfolioChart> createChart(@ApiParam(value = "项目集ID", example = "1") @PathVariable Long portfolioId,
                                                    @ApiParam(value = "报表", example = "1") @RequestBody PortfolioChartRequest request)
            throws IOException {
        checkHasPortfolioChartCreate(portfolioId);
        return success(chartCmdService.createChart(getPortfolio(portfolioId), request));
    }

    @ApiOperation("更新")
    @PutMapping("/{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<PortfolioChart> updateChart(@ApiParam(value = "项目集ID", example = "1") @PathVariable Long portfolioId,
                                                    @ApiParam(value = "报表ID", example = "1") @PathVariable Long id,
                                                    @ApiParam(value = "报表", example = "1") @RequestBody PortfolioChartRequest request)
            throws IOException {
        checkHasPortfolioChartUpdate(portfolioId);
        return success(chartCmdService.updateChart(portfolioId, id, request));
    }

    @ApiOperation("移动调整顺序")
    @PutMapping("/{id:[0-9]+}/move")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<PortfolioChart> updateChart(@ApiParam(value = "项目集ID", example = "1") @PathVariable Long portfolioId,
                                                    @ApiParam(value = "项目集ID", example = "1") @PathVariable Long id,
                                                    @ApiParam(value = "相对报表ID", example = "1") @RequestParam(required = false, defaultValue = "-1") Long afterId) {
        checkHasPortfolioChartUpdate(portfolioId);
        return success(chartCmdService.moveChart(portfolioId, id, afterId));
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse deleteChart(@ApiParam(value = "项目集ID", example = "1") @PathVariable Long portfolioId,
                                    @ApiParam(value = "项目集ID", example = "1") @PathVariable Long id)
            throws IOException {
        checkHasPortfolioChartDelete(portfolioId);
        chartCmdService.deleteChart(portfolioId, id);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("获取报表列表")
    @GetMapping
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<PortfolioChart>> listGroupChart(@ApiParam(value = "项目集ID", example = "1") @PathVariable Long portfolioId) {
        checkHasPortfolioRead(portfolioId);
        return success(chartQueryService.selectChartByPortfolioId(portfolioId));
    }


    @ApiOperation("获取报表详细配置")
    @GetMapping("/{id:[0-9]+}/detail")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<PortfolioChartInfo> getChartInfo(@ApiParam(value = "项目集ID", example = "1") @PathVariable Long portfolioId,
                                                         @ApiParam(value = "项目集ID", example = "1") @PathVariable Long id)
            throws IOException {
        checkHasPortfolioRead(portfolioId);
        return success(chartQueryService.selectChartInfo(portfolioId, id));
    }

    @ApiOperation("获取报表数据，不同报表类型返回不同结果")
    @PostMapping("/{id:[0-9]+}/data")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<Object> getChartData(@ApiParam(value = "项目集ID", example = "1") @PathVariable Long portfolioId,
                                             @ApiParam(value = "项目集ID", example = "1") @PathVariable Long id,
                                             @ApiParam(value = "覆盖默认配置的配置项") @RequestBody Map<String, Object> overwriteConfig)
            throws Exception {
        checkHasPortfolioRead(portfolioId);
        return success(chartDataService.chartData(portfolioId, id, overwriteConfig));
    }

    private void checkHasPortfolioChartCreate(Long portfolioId) {
        checkHasPortfolioPermission(portfolioId, PortfolioOperationType.REPORT_CREATE);
    }

    private void checkHasPortfolioChartUpdate(Long portfolioId) {
        checkHasPortfolioPermission(portfolioId, PortfolioOperationType.REPORT_UPDATE);
    }

    private void checkHasPortfolioChartDelete(Long portfolioId) {
        checkHasPortfolioPermission(portfolioId, PortfolioOperationType.REPORT_DELETE);
    }

    private Portfolio getPortfolio(Long portfolioId) {
        Portfolio portfolio = portfolioQueryService.select(portfolioId);
        if (portfolio == null) {
            throw new CodedException(HttpStatus.NOT_FOUND, "未找到项目集");
        }
        return portfolio;
    }

}