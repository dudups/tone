package com.ezone.ezproject.modules.card.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.CardTestPlan;
import com.ezone.ezproject.modules.card.service.CardQueryService;
import com.ezone.ezproject.modules.card.service.CardTestPlanService;
import com.ezone.ezproject.modules.project.bean.RelatesBean;
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

import javax.validation.constraints.NotNull;
import java.util.List;

@ApiOperation("项目关联测试执行计划")
@RestController
@RequestMapping("/project/card/{cardId:[0-9]+}/testPlan")
@Slf4j
@AllArgsConstructor
public class CardTestPlanController extends AbstractCardController {
    private CardQueryService cardQueryService;
    private CardTestPlanService cardTestPlanService;

    @ApiOperation("添加关联测试执行计划")
    @PostMapping
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<CardTestPlan> bind(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                                           @ApiParam(value = "测试执行计划的所属空间ID") @NotNull @RequestParam Long spaceId,
                                           @ApiParam(value = "测试执行计划ID") @NotNull @RequestParam Long planId) {
        Card card = cardQueryService.select(cardId);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(card.getProjectId());
        return success(cardTestPlanService.bind(cardId, spaceId, planId));
    }

    @ApiOperation("获取关联测试执行计划")
    @GetMapping
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<RelatesBean<CardTestPlan>> select(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId) {
        Card card = cardQueryService.select(cardId);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(card.getProjectId());
        return success(cardTestPlanService.selectRelatesBean(cardId));
    }

    @ApiOperation("移除关联测试执行计划")
    @DeleteMapping("{planId:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse unbind(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                               @ApiParam(value = "测试计划ID", example = "1") @PathVariable Long planId) {
        Card card = cardQueryService.select(cardId);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(card.getProjectId());
        cardTestPlanService.unBind(cardId, planId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("更新卡片关联的测试用例")
    @PutMapping
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse updateBind(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                                   @RequestParam Long spaceId,
                                   @RequestBody List<Long> planIds) {
        Card card = cardQueryService.select(cardId);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkCanUpdateCard(card.getProjectId(), cardId);
        cardTestPlanService.updateBind(cardId, spaceId, planIds);
        return SUCCESS_RESPONSE;
    }
}
