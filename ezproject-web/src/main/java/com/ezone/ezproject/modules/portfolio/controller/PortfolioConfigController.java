package com.ezone.ezproject.modules.portfolio.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.es.entity.PortfolioConfig;
import com.ezone.ezproject.es.entity.enums.PortfolioOperationType;
import com.ezone.ezproject.modules.portfolio.service.PortfolioConfigService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@ApiOperation("项目集配置")
@RestController
@RequestMapping("/portfolio/{portfolioId:[0-9]+}/config")
@Slf4j
@AllArgsConstructor
public class PortfolioConfigController extends AbstractController {

    private PortfolioConfigService portfolioConfigService;

    @ApiOperation("更新项目集报表配置")
    @PutMapping
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse updateConfig(@ApiParam(value = "项目集ID", example = "1") @PathVariable Long portfolioId,
                                     @ApiParam(value = "报表是否包含子项目集项目", example = "false") @RequestParam boolean chartContainDescendant)
            throws IOException {
        checkHasPortfolioChartCreateOrUpdate(portfolioId);
        portfolioConfigService.saveOrUpdate(portfolioId, PortfolioConfig.builder().chartContainDescendant(chartContainDescendant).build());
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("获取配置")
    @GetMapping()
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<PortfolioConfig> getConfig(@ApiParam(value = "项目集ID", example = "1") @PathVariable Long portfolioId)
            throws IOException {
        checkHasPortfolioRead(portfolioId);
        return success(portfolioConfigService.getConfig(portfolioId));
    }


    private void checkHasPortfolioChartCreateOrUpdate(Long portfolioId) {
        userPortfolioPermissionsService.checkPermissionAnyOps(userService.currentUserName(), portfolioId, PortfolioOperationType.REPORT_CREATE, PortfolioOperationType.REPORT_UPDATE);
    }

}