package com.ezone.ezproject.modules.comment.web;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.es.entity.CardComment;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.card.service.CardQueryService;
import com.ezone.ezproject.modules.comment.bean.CreateCommentRequest;
import com.ezone.ezproject.modules.comment.bean.UpdateCommentRequest;
import com.ezone.ezproject.modules.comment.service.CardCommentService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.io.IOException;
import java.util.List;

@ApiOperation("项目卡片模版")
@RestController
@RequestMapping("/project/card/{cardId:[0-9]+}")
@Slf4j
@AllArgsConstructor
public class CardCommentController extends AbstractController {
    private CardCommentService cardCommentService;

    private CardQueryService cardQueryService;

    @ApiOperation("新建卡片评论")
    @PostMapping("comment")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<CardComment> create(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                                            @ApiParam(value = "评论内容") @Size(min = 1) @Valid @RequestBody CreateCommentRequest request)
            throws IOException {
        Card card = cardQueryService.select(cardId);
        checkPermission(card.getProjectId(), OperationType.CARD_COMMENT);
        return success(cardCommentService.create(card, request.getComment(), request.getAtUsers()));
    }

    @ApiOperation("新建卡片评论回复")
    @PostMapping("comment/{commentId:[0-9]+}/reply")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<CardComment> reply(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                                           @ApiParam(value = "被回复评论ID", example = "1") @PathVariable Long commentId,
                                           @ApiParam(value = "回复内容") @Size(min = 1) @RequestBody CreateCommentRequest request)
            throws IOException {
        Card card = cardQueryService.select(cardId);
        checkPermission(card.getProjectId(), OperationType.CARD_COMMENT);
        return success(cardCommentService.reply(card, commentId, request.getComment(), request.getAtUsers()));
    }

    @ApiOperation("更新卡片评论")
    @PutMapping("comment/{commentId:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<CardComment> update(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                                            @ApiParam(value = "评论ID", example = "1") @PathVariable Long commentId,
                                            @ApiParam(value = "评论内容") @Size(min = 1) @RequestBody UpdateCommentRequest request)
            throws IOException {
        Card card = cardQueryService.select(cardId);
        checkPermission(card.getProjectId(), OperationType.CARD_COMMENT);
        return success(cardCommentService.update(card, commentId, request.getComment(), request.getAtUsers()));
    }

    @ApiOperation("查询卡片评论")
    @GetMapping("comment")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<CardComment>> select(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId)
            throws IOException {
        Card card = cardQueryService.select(cardId);
        checkHasProjectRead(card.getProjectId());
        return success(cardCommentService.selectByCardId(cardId));
    }

    @ApiOperation("删除卡片评论")
    @DeleteMapping("comment/{commentId:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<CardComment> delete(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                                            @ApiParam(value = "评论ID", example = "1") @PathVariable Long commentId)
            throws IOException {
        Card card = cardQueryService.select(cardId);
        checkPermission(card.getProjectId(), OperationType.CARD_COMMENT);
        return success(cardCommentService.setDeleted(commentId));
    }

}
