package com.ezone.ezproject.modules.attachment.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Attachment;
import com.ezone.ezproject.modules.attachment.service.CardAttachmentCmdService;
import com.ezone.ezproject.modules.attachment.service.CardAttachmentQueryService;
import com.ezone.ezproject.modules.card.controller.AbstractCardController;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ApiOperation("卡片附件操作")
@RestController
@RequestMapping("/project/card/{cardId:[0-9]+}/attachment")
@Slf4j
@AllArgsConstructor
public class CardAttachmentController extends AbstractCardController {
    private CardAttachmentCmdService cardAttachmentCmdService;
    private CardAttachmentQueryService cardAttachmentQueryService;

    @ApiOperation("上传单个附件")
    @PostMapping
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<Attachment> upload(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                                           @ApiParam(value = "附件描述/备注") @RequestParam(required = false, defaultValue = StringUtils.EMPTY) String description,
                                           @ApiParam(value = "附件文件") @RequestParam("file") MultipartFile file) throws IOException {
        return cardOrDraft(cardId,
                card -> {
                    checkCanUpdateCard(card.getProjectId(), cardId);
                    checkPlanIsActive(card);
                    return success(cardAttachmentCmdService.upload(card, file, description));
                },
                draft -> {
                    checkHasProjectRead(draft.getProjectId());
                    return success(cardAttachmentCmdService.upload(cardId, draft, file, description));
                });
    }

    @ApiOperation("上传多个附件")
    @PostMapping("/batch")
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<List<Attachment>> batchUpload(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                                                      @ApiParam(value = "附件文件") @RequestParam @NotNull MultipartFile[] files) {
        return cardOrDraft(cardId,
                card -> {
                    checkCanUpdateCard(card.getProjectId(), cardId);
                    checkPlanIsActive(card);
                    return success(IntStream.range(0, files.length)
                            .mapToObj(i -> cardAttachmentCmdService.upload(card, files[i], StringUtils.EMPTY))
                            .collect(Collectors.toList()));
                },
                draft -> {
                    checkHasProjectRead(draft.getProjectId());
                    return success(IntStream.range(0, files.length)
                            .mapToObj(i -> cardAttachmentCmdService.upload(cardId, draft, files[i], StringUtils.EMPTY))
                            .collect(Collectors.toList()));
                });
    }

    @ApiOperation("附件列表")
    @GetMapping
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<Attachment>> list(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId) throws IOException {
        Long projectId = getProjectId(cardId);
        checkHasProjectRead(projectId);
        return success(cardAttachmentQueryService.listAttachment(cardId));
    }

    @ApiOperation("下载附件")
    @GetMapping(value = "{attachmentId}")
    @CheckAuthType(TokenAuthType.READ)
    public ResponseEntity download(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                                   @ApiParam(value = "附件ID", example = "1") @PathVariable Long attachmentId) throws IOException {
        if (!cardAttachmentQueryService.hasRel(cardId, attachmentId)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "附件和卡片不匹配!");
        }
        Long projectId = getProjectId(cardId);
        checkHasProjectRead(projectId);
        return cardAttachmentQueryService.download(attachmentId);
    }

    @ApiOperation("预览附件")
    @GetMapping(value = "{attachmentId}/preview/{fileName}")
    @CheckAuthType(TokenAuthType.READ)
    public ResponseEntity preview(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                                  @ApiParam(value = "附件ID", example = "1") @PathVariable Long attachmentId,
                                  @ApiParam(value = "仅用于可读性", example = "1") @PathVariable String fileName) throws IOException {
        if (!cardAttachmentQueryService.hasRel(cardId, attachmentId)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "附件和卡片不匹配!");
        }
        Long projectId = getProjectId(cardId);
        checkHasProjectRead(projectId);
        return cardAttachmentQueryService.preview(attachmentId);
    }

    @ApiOperation("更新附件描述")
    @PutMapping(value = "{attachmentId}/description")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<Attachment> setDescription(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                                                   @ApiParam(value = "附件ID", example = "1") @PathVariable Long attachmentId,
                                                   @RequestBody(required = false) String description) throws IOException {
        if (!cardAttachmentQueryService.hasRel(cardId, attachmentId)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "附件和卡片不匹配!");
        }
        Long projectId = getProjectId(cardId);
        checkHasProjectRead(projectId);
        return success(cardAttachmentCmdService.setDescription(attachmentId, description));
    }

    @ApiOperation("预览附件")
    @GetMapping(value = "/preview/{fileName}")
    @CheckAuthType(TokenAuthType.READ)
    public ResponseEntity preview(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                                  @ApiParam(value = "附件文件名", example = "1") @PathVariable String fileName) throws IOException {
        Long projectId = getProjectId(cardId);
        checkHasProjectRead(projectId);
        return cardAttachmentQueryService.preview(cardId, fileName);
    }

    @ApiOperation("移除附件")
    @DeleteMapping("{attachmentId}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse delete(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                               @ApiParam(value = "附件ID", example = "1") @PathVariable Long attachmentId) throws IOException {
        if (!cardAttachmentQueryService.hasRel(cardId, attachmentId)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "附件和卡片不匹配!");
        }
        cardOrDraft(cardId,
                card -> {
                    checkPlanIsActive(card);
                    checkCanUpdateCard(card.getProjectId(), cardId);
                    cardAttachmentCmdService.delete(card, attachmentId);
                },
                draft -> {
                    checkHasProjectRead(draft.getProjectId());
                    cardAttachmentCmdService.delete(cardId, draft, attachmentId);
                });
        return SUCCESS_RESPONSE;
    }
}
