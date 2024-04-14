package com.ezone.ezproject.modules.project.controller;

import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.modules.chart.ezinsight.config.EzInsightChart;
import com.ezone.ezproject.modules.chart.ezinsight.service.EzInsightDataService;
import com.ezone.ezproject.modules.common.InternalApiAuthentication;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@ApiOperation("公司级内部接口")
@RestController
@RequestMapping("/project/api/chart")
@Slf4j
@AllArgsConstructor
public class ChartApiController extends AbstractController {
    private EzInsightDataService ezInsightDataService;

    @ApiOperation("统计报表结果数据")
    @PostMapping("data")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse companyEndCardCount(
            @ApiParam(value = "insight报表组的类型：COMPANY，PROJECT_SET", example = "1") @RequestParam String chartGroupType,
            @ApiParam(value = "公司ID", example = "1") @RequestParam Long companyId,
            @ApiParam(value = "项目IDs", example = "1") @RequestParam(required = false) List<Long> projectIds,
            @Valid @RequestBody EzInsightChart chart) throws Exception {
        return success(ezInsightDataService.chartData(chartGroupType, companyId, projectIds, chart));
    }
}
