package com.ezone.ezproject.modules.card.field.update;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.function.CacheableFunction;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.StoryMapNode;
import com.ezone.ezproject.dal.mapper.CardMapper;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.modules.card.event.helper.UpdateEventHelper;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.field.bean.FieldChange;
import com.ezone.ezproject.modules.card.field.check.FieldValueCheckHelper;
import com.ezone.ezproject.modules.card.field.limit.SysFieldOpLimit;
import com.ezone.ezproject.modules.card.service.CardHelper;
import com.ezone.ezproject.modules.event.events.CardUpdateEvent;
import com.ezone.ezproject.modules.event.events.CardsUpdateEvent;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class BatchUpdateStatusHelper {
    protected Function<List<Card>, Map<Long, List<Card>>> findCardsDescendant;
    protected Function<List<Card>, Map<Long, List<Card>>> findCardsAncestor;
    private String user;
    private ProjectCardSchema schema;
    private CardDao cardDao;
    private CardMapper cardMapper;
    private Function<Long, Plan> findPlanById;
    private Function<Long, Card> findCardById;
    private List<Card> cards;
    private Function<Long, StoryMapNode> findStoryMapNodeById;
    private Function<Long, String> findStoryMapL2NodeInfoById;
    private ProjectQueryService projectQueryService;
    private Long projectId;
    private Map<Long, Map<String, Object>> cardsJson;

    public CardsUpdateEvent update(String fieldKey, String valueString) throws IOException {
        List<CardUpdateEvent> events = new ArrayList<>();

        CardField field = schema.findCardField(fieldKey);
        if (!SysFieldOpLimit.canOp(field, SysFieldOpLimit.Op.UPDATE)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE,
                    String.format("Cannot directly update value for field:[%s]!", field));
        }

        Object value = FieldUtil.parse(
                field.getValueType() == null ? field.getType().getDefaultValueType() : field.getValueType(),
                valueString);
        cardsJson.forEach((key, cardJson) -> {
            String type = FieldUtil.toString(cardJson.get(CardField.TYPE));
            CardType cardType = schema.findCardType(type);
            CardType.FieldConf fieldConf = cardType.findFieldConf(fieldKey);
            if (null == fieldConf || !fieldConf.isEnable()) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE,
                        String.format("Invalid field:[%s]!", fieldKey));
            }
        });

        // check
        FieldValueCheckHelper.builder()
                .projectId(projectId)
                .findCardById(CacheableFunction.instance(cardMapper::selectByPrimaryKey))
                .findPlanById(CacheableFunction.instance(this.findPlanById))
                .findStoryMapNodeById(findStoryMapNodeById)
                .build()
                .check(field, value);

        // update es
        Map<String, Object> cardProps = new HashMap<>();
        cardProps.put(fieldKey, value);
        cardProps.put(CardField.LAST_MODIFY_USER, user);
        // 更新请求es，传的是Date.toString，直接传Date日期格式有问题
        cardProps.put(CardField.LAST_MODIFY_TIME, System.currentTimeMillis());
        cardDao.updateSelective(cards.stream().map(Card::getId).collect(Collectors.toList()), cardProps);

        // event
        for (Card card : cards) {
            Map<String, Object> cardJson = cardsJson.get(card.getId());
            Object fromValue = cardJson.get(fieldKey);
            CardUpdateEvent cardUpdateEvent = UpdateEventHelper.builder()
                    .findPlanById(findPlanById)
                    .findCardById(findCardById)
                    .findStoryMapL2NodeInfoById(findStoryMapL2NodeInfoById)
                    .build()
                    .cardUpdateEvent(
                            card,
                            user,
                            FieldChange.builder().field(field).fromValue(fromValue).toValue(value).build(),
                            CardHelper.generatePropsForUpdate(cardJson, user, field.getKey(), value)
                    );
            events.add(cardUpdateEvent);
        }
        return CardsUpdateEvent.builder()
                .project(projectQueryService.select(projectId))
                .user(user)
                .cardEvents(events.stream().map(CardUpdateEvent::getCardEvent).collect(Collectors.toList()))
                .build();
    }
}
