package com.ezone.ezproject.modules.card.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.modules.card.service.CardCmdService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@ApiOperation("卡片")
@RestController
@RequestMapping("/project/card/{cardId:[0-9]+}")
@Slf4j
@AllArgsConstructor
public class CardWatchController extends AbstractCardController {
    private CardCmdService cardCmdService;

    @ApiOperation("是否订阅卡片")
    @GetMapping("watch")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<Boolean> isWatch(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId) throws IOException {
        return success(cardQueryService.isWatch(cardId));
    }

    @ApiOperation("订阅卡片")
    @PostMapping("watch")
    @ResponseStatus(HttpStatus.CREATED)
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse watch(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId) throws IOException {
        Card card = cardQueryService.select(cardId);
        checkHasProjectRead(card.getProjectId());
        cardCmdService.setWatch(cardId, true);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("取消订阅卡片")
    @DeleteMapping("watch")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse unWatch(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId) throws IOException {
        Card card = cardQueryService.select(cardId);
        checkHasProjectRead(card.getProjectId());
        cardCmdService.setWatch(cardId, false);
        return SUCCESS_RESPONSE;
    }

}
