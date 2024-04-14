package com.ezone.ezproject.modules.card.event.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.transactional.AfterCommit;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.mapper.CardMapper;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.dao.CardEventDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.modules.card.event.model.AddAttachmentEventMsg;
import com.ezone.ezproject.modules.card.event.model.AddRelateCardEventMsg;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.card.event.model.CreateEventMsg;
import com.ezone.ezproject.modules.card.event.model.DelAttachmentEventMsg;
import com.ezone.ezproject.modules.card.event.model.DelRelateCardEventMsg;
import com.ezone.ezproject.modules.card.event.model.EventType;
import com.ezone.ezproject.modules.card.service.CardQueryService;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import com.ezone.ezproject.modules.project.service.ProjectCardSchemaHelper;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
@Slf4j
@AllArgsConstructor
public class CardEventCmdService {
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

    @AfterCommit
    @Async
    public void asyncSave(List<CardEvent> cardEvents) {
        if (CollectionUtils.isEmpty(cardEvents)) {
            return;
        }
        try {
            Map<Long, CardEvent> cardEventMap = new HashMap<>();
            Map<Long, String> cardEventsJsons = new HashMap<>();
            List<Long> statEventCardIds = new ArrayList<>();
            for (CardEvent cardEvent : cardEvents) {
                Long cardId = cardEvent.getCardId();
                cardEventsJsons.put(cardEvent.getId(), JSON_MAPPER.writeValueAsString(cardEvent));
                cardEventMap.put(cardId, cardEvent);
                if (EventType.EVENT_FOR_STAT_CHART.contains(cardEvent.getEventType())) {
                    statEventCardIds.add(cardId);
                }
            }
            cardEventDao.saveOrUpdate(cardEventsJsons);
            // for stat: card.latestEventId & event.nextTime(endTime)
            Map<Long, Map<String, Object>> cardEventsProps = new HashMap<>();
            cardQueryService.select(statEventCardIds).forEach(card -> {
                CardEvent cardEvent = cardEventMap.get(card.getId());
                Map<String, Object> cardEventProps = new HashMap<>();
                cardEventProps.put(CardEvent.NEXT_DATE, cardEvent.getDate());
                if (card.getLatestEventId() > 0) {
                    cardEventsProps.put(card.getLatestEventId(), cardEventProps);
                }
                card.setLatestEventId(cardEvent.getId());
                cardMapper.updateByPrimaryKey(card);
            });
            if (MapUtils.isNotEmpty(cardEventsProps)) {
                try {
                    cardEventDao.updateSelective(cardEventsProps);
                } catch (Exception e) {
                    log.error("Es update card-event-next-id exception!", e);
                }
            }
        } catch (Exception e) {
            log.error("Save events for cardIds exception!", e);
        }
    }

    @AfterCommit
    @Async
    public void asyncSave(CardEvent cardEvent) {
        if (null == cardEvent) {
            return;
        }
        try {
            cardEventDao.saveOrUpdate(cardEvent);
            // for stat: card.latestEventId & event.nextTime(endTime)
            if (!EventType.EVENT_FOR_STAT_CHART.contains(cardEvent.getEventType())) {
                return;
            }
            Map<String, Object> cardEventProps = new HashMap<>();
            cardEventProps.put(CardEvent.NEXT_DATE, cardEvent.getDate());
            Card card = cardQueryService.select(cardEvent.getCardId());
            if (null == card) {
                // todo 此类的所有异步方法都应在当前事务结束后再异步执行；最优是注解+切面(位于异步外层)；新建出问题概率高，影响统计，优先级不高
                log.warn("Find null card:[{}] when set latest event!", cardEvent.getCardId());
            } else {
                if (card.getLatestEventId() > 0) {
                    try {
                        cardEventDao.updateSelective(card.getLatestEventId(), cardEventProps);
                    } catch (Exception e) {
                        log.error("Es update card-event-next-id exception!", e);
                    }
                }
                card.setLatestEventId(cardEvent.getId());
                cardMapper.updateByPrimaryKey(card);
            }
        } catch (Exception e) {
            log.error(String.format("Save event for cardId:[] exception!", cardEvent.getCardId()), e);
        }
    }

