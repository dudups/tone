package com.ezone.ezproject.modules.card.update;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.StoryMapNode;
import com.ezone.ezproject.dal.mapper.CardMapper;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.UserProjectPermissions;
import com.ezone.ezproject.modules.card.bean.UpdateCardsFieldsRequest;
import com.ezone.ezproject.modules.card.event.helper.UpdateEventHelper;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.field.bean.FieldChange;
import com.ezone.ezproject.modules.card.service.CardEndService;
import com.ezone.ezproject.modules.card.service.CardHelper;
import com.ezone.ezproject.modules.card.service.CardNoticeService;
import com.ezone.ezproject.modules.common.OperationContext;
import com.ezone.ezproject.modules.event.events.CardUpdateEvent;
import com.ezone.ezproject.modules.event.events.CardsUpdateEvent;
import com.ezone.ezproject.modules.project.service.ProjectCardSchemaHelper;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.http.HttpStatus;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class BatchCardsFieldsUpdateHelper {
    protected OperationContext opContext;
    protected BiConsumer<Map<String, Object>, List<FieldChange>> setCalcFields;
    protected Project project;
    protected ProjectCardSchema schema;
    protected UserProjectPermissions permissions;

    protected List<Card> cards;
    protected Map<Long, Map<String, Object>> cardDetails;
    protected Map<String, List<UpdateCardsFieldsRequest.FieldOperation>> typeOpsMap;

    protected ResultCollector resultCollector;

    protected ProjectCardSchemaHelper schemaHelper;
    protected CardDao cardDao;
    protected CardMapper cardMapper;
    protected Function<Long, Plan> findPlanById;
    protected Function<Long, Card> findCardById;
    protected Function<Card, List<Card>> findCardDescendant;
    protected Function<Long, StoryMapNode> findStoryMapNodeById;
    protected Function<Long, String> findStoryMapL2NodeInfoById;

    protected CardNoticeService cardNoticeService;
    protected CardEndService cardEndService;
    protected CardHelper cardHelper;

    public CardsUpdateEvent update() {
        List<CardEvent> cardEvents = new ArrayList<>();
        Map<Long, Map<String, Object>> toCardsDetails = new HashMap<>();
        for (Card card : cards) {
            Map<String, Object> cardDetail = cardDetails.get(card.getId());
            try {
                String type = FieldUtil.getType(cardDetail);
                List<FieldChange> fieldChanges = diff(schema, cardDetail, typeOpsMap.get(type));
                if (!fieldChanges.isEmpty()) {
                    schema.checkFieldChange(type, FieldUtil.getStatus(cardDetail), fieldChanges);
                    //check stats readonly
                    CardType cardType = schema.findCardType(type);
                    CardStatusReadOnlyChecker.check(cardType, FieldUtil.getStatus(cardDetail), fieldChanges);

                    fieldChanges = schema.mergeFlowFieldChange(cardDetail, type, FieldUtil.getStatus(cardDetail), fieldChanges);
                    CardHelper.generatePropsForUpdate(cardDetail, opContext, fieldChanges);
                    setCalcFields.accept(cardDetail, fieldChanges);
                    toCardsDetails.put(card.getId(), cardDetail);
                    CardEvent cardEvent = update(card, fieldChanges, cardDetail);
                    cardEvents.add(cardEvent);
                }
                resultCollector.addSuccess();
            } catch (CodedException e) {
                resultCollector.addFailure(card.getId(), e.getMessage());
            } catch (Exception e) {
                log.error(String.format("Batch update project[%d] cards fields exception!", project.getId()), e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }
        if (MapUtils.isNotEmpty(toCardsDetails)) {
            try {
                cardDao.saveOrUpdate(toCardsDetails);
            } catch (IOException e) {
                log.error(String.format("Batch update project[%d] cards fields exception!", project.getId()), e);
            }
        }
        return CardsUpdateEvent.builder()
                .project(project)
                .user(opContext.getUserName())
                .cardEvents(cardEvents)
                .build();
    }

    protected CardEvent update(
            Card card,
            @NotNull @NotEmpty List<FieldChange> fieldChanges,
            Map<String, Object> toCardDetail) throws IOException {
        CardUpdateEvent cardUpdateEvent = UpdateEventHelper.builder()
                .findPlanById(findPlanById)
                .findCardById(findCardById)
                .findStoryMapL2NodeInfoById(findStoryMapL2NodeInfoById)
                .build()
                .cardUpdateEvent(
                        card,
                        opContext.getUserName(),
                        fieldChanges,
                        toCardDetail
                );
        return cardUpdateEvent.getCardEvent();
    }

    @NotNull
    protected List<FieldChange> diff(ProjectCardSchema schema, Map<String, Object> cardDetail, List<UpdateCardsFieldsRequest.FieldOperation> ops) {
        if (CollectionUtils.isEmpty(ops)) {
            return ListUtils.EMPTY_LIST;
        }
        List<FieldChange> fieldChanges = new ArrayList<>();
        ops.stream()
                .forEach(op -> {
                    String key = op.getField();
                    CardField field = schema.findCardField(key);
                    Object fromValue = cardDetail.get(key);
                    Object changeValue = op.getValue() == null ? null : FieldUtil.parse(field.getValueType(), op.getValue());
                    switch (op.getOpType()) {
                        case SET:
                            Object toValue = changeValue;
                            if (!FieldUtil.equals(field, cardDetail.get(key), toValue)) {
                                fieldChanges.add(FieldChange.builder()
                                        .field(field)
                                        .fromValue(fromValue)
                                        .toValue(toValue)
                                        .build());
                            }
                            break;
                        case ADD:
                            if (changeValue == null) {
                                break;
                            }
                            List addValues = (List) changeValue;
                            if (addValues.isEmpty()) {
                                break;
                            }
                            if (fromValue == null) {
                                fieldChanges.add(FieldChange.builder()
                                        .field(field)
                                        .fromValue(null)
                                        .toValue(changeValue)
                                        .build());
                            } else {
                                List fromValues = (List) fromValue;
                                List diffValues = ListUtils.subtract(addValues, fromValues);
                                if (!diffValues.isEmpty()) {
                                    List toValues = new ArrayList(fromValues);
                                    toValues.addAll(diffValues);
                                    fieldChanges.add(FieldChange.builder()
                                            .field(field)
                                            .fromValue(fromValues)
                                            .toValue(toValues)
                                            .build());
                                }
                            }
                            break;
                        case REMOVE:
                            if (changeValue == null || fromValue == null) {
                                break;
                            }
                            List rmValues = (List) changeValue;
                            List fromValues = (List) fromValue;

                            List diffValues = ListUtils.intersection(rmValues, fromValues);
                            if (!diffValues.isEmpty()) {
                                List toValues = new ArrayList(fromValues);
                                toValues.removeAll(diffValues);
                                fieldChanges.add(FieldChange.builder()
                                        .field(field)
                                        .fromValue(fromValues)
                                        .toValue(toValues)
                                        .build());
                            }
                            break;
                    }
                });
        return fieldChanges;
    }

}
