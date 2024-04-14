package com.ezone.ezproject.modules.portfolio.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.modules.portfolio.service.PortfolioMemberCmdService;
import com.ezone.ezproject.modules.portfolio.service.PortfolioMemberQueryService;
import com.ezone.ezproject.modules.project.bean.MemberBean;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@ApiOperation("项目成员操作")
@RestController
@RequestMapping("/portfolio/{id:[0-9]+}/member")
@Slf4j
@AllArgsConstructor
@Validated
public class PortfolioMemberController extends AbstractController {
    private PortfolioMemberQueryService portfolioMemberQueryService;

    private PortfolioMemberCmdService portfolioMemberCmdService;


    @ApiOperation("查询项目成员")
    @GetMapping
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<MemberBean>> select(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) {
        checkHasPortfolioRead(id);
        return success(portfolioMemberQueryService.select(id).stream().map(MemberBean::from).collect(Collectors.toList()));
    }

    @ApiOperation("设置项目成员")
    @PutMapping
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse setMembers(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                   @RequestBody @Valid List<MemberBean> members) throws IOException {
        checkHasPortfolioManager(id);
        portfolioMemberCmdService.setPortfolioMembers(id, members);
        return SUCCESS_RESPONSE;
    }

}