    @AfterCommit
    @Async
    public void asyncSaveEvents(Supplier<List<CardEvent>> cardEventsSupplier) {
        try {
            asyncSave(cardEventsSupplier.get());
        } catch (Exception e) {
            log.error("Save events exception!", e);
        }
    }

    @AfterCommit
    @Async
    public void asyncSaveEvent(Supplier<CardEvent> cardEventSupplier) {
        try {
            asyncSave(cardEventSupplier.get());
        } catch (Exception e) {
            log.error("Save event exception!", e);
        }
    }

    @AfterCommit
    @Async
    public void asyncSaveForCreate(Long cardId, String user, Map<String, Object> cardJson) {
        try {
            CardEvent.CardEventBuilder builder = cardEventBuilder(cardId, user, EventType.CREATE, cardJson);
            asyncSave(builder
                    .eventMsg(CreateEventMsg.builder()
                            .title(String.valueOf(cardJson.get(CardField.TITLE)))
                            .build())
                    .build());
        } catch (Exception e) {
            log.error(String.format("Save create-event for cardId:[] exception!", cardId), e);
        }
    }

    @AfterCommit
    @Async
    public void asyncSaveForCreate(String user, Map<Long, Map<String, Object>> cardJsons) {
        try {
            List<CardEvent> cardEvents = new ArrayList<>();
            for (Map.Entry<Long, Map<String, Object>> entry : cardJsons.entrySet()) {
                Long cardId = entry.getKey();
                Map<String, Object> cardJson = entry.getValue();
                CardEvent.CardEventBuilder builder = cardEventBuilder(cardId, user, EventType.CREATE, cardJson);
                CardEvent cardEvent = builder
                        .eventMsg(CreateEventMsg.builder()
                                .title(String.valueOf(cardJson.get(CardField.TITLE)))
                                .build())
                        .build();
                cardEvents.add(cardEvent);
            }
            asyncSave(cardEvents);
        } catch (Exception e) {
            log.error(String.format("Save create-event for cardIds:[] exception!", cardJsons.keySet()), e);
        }
    }

    @AfterCommit
    @Async
    public void asyncSaveForAddAttachment(Long cardId, String user, String fileName, Map<String, Object> cardDetail) {
        try {
            CardEvent.CardEventBuilder builder = cardEventBuilder(cardId, user, EventType.ADD_ATTACHMENT, cardDetail);
            asyncSave(builder
                    .eventMsg(AddAttachmentEventMsg.builder()
                            .fileName(fileName)
                            .build())
                    .build());
        } catch (Exception e) {
            log.error(String.format("Save add-attachment-event for cardId:[] exception!", cardId), e);
        }
    }

    @AfterCommit
    @Async
    public void asyncSaveForRmAttachment(Long cardId, String user, String fileName, Map<String, Object> cardDetail) {
        try {
            CardEvent.CardEventBuilder builder = cardEventBuilder(cardId, user, EventType.RM_ATTACHMENT, cardDetail);
            asyncSave(builder
                    .eventMsg(DelAttachmentEventMsg.builder()
                            .fileName(fileName)
                            .build())
                    .build());
        } catch (Exception e) {
            log.error(String.format("Save rm-attachment-event for cardId:[] exception!", cardId), e);
        }
    }

    @AfterCommit
    @Async
    public void asyncSaveForAddRelateCard(Long cardId, String user, String cardKey, Map<String, Object> cardDetail) {
        try {
            CardEvent.CardEventBuilder builder = cardEventBuilder(cardId, user, EventType.ADD_RELATE_CARD, cardDetail);
            asyncSave(builder
                    .eventMsg(AddRelateCardEventMsg.builder()
                            .cardKey(cardKey)
                            .build())
                    .build());
        } catch (Exception e) {
            log.error(String.format("Save add-attachment-event for cardId:[] exception!", cardId), e);
        }
    }

