package com.ezone.ezproject.modules.portfolio.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.limit.incr.CompanyIncrLimit;
import com.ezone.ezproject.dal.entity.Portfolio;
import com.ezone.ezproject.es.entity.UserPortfolioPermissions;
import com.ezone.ezproject.modules.portfolio.bean.CreatePortfolioRequest;
import com.ezone.ezproject.modules.portfolio.bean.EditPortfolioRequest;
import com.ezone.ezproject.modules.portfolio.bean.SearchScope;
import com.ezone.ezproject.modules.portfolio.bean.TotalPortfolioInfo;
import com.ezone.ezproject.modules.portfolio.service.CompanyPortfolioDailyLimiter;
import com.ezone.ezproject.modules.portfolio.service.PortfolioCmdService;
import com.ezone.ezproject.modules.portfolio.service.PortfolioFavouriteCmdService;
import com.ezone.ezproject.modules.portfolio.service.PortfolioQueryService;
import com.ezone.ezproject.modules.portfolio.service.RelPortfolioProjectService;
import com.ezone.ezproject.modules.portfolio.service.UserPortfolioPermissionsService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@ApiOperation("项目集操作")
@RestController
@RequestMapping("/portfolio")
@Slf4j
@AllArgsConstructor
public class PortfolioController extends AbstractController {
    private PortfolioCmdService portfolioCmdService;
    private PortfolioQueryService portfolioQueryService;
    private RelPortfolioProjectService portfolioProjectService;
    private UserPortfolioPermissionsService portfolioPermissionsService;
    private PortfolioFavouriteCmdService portfolioFavouriteCmdService;

    @ApiOperation("新建项目集")
    @PostMapping
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    @CompanyIncrLimit(domainResourceKey = CompanyPortfolioDailyLimiter.DOMAIN_RESOURCE_KEY)
    public BaseResponse create(@Valid @RequestBody CreatePortfolioRequest createPortfolioRequest) {
        checkCreatePortfolio();
        portfolioCmdService.checkPortfolioNames(createPortfolioRequest);
        return success(portfolioCmdService.create(createPortfolioRequest));
    }

    @ApiOperation("获取项目集及祖先节点")
    @GetMapping("{id:[0-9]+}/getAncestor")
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    @CompanyIncrLimit(domainResourceKey = CompanyPortfolioDailyLimiter.DOMAIN_RESOURCE_KEY)
    public BaseResponse getAncestor(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) {
        checkHasPortfolioRead(id);
        Portfolio portfolio = portfolioQueryService.select(id);
        List<Portfolio> ancestor = portfolioQueryService.selectAncestor(portfolio);
        ancestor.add(portfolio);
        return success(ancestor);
    }

    @ApiOperation("项目集列表")
    @GetMapping("list")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<TotalPortfolioInfo> list(@RequestParam String q, @RequestParam(required = false, defaultValue = "ALL") SearchScope scope,
                                                 @ApiParam(value = "是否显示完整的父级树-包含没有权限的父级节点") @RequestParam(required = false, defaultValue = "false") boolean showCompleteTree) {
        return success(portfolioProjectService.listPortfolio(q, scope, showCompleteTree));
    }

    @ApiOperation("删除项目集")
    @DeleteMapping("{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse delete(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) {
        checkHasPortfolioManager(id);
        portfolioCmdService.delete(id);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("编辑项目集")
    @PutMapping("{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse update(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                               @Valid @RequestBody EditPortfolioRequest editPortfolioRequest) {
        checkHasPortfolioManager(id);
        portfolioCmdService.edit(id, editPortfolioRequest);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("项目集收藏")
    @PutMapping("{id:[0-9]+}/favourite")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse favouriteProject(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) {
        checkHasPortfolioRead(id);
        portfolioFavouriteCmdService.favouritePortfolio(userService.currentUserName(), id);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("取消收藏")
    @DeleteMapping("{id:[0-9]+}/favourite")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse unFavouriteProject(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) {
        checkHasPortfolioRead(id);
        portfolioFavouriteCmdService.unFavouritePortfolio(userService.currentUserName(), id);
        return SUCCESS_RESPONSE;
    }


    @ApiOperation("查询项目集操作权限")
    @GetMapping("{id:[0-9]+}/permissions")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<UserPortfolioPermissions> selectPermissions(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id) throws IOException {
        return success(portfolioPermissionsService.permissions(id));
    }

}