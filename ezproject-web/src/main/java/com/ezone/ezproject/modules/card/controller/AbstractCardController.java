package com.ezone.ezproject.modules.card.controller;

import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.CardToken;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.mapper.ExtPlanMapper;
import com.ezone.ezproject.dal.mapper.PlanMapper;
import com.ezone.ezproject.es.entity.CardDraft;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.UserProjectPermissions;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.card.bean.CardIdToken;
import com.ezone.ezproject.modules.card.service.CardDraftQueryService;
import com.ezone.ezproject.modules.card.service.CardHelper;
import com.ezone.ezproject.modules.card.service.CardQueryService;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.SetUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Setter(onMethod_ = {@Autowired})
@NoArgsConstructor
@Slf4j
public abstract class AbstractCardController extends AbstractController {
    protected CardQueryService cardQueryService;
    protected CardDraftQueryService cardDraftQueryService;
    protected ExtPlanMapper plannerMapper;
    protected CardHelper cardHelper;

    public static final List<String> GUEST_FORBID_FIELDS = Arrays.asList(CardField.PLAN_ID, CardField.STORY_MAP_NODE_ID, CardField.STATUS, CardField.OWNER_USERS);

    protected void checkCardLimitPermission(Long projectId, OperationType op, Map<String, Object> cardProps) {
        checkProjectActive(projectId, op);
        UserProjectPermissions permissions = permissions(projectId);
        if (permissions == null) {
            throw CodedException.FORBIDDEN;
        }
        if (permissions.isAdmin()) {
            return;
        }
        if (!permissions.hasLimitPermission(op, cardProps)) {
            throw CodedException.FORBIDDEN;
        }
    }

    protected void checkCardsLimitPermission(Long projectId, OperationType op, Map<Long, Map<String, Object>> cards) {
        checkProjectActive(projectId, op);
        UserProjectPermissions permissions = permissions(projectId);
        if (permissions == null) {
            throw CodedException.FORBIDDEN;
        }
        if (permissions.isAdmin()) {
            return;
        }
        cards.values().forEach(card -> {
            if (!permissions.hasLimitPermission(op, card)) {
                throw CodedException.FORBIDDEN;
            }
        });
    }

    protected void checkCardLimitPermission(Long projectId, OperationType op, Long cardId) {
        checkProjectActive(projectId, op);
        UserProjectPermissions permissions = permissions(projectId);
        if (permissions == null) {
            throw CodedException.FORBIDDEN;
        }
        if (permissions.isAdmin()) {
            return;
        }
        if (!permissions.hasLimitPermission(op, cardOpLimitFields(cardId))) {
            throw CodedException.FORBIDDEN;
        }
    }

