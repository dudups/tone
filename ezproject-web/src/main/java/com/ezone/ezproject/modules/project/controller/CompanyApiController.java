package com.ezone.ezproject.modules.project.controller;

import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.modules.common.InternalApiAuthentication;
import com.ezone.ezproject.modules.company.bean.CardStatData;
import com.ezone.ezproject.modules.company.service.CompanySummaryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@ApiOperation("公司级内部接口")
@RestController
@RequestMapping("/project/api/company")
@Slf4j
@AllArgsConstructor
public class CompanyApiController extends AbstractController {
    private CompanySummaryService companySummaryService;

    @ApiOperation("查询公司完成卡片数")
    @GetMapping("summary/endCardCount")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<List<CardStatData>> companyEndCardCount(@ApiParam(value = "公司IDs", example = "1") @RequestParam List<Long> companyIds,
                                                                @RequestParam long beginTime,
                                                                @RequestParam long endTime) throws IOException {
        return success(companySummaryService.companyEndCardCount(companyIds, new Date(beginTime), new Date(endTime)));
    }
}
