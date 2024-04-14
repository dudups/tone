package com.ezone.ezproject.modules.card.event.helper;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.enums.FieldType;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.card.event.model.EventType;
import com.ezone.ezproject.modules.card.event.model.UpdateEventMsg;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.field.bean.FieldChange;
import com.ezone.ezproject.modules.event.events.CardUpdateEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class UpdateEventHelper {
    public static final ObjectMapper JSON_MAPPER = CardDao.JSON_MAPPER;

    private Function<Long, Plan> findPlanById;
    private Function<Long, Card> findCardById;
    private Function<Long, String> findStoryMapL2NodeInfoById;

    public CardUpdateEvent cardUpdateEvent(Card card, String user, List<FieldChange> fieldChanges, Map<String, Object> cardDetail) {
        return CardUpdateEvent.builder()
                .user(user)
                .card(card)
                .cardEvent(cardEvent(card.getId(), user, fieldChanges, cardDetail))
                .build();
    }

    public CardUpdateEvent cardUpdateEvent(Card card, String user, FieldChange fieldChange, Map<String, Object> cardJson) {
        return cardUpdateEvent(card, user, Arrays.asList(fieldChange), cardJson);
    }

    private CardEvent cardEvent(Long cardId, String user, List<FieldChange> fieldChanges, Map<String, Object> cardJson) {
        try {
            UpdateEventMsg.UpdateEventMsgBuilder builder = UpdateEventMsg.builder();
            fieldChanges.forEach(fieldChange -> {
                CardField field = fieldChange.getField();
                switch (field.getKey()) {
                    case CardField.CONTENT:
                        builder.fieldMsg(UpdateEventMsg.FieldMsg.builder()
                                .fieldKey(field.getKey())
                                .fieldMsg(field.getName())
                                .build());
                        break;
                    case CardField.PARENT_ID:
                        Long fromParentId = FieldUtil.toLong(fieldChange.getFromValue());
                        Long toParentId = FieldUtil.toLong(fieldChange.getToValue());
                        Card fromParent = findCardById.apply(fromParentId);
                        Card toParent = findCardById.apply(toParentId);
                        String fromParentName = null == fromParent ? null : String.format("%s-%s", fromParent.getProjectKey(), fromParent.getSeqNum());
                        String toParentName = null == toParent ? null : String.format("%s-%s", toParent.getProjectKey(), toParent.getSeqNum());
                        builder.fieldDetailMsg(UpdateEventMsg.FieldDetailMsg.builder()
                                .fieldKey(CardField.PARENT_ID)
                                .fieldMsg(field.getName())
                                .fromMsg(fromParentName)
                                .toMsg(toParentName)
                                .build());
                        break;
                    case CardField.PLAN_ID:
                        Long fromPlanId = FieldUtil.toLong(fieldChange.getFromValue());
                        Long toPlanId = FieldUtil.toLong(fieldChange.getToValue());
                        Plan fromPlan = findPlanById.apply(fromPlanId);
                        Plan toPlan = findPlanById.apply(toPlanId);
                        String fromPlanName = null == fromPlan ? null : fromPlan.getName();
                        String toPlanName = null == toPlan ? null : toPlan.getName();
                        builder.fieldDetailMsg(UpdateEventMsg.FieldDetailMsg.builder()
                                .fieldKey(CardField.PLAN_ID)
                                .fieldMsg(field.getName())
                                .fromMsg(fromPlanName)
                                .toMsg(toPlanName)
                                .build());
                        break;
                    case CardField.STORY_MAP_NODE_ID:
                        Long fromStoryMapNodeId = FieldUtil.toLong(fieldChange.getFromValue());
                        Long toStoryMapNodeId = FieldUtil.toLong(fieldChange.getToValue());
                        builder.fieldDetailMsg(UpdateEventMsg.FieldDetailMsg.builder()
                                .fieldKey(CardField.STORY_MAP_NODE_ID)
                                .fieldMsg(field.getName())
                                .fromMsg(findStoryMapL2NodeInfoById.apply(fromStoryMapNodeId))
                                .toMsg(findStoryMapL2NodeInfoById.apply(toStoryMapNodeId))
                                .build());
                        break;
                    default:
                        builder.fieldDetailMsg(UpdateEventMsg.FieldDetailMsg.builder()
                                .fieldKey(field.getKey())
                                .fieldMsg(field.getName())
                                .fromMsg(toMsg(field, fieldChange.getFromValue()))
                                .toMsg(toMsg(field, fieldChange.getToValue()))
                                .build());
                }
            });
            return cardEventBuilder(cardId, user, EventType.UPDATE, cardJson).eventMsg(builder.build()).build();
        } catch (IOException e) {
            log.error(String.format("Save events for cardId:[] exception!", cardId), e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private static String toMsg(CardField field, Object value) {
        if (value == null) {
            return null;
        }
        if (FieldType.ValueType.BOOLEAN.equals(field.getValueType())) {
            return "true".equalsIgnoreCase(String.valueOf(value)) ? "是" : "否";
        } else if (FieldType.SELECT.equals(field.getType()) || FieldType.SELECT.equals(field.getType())) {
            if (CollectionUtils.isNotEmpty(field.getOptions())) {
                Function<String, String> getOperationName = key -> {
                    CardField.Option option = field.getOptions().stream().filter(o -> o.getKey().equals(key)).findAny().orElse(null);
                    return option == null ? key : option.getName();
                };
                if (value instanceof String) {
                    return getOperationName.apply((String) value);
                } else if (value instanceof Collection) {
                    return FieldUtil.toString(((Collection) value).stream().map(getOperationName).collect(Collectors.toList()));
                } else if (value instanceof String[]) {
                    return FieldUtil.toString(Stream.of((String[]) value).map(getOperationName).collect(Collectors.toList()));
                }

            }
        }
        return FieldUtil.toString(value);
    }

    private static CardEvent.CardEventBuilder cardEventBuilder(Long cardId, String user, EventType eventType, Map<String, Object> cardJson)
            throws IOException {
        return CardEvent.builder()
                .id(IdUtil.generateId())
                .cardId(cardId)
                .date(new Date())
                .user(user)
                .eventType(eventType)
                .cardDetail(cardJson);
    }
}
