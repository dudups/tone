package com.ezone.ezproject.modules.attachment.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.storage.IStorage;
import com.ezone.ezproject.dal.entity.Attachment;
import com.ezone.ezproject.dal.entity.AttachmentExample;
import com.ezone.ezproject.dal.entity.CardAttachmentRel;
import com.ezone.ezproject.dal.entity.CardAttachmentRelExample;
import com.ezone.ezproject.dal.mapper.AttachmentMapper;
import com.ezone.ezproject.dal.mapper.CardAttachmentRelMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class CardAttachmentQueryService {
    private AttachmentMapper attachmentMapper;

    private CardAttachmentRelMapper cardAttachmentRelMapper;

    private IStorage storage;

    public boolean hasRel(Long cardId, Long attachmentId) {
        CardAttachmentRelExample relExample = new CardAttachmentRelExample();
        relExample.createCriteria().andCardIdEqualTo(cardId).andAttachmentIdEqualTo(attachmentId);
        List<CardAttachmentRel> rels =cardAttachmentRelMapper.selectByExample(relExample);
        return CollectionUtils.isNotEmpty(rels);
    }

    public List<Attachment> listAttachment(Long cardId) {
        CardAttachmentRelExample relExample = new CardAttachmentRelExample();
        relExample.createCriteria().andCardIdEqualTo(cardId);
        List<CardAttachmentRel> rels =cardAttachmentRelMapper.selectByExample(relExample);
        if (CollectionUtils.isEmpty(rels)) {
            return ListUtils.EMPTY_LIST;
        }
        List<Long> ids = rels.stream().map(CardAttachmentRel::getAttachmentId).collect(Collectors.toList());
        AttachmentExample attachmentExample = new AttachmentExample();
        attachmentExample.createCriteria().andIdIn(ids);
        return attachmentMapper.selectByExample(attachmentExample);
    }

    public Attachment select(Long attachmentId) {
        return attachmentMapper.selectByPrimaryKey(attachmentId);
    }

    public Attachment select(Long cardId, String fileName) {
        CardAttachmentRelExample relExample = new CardAttachmentRelExample();
        relExample.createCriteria().andCardIdEqualTo(cardId);
        List<CardAttachmentRel> rels =cardAttachmentRelMapper.selectByExample(relExample);
        if (CollectionUtils.isEmpty(rels)) {
            return null;
        }
        List<Long> ids = rels.stream().map(CardAttachmentRel::getAttachmentId).collect(Collectors.toList());
        AttachmentExample attachmentExample = new AttachmentExample();
        attachmentExample.createCriteria().andIdIn(ids).andFileNameEqualTo(fileName);
        List<Attachment> attachments = attachmentMapper.selectByExample(attachmentExample);
        return CollectionUtils.isEmpty(attachments) ? null : attachments.get(0);
    }

    public ResponseEntity download(Long attachmentId) {
        Attachment attachment = attachmentMapper.selectByPrimaryKey(attachmentId);
        if (null == attachment) {
            throw CodedException.NOT_FOUND;
        }
        String fileName = attachment.getFileName();
        String storagePath = attachment.getStoragePath();
        return response(fileName, storage.open(storagePath));
    }

    public ResponseEntity preview(Long cardId, String fileName) {
        Attachment attachment = select(cardId, fileName);
        return preview(attachment);
    }

    public ResponseEntity preview(Long attachmentId) {
        Attachment attachment = attachmentMapper.selectByPrimaryKey(attachmentId);
        return preview(attachment);
    }

    private ResponseEntity response(String fileName, InputStream content) {
        HttpHeaders headers = new HttpHeaders();
        try {
            // URLEncoder按w3c要求，空格转+，而RFC则包括空格等字符统一编码%XX格式，故需修正处理一下
            headers.setContentDispositionFormData("attachment", encodeAsUri(fileName));
        } catch (UnsupportedEncodingException e) {
            headers.setContentDispositionFormData("attachment", fileName);
        }
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        InputStreamResource inputStreamResource = new InputStreamResource(content);
        return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
    }

    private ResponseEntity preview(Attachment attachment) {
        if (null == attachment) {
            throw CodedException.NOT_FOUND;
        }
        String fileName = attachment.getFileName();
        InputStream content = storage.open(attachment.getStoragePath());
        String contentType = attachment.getContentType();
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.startsWith(contentType, MediaType.TEXT_HTML_VALUE)) {
            contentType = MediaType.TEXT_PLAIN_VALUE;
        }
        try {
            headers.set(HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=%s", encodeAsUri(fileName)));
            headers.setContentType(MediaType.parseMediaType(contentType));
        } catch (UnsupportedEncodingException e) {
            headers.set(HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=%s", fileName));
            headers.setContentType(MediaType.parseMediaType(contentType));
        } catch (Exception e) {
            try {
                content.close();
            } catch (Exception ce) {
                log.error("close content stream exception!", ce);
            }
            throw e;
        }
        InputStreamResource inputStreamResource = new InputStreamResource(content);
        return ResponseEntity.ok()
                .eTag(String.valueOf(attachment.getUploadTime().getTime()))
                .headers(headers)
                .body(inputStreamResource);
    }

    private static String encodeAsUri(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, "UTF-8").replaceAll("\\+", "%20");
    }

}