    @AfterCommit
    @Async
    public void asyncSaveForRmRelateCard(Long cardId, String user, String cardKey, Map<String, Object> cardDetail) {
        try {
            CardEvent.CardEventBuilder builder = cardEventBuilder(cardId, user, EventType.RM_RELATE_CARD, cardDetail);
            asyncSave(builder
                    .eventMsg(DelRelateCardEventMsg.builder()
                            .cardKey(cardKey)
                            .build())
                    .build());
        } catch (Exception e) {
            log.error(String.format("Save rm-attachment-event for cardId:[] exception!", cardId), e);
        }
    }

    @AfterCommit
    @Async
    public void asyncSaveForDelete(Long cardId, String user, Map<String, Object> cardDetail) {
        try {
            CardEvent.CardEventBuilder builder = cardEventBuilder(cardId, user, EventType.DELETE, cardDetail);
            asyncSave(builder.build());
        } catch (Exception e) {
            log.error(String.format("Save delete-event for cardId:[] exception!", cardId), e);
        }
    }


    @AfterCommit
    @Async
    public void asyncSaveForDelete(String user, Map<Long, Map<String, Object>> cardJsons) {
        try {
            List<CardEvent> cardEvents = new ArrayList<>();
            for (Map.Entry<Long, Map<String, Object>> entry : cardJsons.entrySet()) {
                Long cardId = entry.getKey();
                Map<String, Object> cardJson = entry.getValue();
                CardEvent cardEvent = cardEventBuilder(cardId, user, EventType.DELETE, cardJson).build();
                cardEvents.add(cardEvent);
            }
            asyncSave(cardEvents);
        } catch (Exception e) {
            log.error(String.format("Save delete-event for cardIds:[] exception!", cardJsons.keySet()), e);
        }
    }

    @AfterCommit
    @Async
    public void asyncSaveForRecovery(Long cardId, String user, Map<String, Object> cardDetail) {
        try {
            CardEvent.CardEventBuilder builder = cardEventBuilder(cardId, user, EventType.RECOVERY, cardDetail);
            asyncSave(builder.build());
        } catch (Exception e) {
            log.error(String.format("Save recovery-event for cardId:[] exception!", cardId), e);
        }
    }

    @AfterCommit
    @Async
    public void asyncSaveForRecovery(String user, Map<Long, Map<String, Object>> cardJsons) {
        try {
            List<CardEvent> cardEvents = new ArrayList<>();
            for (Map.Entry<Long, Map<String, Object>> entry : cardJsons.entrySet()) {
                Long cardId = entry.getKey();
                Map<String, Object> cardJson = entry.getValue();
                CardEvent cardEvent = cardEventBuilder(cardId, user, EventType.RECOVERY, cardJson).build();
                cardEvents.add(cardEvent);
            }
            asyncSave(cardEvents);
        } catch (Exception e) {
            log.error(String.format("Save recovery-event for cardIds:[%s] exception!", StringUtils.join(cardJsons.keySet(), ",")), e);
        }
    }

    @AfterCommit
    @Async
    public void asyncChangePlanIsActive(String user, List<Long> cardIds) {
        ListUtils.partition(cardIds, 1000).forEach(bathIds -> {
            try {
                Map<Long, Map<String, Object>> cards = cardDao.findAsMap(bathIds);
                List<CardEvent> cardEvents = new ArrayList<>();
                for (Map.Entry<Long, Map<String, Object>> entry : cards.entrySet()) {
                    Long cardId = entry.getKey();
                    Map<String, Object> cardJson = entry.getValue();
                    CardEvent cardEvent = cardEventBuilder(cardId, user, EventType.PLAN_IS_ACTIVE, cardJson).build();
                    cardEvents.add(cardEvent);
                }
                asyncSave(cardEvents);
            } catch (Exception e) {
                log.error(String.format("Change planIsActive for batch cards:[%s] exception!", StringUtils.join(bathIds, ",")), e);
            }
        });
    }

    public void deleteByCardIds(List<Long> cardIds) throws IOException {
        cardEventDao.deleteByCardIds(cardIds);
    }

    private CardEvent.CardEventBuilder cardEventBuilder(Long cardId, String user, EventType eventType, Map<String, Object> cardJson)
            throws IOException {
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
