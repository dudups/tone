package com.ezone.ezproject.modules.card.field;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.StoryMapNode;
import com.ezone.ezproject.dal.mapper.CardMapper;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.es.entity.ProjectWorkloadSetting;
import com.ezone.ezproject.ez.context.SystemSettingService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bpm.service.CardBpmCmdService;
import com.ezone.ezproject.modules.card.bpm.service.CardBpmQueryService;
import com.ezone.ezproject.modules.card.event.helper.UpdateEventHelper;
import com.ezone.ezproject.modules.card.field.bean.FieldChange;
import com.ezone.ezproject.modules.card.field.check.ActualWorkloadChecker;
import com.ezone.ezproject.modules.card.field.check.CardFieldStatusesChecker;
import com.ezone.ezproject.modules.card.field.check.FieldValueCheckHelper;
import com.ezone.ezproject.modules.card.field.limit.SysFieldOpLimit;
import com.ezone.ezproject.modules.card.field.update.RelationalFieldUpdateHelper;
import com.ezone.ezproject.modules.card.service.CardEndService;
import com.ezone.ezproject.modules.card.service.CardHelper;
import com.ezone.ezproject.modules.card.service.CardNoticeService;
import com.ezone.ezproject.modules.card.update.CardStatusReadOnlyChecker;
import com.ezone.ezproject.modules.common.OperationContext;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService;
import com.ezone.ezproject.modules.event.events.CardUpdateEvent;
import com.ezone.ezproject.modules.log.service.OperationLogCmdService;
import com.ezone.ezproject.modules.project.service.ProjectCardSchemaHelper;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.http.HttpStatus;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class CompletelyFieldsUpdateHelper {
    protected CompanyCardSchema companyCardSchema;
    protected UserService userService;
    protected OperationContext opContext;
    protected ProjectCardSchema schema;
    protected ProjectWorkloadSetting workloadSetting;
    protected boolean checkProps;
    protected ProjectCardSchemaHelper schemaHelper;
    protected CardDao cardDao;
    protected CardMapper cardMapper;
    protected Function<Long, Plan> findPlanById;
    protected Function<Long, Card> findCardById;
    protected Function<Card, List<Card>> findCardDescendant;
    protected Function<Long, StoryMapNode> findStoryMapNodeById;
    protected Function<Long, String> findStoryMapL2NodeInfoById;
    protected Card card;
    protected Map<String, Object> cardDetail;
    protected BiConsumer<Map<String, Object>, List<FieldChange>> setCalcFields;
    protected CardNoticeService cardNoticeService;
    protected CardEndService cardEndService;
    protected CardHelper cardHelper;
    protected OperationLogCmdService operationLogCmdService;
    protected CardBpmCmdService cardBpmCmdService;
    protected SystemSettingService systemSettingService;
    private CompanyProjectSchemaQueryService companyProjectSchemaQueryService;
    private ProjectSchemaQueryService projectSchemaQueryService;
    private CardBpmQueryService cardBpmQueryService;

    public CardUpdateEvent update() throws IOException {
        return update(() -> cardDao.findAsMap(card.getId()), this.cardDetail);
    }

    private static final String NOTICE_AT_USERS = "usersToNotice";

    protected CardUpdateEvent update(Supplier<Map<String, Object>> fromCardDetailSupplier, Map<String, Object> toCardDetail) throws IOException {
        List<String> noticeAtUsers = FieldUtil.toStringList(toCardDetail.get(NOTICE_AT_USERS));
        // remove invalid props; check type&status;
        schemaHelper.preProcessCardProps(schema, toCardDetail);

        // check status&flow&permission
        String status = FieldUtil.toString(toCardDetail.get(CardField.STATUS));
        Map<String, Object> fromCardDetail = fromCardDetailSupplier.get();
        //归档的不允许任何修改
        boolean planIsActive = FieldUtil.getPlanIsActive(fromCardDetail);
        if (!planIsActive) {
            throw new CodedException(HttpStatus.FORBIDDEN, "已归档卡片，禁止直接编辑");
        }
        String fromStatus = FieldUtil.toString(fromCardDetail.get(CardField.STATUS));
        if (!fromStatus.equals(status)) {
            schemaHelper.checkChangeCardStatus(schema, fromCardDetail, opContext.getUserName(), status, systemSettingService.bpmIsOpen());
        }

        List<FieldChange> fieldChanges = diff(schema, toCardDetail, fromCardDetail);
        if (CollectionUtils.isEmpty(fieldChanges)) {
            return null;
        }
        String type = FieldUtil.toString(toCardDetail.get(CardField.TYPE));
        fieldChanges = schema.mergeFlowFieldChange(fromCardDetail, type, status, fieldChanges);
        CardType cardType = schema.findCardType(type);
        if (checkProps) {
            CardStatusReadOnlyChecker.check(cardType, status, fieldChanges);
            // check fields
            CardFieldStatusesChecker.builder()
                    .schema(schema)
                    .fromCardDetail(fromCardDetail)
                    .build()
                    .check(toCardDetail);
            ActualWorkloadChecker.builder()
                    .workloadSetting(workloadSetting)
                    .build()
                    .check(fieldChanges);
            // check fields value
            checkChangeFieldValue(fieldChanges);
        }
        Map<String, Object> finalCardDetail = new HashMap<>();
        finalCardDetail.putAll(fromCardDetail);
        CardHelper.generatePropsForUpdate(finalCardDetail, opContext, fieldChanges);
        setCalcFields.accept(finalCardDetail, fieldChanges);

        cardDao.saveOrUpdate(card.getId(), finalCardDetail);
        CardUpdateEvent cardUpdateEvent = cardUpdateEvent(fieldChanges, finalCardDetail);
        // status flow notice
        cardNoticeService.noticeStatusFlow(opContext.getUserName(), finalCardDetail, schema, fromStatus, status);
        // cardEnd
        if (CardHelper.isChangeToEnd(schema, type, fromStatus, status)) {
            cardEndService.cardEnd(FieldUtil.toStringList(finalCardDetail.get(CardField.OWNER_USERS)), card);
        }
        // at users
        if (CollectionUtils.isNotEmpty(noticeAtUsers)) {
            cardNoticeService.noticeAtUsersInCard(card, finalCardDetail, opContext.getUserName(), FieldUtil.getAtUsers(finalCardDetail), companyCardSchema, schema, ProjectNoticeConfig.Type.UPDATE);
        }
        return cardUpdateEvent;
    }

    protected void checkChangeFieldValue(List<FieldChange> fieldChanges) {
        // check fields values[[
        FieldValueCheckHelper checker = FieldValueCheckHelper.builder()
                .projectId(card.getProjectId())
                .findCardById(findCardById)
                .findPlanById(findPlanById)
                .findStoryMapNodeById(findStoryMapNodeById)
                .build();
        fieldChanges.forEach(change -> checker.check(change.getField(), change.getToValue()));
        // update relational field in db
        RelationalFieldUpdateHelper updateHelper = RelationalFieldUpdateHelper.builder()
                .projectId(card.getProjectId())
                .cardMapper(cardMapper)
                .findPlanById(findPlanById)
                .findCardById(findCardById)
                .findCardDescendant(findCardDescendant)
                .build();
        fieldChanges.forEach(change -> updateHelper.update(card, change.getField().getKey(), change.getToValue()));
    }

    protected CardUpdateEvent cardUpdateEvent(List<FieldChange> fieldChanges, Map<String, Object> toCardDetail) throws IOException {
        // event
        return UpdateEventHelper.builder()
                .findPlanById(findPlanById)
                .findCardById(findCardById)
                .findStoryMapL2NodeInfoById(findStoryMapL2NodeInfoById)
                .build()
                .cardUpdateEvent(card, opContext.getUserName(), fieldChanges, toCardDetail);
    }

    @NotNull
    protected List<FieldChange> diff(ProjectCardSchema schema, Map<String, Object> cardRequest, Map<String, Object> cardJson) {
        List<FieldChange> fieldChanges = new ArrayList<>();
        cardRequest.entrySet().stream()
                .filter(e -> SysFieldOpLimit.canOp(e.getKey(), SysFieldOpLimit.Op.UPDATE))
                .forEach(entry -> {
                    String key = entry.getKey();
                    Object fromValue = cardJson.get(key);
                    Object toValue = entry.getValue();
                    CardField field = schema.findCardField(key);
                    switch (field.getValueType()) {
                        default:
                            if (!FieldUtil.equals(field, cardJson.get(key), toValue)) {
                                fieldChanges.add(
                                        FieldChange.builder()
                                                .field(field)
                                                .fromValue(fromValue)
                                                .toValue(toValue)
                                                .build());
                            }
                    }
                });
        return fieldChanges;
    }

}
