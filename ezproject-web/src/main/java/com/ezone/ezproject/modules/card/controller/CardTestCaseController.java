package com.ezone.ezproject.modules.card.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.modules.cli.EzTestCliService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@ApiOperation("卡片")
@RestController
@RequestMapping("/project/card")
@Slf4j
@AllArgsConstructor
public class CardTestCaseController extends AbstractCardController {

    private EzTestCliService ezTestCliService;

    @ApiOperation("查询发现bug的测试用例执行记录")
    @GetMapping("{id:[0-9]+}/listBugBindCaseRuns")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse listBugBindCaseRuns(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long id) {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(card.getProjectId());
        return success(ezTestCliService.listBugBindCaseRuns(id));
    }

    @ApiOperation("解绑卡片关联的测试用例")
    @PostMapping("{id:[0-9]+}/unBindCases")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse unBindCases(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long id,
                                    @RequestBody List<Long> caseIds) {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkCanUpdateCard(card.getProjectId(), card.getId());
        ezTestCliService.unBindCases(id, caseIds);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询卡片关联的测试用例")
    @GetMapping("{id:[0-9]+}/listBindCaseIds")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<Long>> listBindCaseIds(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long id,
                                                    @RequestParam Long spaceId) {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(card.getProjectId());
        return success(ezTestCliService.listBindCaseIds(id, spaceId));
    }

    @ApiOperation("查询卡片关联的测试用例")
    @GetMapping("{id:[0-9]+}/listBindCases")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse listBindCases(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long id) {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(card.getProjectId());
        return success(ezTestCliService.listBindCases(id));
    }

    @ApiOperation("更新卡片关联的测试用例")
    @PutMapping("{id:[0-9]+}/updateBindCases")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse updateBindCases(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long id,
                                        @RequestParam Long spaceId,
                                        @RequestBody List<Long> caseIds) {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkCanUpdateCard(card.getProjectId(), card.getId());

        ezTestCliService.updateBindCases(id, spaceId, caseIds);
        return SUCCESS_RESPONSE;
    }
}