    private Map<String, Object> cardOpLimitFields(Long cardId) {
        try {
            return cardQueryService.selectDetail(cardId, OperationType.CARD_OP_LIMIT_FIELDS);
        } catch (IOException e) {
            log.error("select card detail exception!", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    protected void checkCanCreateCard(Long projectId, Map<String, Object> card) {
        checkCardLimitPermission(projectId, OperationType.CARD_CREATE, card);
    }

    protected void checkCanCreateCards(Long projectId, Map<Long, Map<String, Object>> cards) {
        checkCardsLimitPermission(projectId, OperationType.CARD_CREATE, cards);
    }

    protected void checkCanUpdateCard(Long projectId, Long cardId, String field, Object value) {
        checkProjectActive(projectId, OperationType.CARD_UPDATE);
        UserProjectPermissions permissions = permissions(projectId);
        if (permissions == null) {
            throw CodedException.FORBIDDEN;
        }
        if (permissions.isAdmin()) {
            return;
        }
        if (!permissions.hasLimitPermission(OperationType.CARD_UPDATE, cardOpLimitFields(cardId), field, value)) {
            throw CodedException.FORBIDDEN;
        }
    }

    protected void checkCanUpdateCard(Long projectId, List<Long> cardIds, String field, Object value) {
        checkProjectActive(projectId, OperationType.CARD_UPDATE);
        UserProjectPermissions permissions = permissions(projectId);
        if (permissions == null) {
            throw CodedException.FORBIDDEN;
        }
        if (permissions.isAdmin()) {
            return;
        }
        try {
            Map<Long, Map<String, Object>> cardDetails = cardQueryService.selectDetail(cardIds, OperationType.CARD_OP_LIMIT_FIELDS);
            cardIds.forEach(cardId -> {
                if (!permissions.hasLimitPermission(OperationType.CARD_UPDATE, cardDetails.get(cardId), field, value)) {
                    throw CodedException.FORBIDDEN;
                }
            });
        } catch (IOException e) {
            log.error("select card detail exception!", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    protected void checkCanUpdateCard(Long projectId, Long cardId, String field1, Object value1, String field2, Object value2) {
        checkProjectActive(projectId, OperationType.CARD_UPDATE);
        UserProjectPermissions permissions = permissions(projectId);
        if (permissions == null) {
            throw CodedException.FORBIDDEN;
        }
        if (permissions.isAdmin()) {
            return;
        }
        if (!permissions.hasLimitPermission(OperationType.CARD_UPDATE, cardOpLimitFields(cardId), field1, value1, field2, value2)) {
            throw CodedException.FORBIDDEN;
        }
    }

    protected void checkCanUpdateCard(Long projectId, Long cardId, Map<String, Object> card) throws IOException {
        checkProjectActive(projectId, OperationType.CARD_UPDATE);
        UserProjectPermissions permissions = permissions(projectId);
        if (permissions == null) {
            throw CodedException.FORBIDDEN;
        }
        if (permissions.isAdmin()) {
            return;
        }
        if (!permissions.hasLimitPermission(OperationType.CARD_UPDATE, cardOpLimitFields(cardId), card)) {
            throw CodedException.FORBIDDEN;
        }
    }

    protected Long getProjectId(Long cardId) {
        return cardOrDraft(cardId, Card::getProjectId, CardDraft::getProjectId);
    }

    protected void checkCanUpdateCard(Long projectId, Long cardId) {

        checkCardLimitPermission(projectId, OperationType.CARD_UPDATE, cardId);
    }

    protected void checkPlanIsActive(Card card) {
        Plan plan = plannerMapper.selectByPrimaryKey(card.getPlanId());
        if (plan != null && BooleanUtils.isNotTrue(plan.getIsActive())) {
            throw PLAN_NOT_ACTIVE_EXCEPTION;
        }
    }

    protected void cardOrDraft(Long cardId, Consumer<Card> cardConsumer, Consumer<CardDraft> draftConsumer) {
        cardOrDraft(cardId,
                card -> {
                    cardConsumer.accept(card);
                    return 0;
                },
                draft -> {
                    draftConsumer.accept(draft);
                    return 0;
                });
    }

    protected <T> T cardOrDraft(Long cardId, Function<Card, T> cardConsumer, Function<CardDraft, T> draftConsumer) {
        Card card = cardQueryService.select(cardId);
        if (null != card) {
            return cardConsumer.apply(card);
        }
        CardDraft draft;
        try {
            draft = cardDraftQueryService.select(cardId);
        } catch (IOException e) {
            log.error("select draft exception!", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        if (null != draft) {
            return draftConsumer.apply(draft);
        }
        throw CodedException.NOT_FOUND;
    }

    protected Set<CardIdToken> validAccessTokens(Long accessFrom, List<CardIdToken> cardAccessTokens) {
        if (CollectionUtils.isEmpty(cardAccessTokens)) {
            return SetUtils.EMPTY_SET;
        }
        List<CardToken> cardTokens = cardQueryService.selectCardToken(cardAccessTokens.stream().map(CardIdToken::getId).distinct().collect(Collectors.toList()));
        return cardTokens.stream()
                .map(token -> CardIdToken.builder().id(token.getCardId()).token(cardHelper.cardAccessToken(accessFrom, token)).build())
                .collect(Collectors.toSet());
    }
}
