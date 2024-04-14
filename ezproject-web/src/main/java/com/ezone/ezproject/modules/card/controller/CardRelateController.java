package com.ezone.ezproject.modules.card.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.modules.card.service.CardRelateRelCmdService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class CardRelateController extends AbstractCardController {
    private CardRelateRelCmdService cardRelateRelCmdService;

    @ApiOperation("添加关联卡片")
    @PostMapping("relate/{relateCardId:[0-9]+}")
    @ResponseStatus(HttpStatus.CREATED)
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse create(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                               @ApiParam(value = "关联卡片ID", example = "1") @PathVariable Long relateCardId) throws IOException {
        Card card = cardQueryService.select(cardId);
        checkCanUpdateCard(card.getProjectId(), cardId);
        Card relateCard = cardQueryService.select(relateCardId);
        checkHasProjectRead(relateCard.getProjectId());
        cardRelateRelCmdService.create(card, relateCard);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("解除关联卡片")
    @DeleteMapping("relate/{relateCardId:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse delete(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                               @ApiParam(value = "关联卡片ID", example = "1") @PathVariable Long relateCardId) throws IOException {
        Card card = cardQueryService.select(cardId);
        checkCanUpdateCard(card.getProjectId(), cardId);
        cardRelateRelCmdService.delete(card, relateCardId);
        return SUCCESS_RESPONSE;
    }

}
