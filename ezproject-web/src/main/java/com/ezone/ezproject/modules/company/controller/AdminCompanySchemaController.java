package com.ezone.ezproject.modules.company.controller;

import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.ez.context.SystemUserService;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@ApiOperation("公司概览")
@RestController
@RequestMapping("/project/company/{companyId:[0-9]+}/schema")
@Slf4j
@AllArgsConstructor
public class AdminCompanySchemaController extends AbstractController {

    private CompanyProjectSchemaQueryService companyProjectSchemaQueryService;
    private SystemUserService systemUserService;

    @ApiOperation("查询当前公司级别卡片定义信息")
    @GetMapping("card/public")
    public BaseResponse<CompanyCardSchema> getCompanyCardSchemaPublic(@ApiParam(value = "企业", example = "1") @PathVariable Long companyId) {
        systemUserService.checkSystemUser();
        return success(companyProjectSchemaQueryService.getCompanyCardSchema(companyId));
    }
}
