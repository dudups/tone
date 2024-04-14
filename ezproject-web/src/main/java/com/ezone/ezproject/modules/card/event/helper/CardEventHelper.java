package com.ezone.ezproject.modules.card.event.helper;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.mapper.CardMapper;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.dao.CardEventDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.modules.card.bean.ChangeTypeRequest;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.card.event.model.EventType;
import com.ezone.ezproject.modules.card.event.model.UpdateEventMsg;
import com.ezone.ezproject.modules.card.service.CardQueryService;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import com.ezone.ezproject.modules.project.service.ProjectCardSchemaHelper;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class CardEventHelper {
    public static final ObjectMapper JSON_MAPPER = CardDao.JSON_MAPPER;

    private CardDao cardDao;
    private CardMapper cardMapper;
    private CardEventDao cardEventDao;

    private CardQueryService cardQueryService;
    private ProjectSchemaQueryService projectSchemaQueryService;
    private PlanQueryService planQueryService;
    private ProjectCardSchemaHelper schemaHelper;

    @Getter(lazy = true)
    private final CardField cardFieldParentId = schemaHelper.getSysProjectCardSchema().findCardField(CardField.PARENT_ID);
    @Getter(lazy = true)
    private final CardField cardFieldPlanId = schemaHelper.getSysProjectCardSchema().findCardField(CardField.PLAN_ID);
    @Getter(lazy = true)
    private final CardField cardFieldStoryMapNodeId = schemaHelper.getSysProjectCardSchema().findCardField(CardField.STORY_MAP_NODE_ID);
    @Getter(lazy = true)
    private final CardField cardFieldType = schemaHelper.getSysProjectCardSchema().findCardField(CardField.TYPE);
    @Getter(lazy = true)
    private final CardField cardFieldStatus = schemaHelper.getSysProjectCardSchema().findCardField(CardField.STATUS);

    public List<CardEvent> cardEventsForMigrate(Long projectId, List<Long> cardIds, String user, Map<Long, Long> fromCardPlanIds, Long toPlanId, Map<Long, Plan> plans) {
        try {
            List<CardEvent> cardEvents = new ArrayList<>();
            Plan toPlan = plans.get(toPlanId);
            String toPlanName = null == toPlan ? null : toPlan.getName();
            CardField field = getCardFieldPlanId();
            for (Map.Entry<Long, Map<String, Object>> entry : cardDao.findAsMap(cardIds).entrySet()) {
                Long cardId = entry.getKey();
                Plan fromPlan = plans.get(fromCardPlanIds.get(cardId));
                String fromPlanName = null == fromPlan ? null : fromPlan.getName();
                CardEvent cardEvent = cardEventBuilder(cardId, user, EventType.UPDATE, entry.getValue())
                        .eventMsg(UpdateEventMsg.builder().fieldDetailMsg(UpdateEventMsg.FieldDetailMsg.builder()
                                .fieldKey(CardField.PLAN_ID)
                                .fieldMsg(field.getName())
                                .fromMsg(fromPlanName)
                                .toMsg(toPlanName)
                                .build()).build())
                        .build();
                cardEvents.add(cardEvent);
            }
            return cardEvents;
        } catch (Exception e) {
            log.error(String.format("Generate update-event for migrate cardIds:[] exception!", cardIds), e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public List<CardEvent> cardEventsForUnBindStoryMap(Long projectId, List<Long> cardIds, String user, Map<Long, String> cardStoryMapNodeInfo) {
        try {
            List<CardEvent> cardEvents = new ArrayList<>();
            CardField field = getCardFieldStoryMapNodeId();
            for (Map.Entry<Long, Map<String, Object>> entry : cardDao.findAsMap(cardIds).entrySet()) {
                Long cardId = entry.getKey();
                CardEvent cardEvent = cardEventBuilder(cardId, user, EventType.UPDATE, entry.getValue())
                        .eventMsg(UpdateEventMsg.builder()
                                .fieldDetailMsg(UpdateEventMsg.FieldDetailMsg.builder()
                                        .fieldKey(CardField.STORY_MAP_NODE_ID)
                                        .fieldMsg(field.getName())
                                        .fromMsg(cardStoryMapNodeInfo.get(cardId))
                                        .toMsg(null)
                                        .build())
                                .build())
                        .build();
                cardEvents.add(cardEvent);
            }
            return cardEvents;
        } catch (Exception e) {
            log.error(String.format("Save update-event for unBindStoryMap cardIds:[] exception!", cardIds), e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public List<CardEvent> cardEventsForChangeType(Map<Long, Map<String, Object>> cards, Map<Long, String> cardIdFromTypeMap, Map<Long, String> cardIdFromStatusMap, String user, Map<String, ChangeTypeRequest.TypeChangeConfig> typeChangeConfigMap) {
        try {
            List<CardEvent> cardEvents = new ArrayList<>();
            CardField field = getCardFieldType();
            CardField status = getCardFieldStatus();
            for (Map.Entry<Long, Map<String, Object>> entry : cards.entrySet()) {
                Long cardId = entry.getKey();
                String fromType = cardIdFromTypeMap.get(cardId);
                ChangeTypeRequest.TypeChangeConfig toType = typeChangeConfigMap.get(fromType);
                CardEvent cardEvent = cardEventBuilder(cardId, user, EventType.UPDATE, entry.getValue())
                        .eventMsg(UpdateEventMsg.builder()
                                .fieldDetailMsg(UpdateEventMsg.FieldDetailMsg.builder()
                                        .fieldKey(CardField.TYPE)
                                        .fieldMsg(field.getName())
                                        .fromMsg(fromType)
                                        .toMsg(toType.getToType())
                                        .build())
                                .fieldDetailMsg(UpdateEventMsg.FieldDetailMsg.builder()
                                        .fieldKey(CardField.STATUS)
                                        .fieldMsg(status.getName())
                                        .fromMsg(cardIdFromStatusMap.get(cardId))
                                        .toMsg(toType.getStatusMap().get(cardIdFromStatusMap.get(cardId)))
                                        .build())
                                .build())
                        .build();
                cardEvents.add(cardEvent);
            }
            return cardEvents;
        } catch (Exception e) {
            log.error(String.format("Save update-event for changeType cardIds:[] exception!", cards.keySet()), e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private CardEvent.CardEventBuilder cardEventBuilder(Long cardId, String user, EventType eventType, Map<String, Object> cardJson) {
        return CardEvent.builder()
                .id(IdUtil.generateId())
                .cardId(cardId)
                .date(new Date())
                .user(user)
                .eventType(eventType)
                .cardDetail(cardJson);
    }

    private CardEvent.CardEventBuilder cardEventBuilder(Long projectId, Long cardId, String user, EventType eventType)
            throws IOException {
        return cardEventBuilder(cardId, user, eventType, cardDao.findAsMap(cardId));
    }
}
