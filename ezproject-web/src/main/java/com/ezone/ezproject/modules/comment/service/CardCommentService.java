package com.ezone.ezproject.modules.comment.service;

import com.ezone.ezbase.iam.bean.LoginUser;
import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.es.dao.CardCommentDao;
import com.ezone.ezproject.es.entity.CardComment;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.service.CardHelper;
import com.ezone.ezproject.modules.card.service.CardNoticeService;
import com.ezone.ezproject.modules.card.service.CardQueryService;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService;
import com.ezone.ezproject.modules.notice.NoticeService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class CardCommentService {
    private CardCommentDao cardCommentDao;
    private UserService userService;
    private CardHelper cardHelper;
    private CardNoticeService cardNoticeService;
    private CardQueryService cardQueryService;

    private NoticeService noticeService;
    private CompanyProjectSchemaQueryService companyProjectSchemaQueryService;
    private ProjectSchemaQueryService projectSchemaQueryService;

    public CardComment create(Card card, String comment, List<String> atUsers) throws IOException {
        LoginUser user = userService.currentUser();
        CardComment cardComment = CardComment.builder()
                .id(IdUtil.generateId())
                .cardId(card.getId())
                .seqNum(cardHelper.commentSeqNum(card.getId()))
                .maxSeqNum(0L)
                .user(user.getUsername())
                .comment(comment)
                .createTime(new Date())
                .build();
        cardCommentDao.saveOrUpdate(cardComment);
        noticeAtUsers(card, user);

        return cardComment;
    }

    public CardComment reply(Card card, Long parentId, String comment, List<String> atUsers) throws IOException {
        CardComment parentComment = cardCommentDao.find(parentId, CardComment.CARD_ID, CardComment.SEQ_NUM, CardComment.ANCESTOR_ID);
        if (parentComment == null) {
            throw new CodedException(HttpStatus.NOT_FOUND, "Parent comment not exist!");
        }
        if (!card.getId().equals(parentComment.getCardId())) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "Parent comment not match card!");
        }
        Long ancestorId = parentComment.getAncestorId() > 0 ? parentComment.getAncestorId() : parentId;
        LoginUser user = userService.currentUser();
        CardComment cardComment = CardComment.builder()
                .id(IdUtil.generateId())
                .cardId(parentComment.getCardId())
                .seqNum(cardHelper.descendantCommentSeqNum(ancestorId))
                .ancestorId(ancestorId)
                .parentId(parentId)
                .parentSeqNum(parentComment.getSeqNum())
                .user(user.getUsername())
                .comment(comment)
                .createTime(new Date())
                .build();
        cardCommentDao.saveOrUpdate(cardComment);
        noticeAtUsers(card, user);
        return cardComment;
    }

    public CardComment update(Card card, Long commentId, String comment, List<String> atUsers) throws IOException {
        LoginUser user = userService.currentUser();
        CardComment cardComment = select(commentId);
        if (!user.getUsername().equals(cardComment.getUser())) {
            throw CodedException.FORBIDDEN;
        }
        cardComment.setComment(comment);
        cardComment.setLastModifyTime(new Date());
        cardCommentDao.saveOrUpdate(cardComment);
        noticeAtUsers(card, user);
        return cardComment;
    }

    private void noticeAtUsers(Card card, LoginUser user) {
        Map<String, Object> cardDetail = cardQueryService.selectDetail(card.getId());
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(card.getCompanyId());
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(card.getProjectId());
        cardNoticeService.noticeAtUsersInCardComment(card, cardDetail, user, FieldUtil.getAtUsers(cardDetail), companyCardSchema, schema, ProjectNoticeConfig.Type.CREATE);
    }

    public CardComment setDeleted(Long id) throws IOException {
        String user = userService.currentUserName();
        CardComment cardComment = select(id);
        if (!user.equals(cardComment.getUser())) {
            throw CodedException.FORBIDDEN;
        }
        cardComment.setDeleted(true);
        cardComment.setComment(StringUtils.EMPTY);
        cardComment.setLastModifyTime(new Date());
        cardCommentDao.saveOrUpdate(cardComment);
        return cardComment;
    }

    public List<CardComment> selectByCardId(Long cardId) throws IOException {
        return cardCommentDao.searchWithSeqNumDesc(cardId);
    }

    public CardComment select(Long id) throws IOException {
        return cardCommentDao.find(id);
    }

    public void deleteByCardIds(List<Long> cardIds) throws IOException {
        cardCommentDao.deleteByCardIds(cardIds);
    }
}
