package com.ezone.ezproject.modules.attachment.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.storage.EzbaseStorageHelper;
import com.ezone.ezproject.dal.entity.Attachment;
import com.ezone.ezproject.modules.attachment.service.CardAttachmentQueryService;
import com.ezone.ezproject.modules.card.controller.AbstractCardController;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;

@ApiOperation("卡片附件操作")
@RestController
@RequestMapping("/project/proxy/card/{cardId:[0-9]+}/attachment")
@Slf4j
@AllArgsConstructor
public class ProxyCardAttachmentController extends AbstractCardController {
    private CardAttachmentQueryService cardAttachmentQueryService;
    private EzbaseStorageHelper ezbaseStorageHelper;

    @ApiOperation("预览附件")
    @GetMapping(value = "{attachmentId}/view/{fileName}")
    @CheckAuthType(TokenAuthType.READ)
    public ResponseEntity viewAttachment(
            @ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
            @ApiParam(value = "附件ID", example = "1") @PathVariable Long attachmentId,
            @RequestParam(required = false) boolean inner,
            @ApiParam(value = "仅用于可读性", example = "1") @PathVariable String fileName) throws IOException {
        if (!cardAttachmentQueryService.hasRel(cardId, attachmentId)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "附件和卡片不匹配!");
        }
        Long projectId = getProjectId(cardId);
        checkHasProjectRead(projectId);
        Attachment attachment = cardAttachmentQueryService.select(attachmentId);
        return ResponseEntity
                .status(HttpStatus.TEMPORARY_REDIRECT)
                // 自动编码遇空格异常，自己编码
                .location(ezbaseUri(String.format("/internal/file/view?filePath=%s&inner=%b", ezbaseStorageHelper.encodedPath(attachment.getStoragePath()), inner)))
                .build();
    }

    @ApiOperation("预览附件")
    @GetMapping(value = "/view/attachment/{fileName}")
    @CheckAuthType(TokenAuthType.READ)
    public ResponseEntity viewCardAttachment(
            @ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
            @RequestParam(required = false) boolean inner,
            @ApiParam(value = "附件文件名", example = "1") @PathVariable String fileName) throws IOException {
        Long projectId = getProjectId(cardId);
        checkHasProjectRead(projectId);
        Attachment attachment = cardAttachmentQueryService.select(cardId, fileName);
        return ResponseEntity
                .status(HttpStatus.TEMPORARY_REDIRECT)
                .location(ezbaseUri(String.format("/internal/file/view?filePath=%s&inner=%b", ezbaseStorageHelper.encodedPath(attachment.getStoragePath()), inner)))
                .build();
    }

    private URI ezbaseUri(String relativeUri) throws CodedException {
        try {
            return new URI(String.format("http://ezdoc%s", relativeUri));
        } catch (Exception e) {
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
