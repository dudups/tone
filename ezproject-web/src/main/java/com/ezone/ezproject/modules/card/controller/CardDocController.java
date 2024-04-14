package com.ezone.ezproject.modules.card.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.CardDocRel;
import com.ezone.ezproject.modules.card.service.CardDocService;
import com.ezone.ezproject.modules.card.service.CardQueryService;
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

@ApiOperation("card关联doc")
@RestController
@RequestMapping("/project/card/{cardId:[0-9]+}/doc")
@Slf4j
@AllArgsConstructor
public class CardDocController extends AbstractCardController {
    private CardQueryService cardQueryService;
    private CardDocService cardDocService;

    @ApiOperation("添加关联doc")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<CardDocRel> bind(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                                         @ApiParam(value = "doc的所属空间ID") @NotNull @RequestParam Long spaceId,
                                         @ApiParam(value = "docID") @NotNull @RequestParam Long docId) {
        Card card = cardQueryService.select(cardId);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkCanUpdateCard(card.getProjectId(), cardId);
        return success(cardDocService.bind(cardId, spaceId, docId));
    }

    @ApiOperation("获取关联doc")
    @GetMapping
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<RelatesBean<CardDocRel>> select(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId) {
        Card card = cardQueryService.select(cardId);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(card.getProjectId());
        return success(cardDocService.selectRelatesBean(cardId));
    }

    @ApiOperation("移除关联doc")
    @DeleteMapping("{docId:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse unbind(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                               @ApiParam(value = "doc space ID", example = "1") @RequestParam Long docSpaceId,
                               @ApiParam(value = "doc ID", example = "1") @PathVariable Long docId) {
        Card card = cardQueryService.select(cardId);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(card.getProjectId());
        cardDocService.unBind(cardId, docSpaceId, docId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("更新卡片关联的doc")
    @PutMapping
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse updateBind(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                                   @RequestParam Long spaceId,
                                   @RequestBody List<Long> docIds) {
        Card card = cardQueryService.select(cardId);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkCanUpdateCard(card.getProjectId(), cardId);
        cardDocService.updateBind(cardId, spaceId, docIds);
        return SUCCESS_RESPONSE;
    }

}
