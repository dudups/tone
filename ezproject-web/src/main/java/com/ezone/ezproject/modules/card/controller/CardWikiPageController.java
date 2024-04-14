package com.ezone.ezproject.modules.card.controller;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.CardWikiPageRel;
import com.ezone.ezproject.modules.card.service.CardQueryService;
import com.ezone.ezproject.modules.card.service.CardWikiPageService;
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

@ApiOperation("card关联page")
@RestController
@RequestMapping("/project/card/{cardId:[0-9]+}/wikiPage")
@Slf4j
@AllArgsConstructor
public class CardWikiPageController extends AbstractCardController {
    private CardQueryService cardQueryService;
    private CardWikiPageService cardWikiPageService;

    @ApiOperation("添加关联page")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<CardWikiPageRel> bind(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                                              @ApiParam(value = "page的所属空间ID") @NotNull @RequestParam Long spaceId,
                                              @ApiParam(value = "pageID") @NotNull @RequestParam Long pageId) {
        Card card = cardQueryService.select(cardId);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkCanUpdateCard(card.getProjectId(), cardId);
        return success(cardWikiPageService.bind(cardId, spaceId, pageId));
    }

    @ApiOperation("获取关联page")
    @GetMapping
    public BaseResponse<RelatesBean<CardWikiPageRel>> select(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId) {
        Card card = cardQueryService.select(cardId);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(card.getProjectId());
        return success(cardWikiPageService.selectRelatesBean(cardId));
    }

    @ApiOperation("移除关联page")
    @DeleteMapping("{pageId:[0-9]+}")
    public BaseResponse unbind(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                               @ApiParam(value = "测试计划ID", example = "1") @PathVariable Long pageId) {
        Card card = cardQueryService.select(cardId);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(card.getProjectId());
        cardWikiPageService.unBind(cardId, pageId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("更新卡片关联的page")
    @PutMapping
    public BaseResponse updateBind(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                                   @RequestParam Long spaceId,
                                   @RequestBody List<Long> pageIds) {
        Card card = cardQueryService.select(cardId);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkCanUpdateCard(card.getProjectId(), cardId);
        cardWikiPageService.updateBind(cardId, spaceId, pageIds);
        return SUCCESS_RESPONSE;
    }
}
