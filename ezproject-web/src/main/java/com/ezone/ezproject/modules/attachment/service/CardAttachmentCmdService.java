package com.ezone.ezproject.modules.attachment.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.storage.IStorage;
import com.ezone.ezproject.common.storage.StoragePathUtil;
import com.ezone.ezproject.dal.entity.Attachment;
import com.ezone.ezproject.dal.entity.AttachmentExample;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.CardAttachmentRel;
import com.ezone.ezproject.dal.entity.CardAttachmentRelExample;
import com.ezone.ezproject.dal.mapper.AttachmentMapper;
import com.ezone.ezproject.dal.mapper.CardAttachmentRelMapper;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.entity.CardDraft;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.bill.service.StorageBill;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.update.CardStatusReadOnlyChecker;
import com.ezone.ezproject.modules.event.EventDispatcher;
import com.ezone.ezproject.modules.event.events.AttachmentCreateEvent;
import com.ezone.ezproject.modules.event.events.AttachmentDeleteEvent;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.SetUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class CardAttachmentCmdService {
    private AttachmentMapper attachmentMapper;

    private CardAttachmentRelMapper cardAttachmentRelMapper;

    private CardDao cardDao;

    private IStorage storage;

    private CardAttachmentQueryService cardAttachmentQueryService;

    private ProjectQueryService projectQueryService;

    private UserService userService;

    private StorageBill storageBill;

    private EventDispatcher eventDispatcher;

    private ProjectSchemaQueryService projectSchemaQueryService;

    public Attachment upload(Card card, MultipartFile file, String description) {
        String user = userService.currentUserName();
        String fileName = file.getOriginalFilename();
        String filePath = filePath(card, fileName);
        Map<String, Object> cardDetail = cardDao.findAsMap(card.getId());
        checkCardStatusReadonly(cardDetail);
        try {
            Attachment duplicateNameAttachment = cardAttachmentQueryService.select(card.getId(), fileName);
            storage.save(filePath, file.getInputStream(), file.getSize());
            storageBill.updateBill(card.getCompanyId());
            Attachment attachment = Attachment.builder()
                    .id(IdUtil.generateId())
                    .fileName(fileName)
                    // todo 解析内容，前端实现则取file.getContentType()，后端则URLConnection.guessContentTypeFromStream(可重复读的内容流)
                    .contentType(file.getContentType())
                    .storagePath(filePath)
                    .description(StringUtils.defaultString(description))
                    .uploadTime(new Date())
                    .uploadUser(user)
                    .build();
            insert(card.getId(), attachment);
            if (duplicateNameAttachment != null) {
                delete(card.getCompanyId(), card.getId(), duplicateNameAttachment);
            }
            eventDispatcher.asyncDispatch(() -> AttachmentCreateEvent.builder()
                    .user(user)
                    .card(card)
                    .cardDetail(cardDetail)
                    .attachment(attachment)
                    .build());
            return attachment;
        } catch (IOException e) {
            log.error(String.format("Upload file:[%s] exception!", filePath), e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public Attachment upload(Long draftId, CardDraft draft, MultipartFile file, String description) {
        String user = userService.currentUserName();
        String fileName = file.getOriginalFilename();
        Long projectId = draft.getProjectId();
        Long companyId = projectQueryService.getProjectCompany(projectId);
        String filePath = filePath(companyId, projectId, draftId, fileName);
        try {
            Attachment duplicateNameAttachment = cardAttachmentQueryService.select(draftId, fileName);
            storage.save(filePath, file.getInputStream(), file.getSize());
            storageBill.updateBill(companyId);
            Attachment attachment = Attachment.builder()
                    .id(IdUtil.generateId())
                    .fileName(fileName)
                    .contentType(file.getContentType())
                    .storagePath(filePath)
                    .description(StringUtils.defaultString(description))
                    .uploadTime(new Date())
                    .uploadUser(user)
                    .build();
            insert(draftId, attachment);
            if (duplicateNameAttachment != null) {
                delete(companyId, draftId, duplicateNameAttachment);
            }
            return attachment;
        } catch (IOException e) {
            log.error(String.format("Upload file:[%s] exception!", filePath), e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public Attachment setDescription(Long attachmentId, String description) {
        Attachment attachment = attachmentMapper.selectByPrimaryKey(attachmentId);
        if (null == attachment) {
            throw CodedException.NOT_FOUND;
        }
        attachment.setDescription(StringUtils.defaultString(description));
        attachmentMapper.updateByPrimaryKey(attachment);
        return attachment;
    }

    // 20221209：draftId直接作为cardId，不需要调用本方法了
    @Deprecated
    public void commitDraftAttachment(Long fromDraftId, Long toCardId) {
        CardAttachmentRelExample example = new CardAttachmentRelExample();
        example.createCriteria().andCardIdEqualTo(fromDraftId);
        List<CardAttachmentRel> rels =cardAttachmentRelMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(rels)) {
            return;
        }
        rels.forEach(rel -> {
            rel.setCardId(toCardId);
            cardAttachmentRelMapper.updateByPrimaryKey(rel);
        });
    }

    @Transactional
    public void insert(Long cardId, Attachment attachment) {
        CardAttachmentRel rel = CardAttachmentRel.builder().id(IdUtil.generateId()).cardId(cardId).attachmentId(attachment.getId()).build();
        attachmentMapper.insert(attachment);
        cardAttachmentRelMapper.insert(rel);
    }

    @Transactional
    public void delete(Card card, Long attachmentId) {
        Map<String, Object> cardDetail = cardDao.findAsMap(card.getId());
        checkCardStatusReadonly(cardDetail);
        Attachment attachment = attachmentMapper.selectByPrimaryKey(attachmentId);
        delete(card.getCompanyId(), card.getId(), attachment);
        String user = userService.currentUserName();
        eventDispatcher.asyncDispatch(() -> AttachmentDeleteEvent.builder()
                .user(user)
                .card(card)
                .cardDetail(cardDetail)
                .attachment(attachment)
                .build());
    }

    @Transactional
    public void delete(Long draftId, CardDraft draft, Long attachmentId) {
        Attachment attachment = attachmentMapper.selectByPrimaryKey(attachmentId);
        Long projectId = draft.getProjectId();
        delete(projectQueryService.getProjectCompany(projectId), draftId, attachment);
    }

    @Transactional
    public void delete(Long draftId, CardDraft draft) {
        Long projectId = draft.getProjectId();
        delete(projectQueryService.getProjectCompany(projectId), Arrays.asList(draftId));
    }

    private void delete(Long companyId, Long cardId, Attachment attachment) {
        CardAttachmentRelExample example = new CardAttachmentRelExample();
        example.createCriteria().andAttachmentIdEqualTo(attachment.getId());
        List<CardAttachmentRel> rels =cardAttachmentRelMapper.selectByExample(example);
        Optional<CardAttachmentRel> optional = rels.stream().filter(rel -> cardId.equals(rel.getCardId())).findAny();
        if (optional.isPresent()) {
            cardAttachmentRelMapper.deleteByPrimaryKey(optional.get().getId());
            if (rels.size() == 1) {
                attachmentMapper.deleteByPrimaryKey(attachment.getId());
                storage.delete(attachment.getStoragePath());
                storageBill.updateBill(companyId);
            }
        }
    }

    /**
     * @param cardIds 卡片或草稿ID
     */
    @Transactional
    public void delete(Long company, List<Long> cardIds) {
        CardAttachmentRelExample example = new CardAttachmentRelExample();
        example.createCriteria().andCardIdIn(cardIds);
        List<CardAttachmentRel> rels = cardAttachmentRelMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(rels)) {
            return;
        }
        cardAttachmentRelMapper.deleteByExample(example);
        Set<Long> attachmentIds = rels.stream().map(CardAttachmentRel::getAttachmentId).collect(Collectors.toSet());
        example = new CardAttachmentRelExample();
        example.createCriteria().andAttachmentIdIn(new ArrayList<>(attachmentIds));
        rels = cardAttachmentRelMapper.selectByExample(example);
        Set<Long> retainAttachmentIds = SetUtils.EMPTY_SET;
        if (CollectionUtils.isNotEmpty(rels)) {
            retainAttachmentIds = rels.stream().map(CardAttachmentRel::getAttachmentId).collect(Collectors.toSet());
        }
        Collection<Long> delAttachmentIds = CollectionUtils.subtract(attachmentIds, retainAttachmentIds);
        if (CollectionUtils.isEmpty(delAttachmentIds)) {
            return;
        }
        AttachmentExample attachmentExample = new AttachmentExample();
        attachmentExample.createCriteria().andIdIn(new ArrayList<>(delAttachmentIds));
        List<Attachment> attachments = attachmentMapper.selectByExample(attachmentExample);
        if (CollectionUtils.isEmpty(attachments)) {
            return;
        }
        attachmentMapper.deleteByExample(attachmentExample);
        attachments.forEach(attachment -> storage.delete(attachment.getStoragePath()));
        storageBill.updateBill(company);
    }

    private String filePath(Card card, String fileName) {
        return filePath(card.getCompanyId(), card.getProjectId(), card.getId(), fileName);
    }

    private String filePath(Long companyId, Long projectId, Long cardId, String fileName) {
        return StoragePathUtil.join(
                String.valueOf(companyId),
                String.valueOf(projectId),
                String.valueOf(cardId),
                UUID.randomUUID().toString(),
                fileName);
    }

    private void checkCardStatusReadonly(Map<String, Object> cardDetail) {
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(FieldUtil.getProjectId(cardDetail));
        CardType cardType = schema.findCardType(FieldUtil.getType(cardDetail));
        CardType.StatusConf statusConf = cardType.findStatusConf(FieldUtil.getStatus(cardDetail));
        if (statusConf.isReadOnly()) {
           throw new CodedException(HttpStatus.FORBIDDEN, "此卡片当前状态为只读状态，不允许编辑");
        }
    }

}
