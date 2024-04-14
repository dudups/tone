package com.ezone.ezproject.modules.card.controller;

import com.ezone.devops.ezcode.base.enums.CardRelateType;
import com.ezone.devops.ezcode.sdk.bean.model.InternalBranch;
import com.ezone.devops.ezcode.sdk.bean.request.InternalAddBranchRequest;
import com.ezone.devops.ezcode.sdk.service.InternalBranchService;
import com.ezone.devops.ezcode.sdk.service.InternalCardService;
import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.modules.card.bean.AddBranchRequest;
import com.ezone.ezproject.modules.card.bean.BindBranchRequest;
import com.ezone.ezproject.modules.project.service.ProjectRepoService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@ApiOperation("卡片与代码分支关连")
@RestController
@RequestMapping("/project/card")
@Slf4j
@AllArgsConstructor
public class CardCodeBranchController extends AbstractCardController {

    private InternalBranchService internalBranchService;

    private InternalCardService internalCardService;

    private ProjectRepoService projectRepoService;

    @ApiOperation("查询卡片关联的代码信息")
    @GetMapping(value = "{projectKey:.+}-{seqNum:[0-9]+}/relateCodeMsg")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse relateCodeMsg(@ApiParam(value = "项目标示", example = "p1") @PathVariable String projectKey,
                                      @ApiParam(value = "卡片编号", example = "1") @PathVariable Long seqNum,
                                      @ApiParam(value = "类型来自于code:如：PUSH, COMMIT, REVIEW", example = "PUSH") @RequestParam String msgType) {
        Card card = cardQueryService.select(companyService.currentCompany(), projectKey, seqNum);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(card.getProjectId());
        String cardKey = projectKey + "-" + seqNum;
        BaseResponse response = internalCardService.listCardRelateResources(companyService.currentCompany(), CardRelateType.valueOf(msgType), cardKey, userService.currentUserName());
        checkCodeResponse(response);
        return response;
    }

    private void checkCodeResponse(BaseResponse response) {
        if (null == response) {
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, "请求code服务异常！");
        }
        if (response.isError()) {
            throw new CodedException(response.getCode(), response.getMessage());
        }
    }


    @ApiOperation("新建分支并绑定卡片")
    @PutMapping(value = "{projectKey:.+}-{seqNum:[0-9]+}/creadAndBindCodeBranch", produces = MediaType.APPLICATION_JSON_VALUE)
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse createAndBindCodeBranch(@PathVariable String projectKey,
                                                @ApiParam(value = "卡片编号", example = "1") @PathVariable Long seqNum,
                                                @ApiParam(value = "创建分支请求") @RequestBody AddBranchRequest request) {
        //判断是否本项目关联的代序库
        projectRepoService.checkIsBindRepo(request.getProjectId(), request.getRepoId());
        Card card = cardQueryService.select(companyService.currentCompany(), projectKey, seqNum);
        checkCanUpdateCard(request.getProjectId(), card.getId());
        InternalAddBranchRequest addBranchRequest = new InternalAddBranchRequest();
        addBranchRequest.setCompanyId(companyService.currentCompany());
        addBranchRequest.setRepoId(request.getRepoId());
        addBranchRequest.setCardKey(projectKey + "-" + seqNum);
        addBranchRequest.setOperator(userService.currentUserName());
        addBranchRequest.setName(request.getName());
        addBranchRequest.setBaseRefName(request.getBaseRefName());
        addBranchRequest.setBaseRefType(request.getRefType());
        addBranchRequest.setNote(request.getNote());
        return internalBranchService.addBranch(addBranchRequest);
    }

    @ApiOperation("获取卡片能绑定的分支列表")
    @PostMapping(value = "listBranches", produces = MediaType.APPLICATION_JSON_VALUE)
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse listBranches(
            @ApiParam(value = "代码库ID", example = "1") @RequestParam Long reportId,
            @ApiParam(value = "过滤字符串") @RequestParam String query) {
        return success(internalBranchService.listBranches(companyService.currentCompany(), reportId, query));
    }

    @ApiOperation("卡片绑定分支")
    @PutMapping(value = "{projectKey:.+}-{seqNum:[0-9]+}/bindCodeBranch", produces = MediaType.APPLICATION_JSON_VALUE)
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse bindCodeBranch(@PathVariable String projectKey,
                                       @ApiParam(value = "卡片编号", example = "1") @PathVariable Long seqNum,
                                       @ApiParam(value = "创建分支请求") @RequestBody BindBranchRequest request) {
        //判断是否本项目关联的代序库
        projectRepoService.checkIsBindRepo(request.getProjectId(), request.getRepoId());
        Card card = cardQueryService.select(companyService.currentCompany(), projectKey, seqNum);
        checkCanUpdateCard(request.getProjectId(), card.getId());
        return internalCardService.bindCard(companyService.currentCompany(), request.getRepoId(), request.getRelateKey(),
                request.getRefType(), projectKey + "-" + seqNum, userService.currentUserName());
    }

    @ApiOperation("卡片解绑分支")
    @PutMapping(value = "{projectKey:.+}-{seqNum:[0-9]+}/unbindCodeBranch", produces = MediaType.APPLICATION_JSON_VALUE)
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse unbindCodeBranch(@PathVariable String projectKey,
                                         @ApiParam(value = "卡片编号", example = "1") @PathVariable Long seqNum,
                                         @ApiParam(value = "创建分支请求") @RequestBody BindBranchRequest request) {
        //判断是否本项目关联的代序库
        projectRepoService.checkIsBindRepo(request.getProjectId(), request.getRepoId());
        Card card = cardQueryService.select(companyService.currentCompany(), projectKey, seqNum);
        checkCanUpdateCard(request.getProjectId(), card.getId());
        return internalCardService.unbindBranchCard(companyService.currentCompany(), request.getRepoId(), request.getRelateKey(),
                projectKey + "-" + seqNum, userService.currentUserName());
    }
}
