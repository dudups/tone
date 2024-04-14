package com.ezone.ezproject.modules.card.service;

import com.ezone.ezbase.iam.bean.BaseUser;
import com.ezone.ezbase.iam.bean.LoginUser;
import com.ezone.ezproject.Application;
import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.exception.ErrorCode;
import com.ezone.ezproject.common.function.CacheableFunction;
import com.ezone.ezproject.common.storage.IStorage;
import com.ezone.ezproject.common.stream.CollectorsV2;
import com.ezone.ezproject.common.transactional.AfterCommit;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.CardExample;
import com.ezone.ezproject.dal.entity.CardRelateRel;
import com.ezone.ezproject.dal.entity.CardTokenExample;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.mapper.CardAttachmentRelMapper;
import com.ezone.ezproject.dal.mapper.CardMapper;
import com.ezone.ezproject.dal.mapper.CardRelateRelMapper;
import com.ezone.ezproject.dal.mapper.CardTokenMapper;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.entity.CardDraft;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardStatus;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.es.entity.ProjectWorkloadSetting;
import com.ezone.ezproject.es.entity.UserProjectPermissions;
import com.ezone.ezproject.es.entity.enums.InnerCardType;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.es.entity.enums.WebHookEventType;
import com.ezone.ezproject.external.ci.bean.AutoStatusFlowEventType;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.SystemSettingService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.attachment.service.CardAttachmentCmdService;
import com.ezone.ezproject.modules.card.bean.BpmUserChoosesRequest;
import com.ezone.ezproject.modules.card.bean.CardIncrWorkloadResult;
import com.ezone.ezproject.modules.card.bean.CardRemindRequest;
import com.ezone.ezproject.modules.card.bean.ChangeTypeCheckResult;
import com.ezone.ezproject.modules.card.bean.ChangeTypeRequest;
import com.ezone.ezproject.modules.card.bean.IncrWorkloadRequest;
import com.ezone.ezproject.modules.card.bean.QuickCreateCardRequest;
import com.ezone.ezproject.modules.card.bean.StatusBatchUpdateRequest;
import com.ezone.ezproject.modules.card.bean.UpdateCardsFieldsRequest;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.bean.query.In;
import com.ezone.ezproject.modules.card.bean.query.Lt;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.card.bpm.bean.CardBpmFlow;
import com.ezone.ezproject.modules.card.bpm.bean.StatusFlowResult;
import com.ezone.ezproject.modules.card.bpm.service.CardBpmCmdService;
import com.ezone.ezproject.modules.card.bpm.service.CardBpmQueryService;
import com.ezone.ezproject.modules.card.bpm.service.CardWorkloadBpmCmdService;
import com.ezone.ezproject.modules.card.bpm.service.CardWorkloadBpmQueryService;
import com.ezone.ezproject.modules.card.copy.AbstractCardCopy;
import com.ezone.ezproject.modules.card.copy.DiffProjectCardCopy;
import com.ezone.ezproject.modules.card.copy.PlanInternalCardCopy;
import com.ezone.ezproject.modules.card.copy.ProjectInternalCardCopy;
import com.ezone.ezproject.modules.card.event.helper.CardEventHelper;
import com.ezone.ezproject.modules.card.event.helper.UpdateEventHelper;
import com.ezone.ezproject.modules.card.event.model.AutoStatusFlowEventMsg;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.card.event.model.CardIncrWorkload;
import com.ezone.ezproject.modules.card.event.model.EventType;
import com.ezone.ezproject.modules.card.event.model.UpdateEventMsg;
import com.ezone.ezproject.modules.card.event.service.CardEventCmdService;
import com.ezone.ezproject.modules.card.excel.ExcelCardImport;
import com.ezone.ezproject.modules.card.field.CompletelyFieldsUpdateHelper;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.field.MultiFieldsUpdateHelper;
import com.ezone.ezproject.modules.card.field.SingleFieldBatchUpdateHelper;
import com.ezone.ezproject.modules.card.field.StoryMapLocationUpdateHelper;
import com.ezone.ezproject.modules.card.field.bean.FieldChange;
import com.ezone.ezproject.modules.card.field.calc.CacheableFieldRefValueLoader;
import com.ezone.ezproject.modules.card.field.calc.FieldCalcGraph;
import com.ezone.ezproject.modules.card.field.check.ActualWorkloadChecker;
import com.ezone.ezproject.modules.card.field.check.CardFieldStatusesChecker;
import com.ezone.ezproject.modules.card.field.check.FieldValueCheckHelper;
import com.ezone.ezproject.modules.card.field.limit.SysFieldOpLimit;
import com.ezone.ezproject.modules.card.rank.RankLocation;
import com.ezone.ezproject.modules.card.status.flow.AutoStatusFlowEventFilter;
import com.ezone.ezproject.modules.card.status.flow.AutoStatusFlowMatcher;
import com.ezone.ezproject.modules.card.tree.TreePromoteForDelete;
import com.ezone.ezproject.modules.card.update.BatchCardsFieldsUpdateHelper;
import com.ezone.ezproject.modules.card.update.FieldOperationsChecker;
import com.ezone.ezproject.modules.card.update.ResultCollector;
import com.ezone.ezproject.modules.comment.service.CardCommentService;
import com.ezone.ezproject.modules.common.OperationContext;
import com.ezone.ezproject.modules.common.TransactionHelper;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService;
import com.ezone.ezproject.modules.event.EventDispatcher;
import com.ezone.ezproject.modules.event.events.CardCreateEvent;
import com.ezone.ezproject.modules.event.events.CardDeleteEvent;
import com.ezone.ezproject.modules.event.events.CardRecoveryEvent;
import com.ezone.ezproject.modules.event.events.CardUpdateEvent;
import com.ezone.ezproject.modules.event.events.CardsCloseStatusEvent;
import com.ezone.ezproject.modules.event.events.CardsDeleteEvent;
import com.ezone.ezproject.modules.event.events.CardsRecoveryEvent;
import com.ezone.ezproject.modules.event.events.CardsUpdateEvent;
import com.ezone.ezproject.modules.hook.service.WebHookProjectCmdService;
import com.ezone.ezproject.modules.log.service.OperationLogCmdService;
import com.ezone.ezproject.modules.permission.PermissionService;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import com.ezone.ezproject.modules.project.bean.CheckSchemaResult;
import com.ezone.ezproject.modules.project.service.ProjectCardSchemaHelper;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import com.ezone.ezproject.modules.storymap.service.StoryMapQueryService;
import com.ezone.ezproject.modules.template.service.ProjectCardTemplateService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
@AllArgsConstructor
public class CardCmdService {
    private CardMapper cardMapper;

    private CardDao cardDao;

    private CardTokenMapper cardTokenMapper;

    private CardQueryService cardQueryService;

    private ProjectCardTemplateService projectCardTemplateService;

    private CardDraftCmdService cardDraftCmdService;
    private CardDraftQueryService cardDraftQueryService;

    private UserService userService;

    private CompanyService companyService;

    private SystemSettingService systemSettingService;

    private ProjectQueryService projectQueryService;

    private ProjectSchemaQueryService projectSchemaQueryService;
    private CompanyProjectSchemaQueryService companyProjectSchemaQueryService;

    private PlanQueryService planQueryService;

    private CardHelper cardHelper;

    private ProjectCardSchemaHelper schemaHelper;

    private CardAttachmentRelMapper cardAttachmentRelMapper;

    private CardRelateRelMapper cardRelateRelMapper;

    private CardEventCmdService cardEventCmdService;

    private IStorage storage;

    private CardAttachmentCmdService cardAttachmentCmdService;

    private CardCommentService cardCommentService;

    private CardNoticeService cardNoticeService;

    private CardRelateRelCmdService cardRelateRelCmdService;

    private CardRelateRelQueryService cardRelateRelQueryService;

    private StoryMapQueryService storyMapQueryService;

    private ProjectCardDailyLimiter projectCardDailyLimiter;

    private CardEndService cardEndService;

    private WebHookProjectCmdService webHookProjectCmdService;

    private CardEventHelper cardEventHelper;

    private EventDispatcher eventDispatcher;

    private CardTestPlanService cardTestPlanService;

    private CardDocService cardDocService;

    private CardWikiPageService cardWikiPageService;

    private PermissionService permissionService;

    private OperationLogCmdService operationLogCmdService;

    private CardBpmCmdService cardBpmCmdService;
    private CardBpmQueryService cardBpmQueryService;

    private CardWorkloadBpmCmdService cardWorkloadBpmCmdService;
    private CardWorkloadBpmQueryService cardWorkloadBpmQueryService;

    private TransactionHelper transactionHelper;

    @Getter(lazy = true)
    private final CardField cardFieldParentId = schemaHelper.getSysProjectCardSchema().findCardField(CardField.PARENT_ID);

    public Card completelyCreate(Long projectId, Long draftId, Map<String, Object> cardDetail, List<Long> relateCardIds, Boolean sendEmail) throws IOException {
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        // remove invalid props; check type&status;
        schemaHelper.preProcessCardProps(schema, cardDetail);
        sysFieldOpLimitPreProcess(cardDetail, SysFieldOpLimit.Op.CREATE);
        // check fields
        ProjectWorkloadSetting workloadSetting = projectSchemaQueryService.getProjectWorkloadSetting(projectId);
        ActualWorkloadChecker.builder()
                .workloadSetting(workloadSetting)
                .build()
                .check(cardDetail);
        CardFieldStatusesChecker.builder()
                .schema(schema)
                .fromCardDetail(null)
                .build()
                .check(cardDetail);
        // check fields values
        FieldValueCheckHelper checker = FieldValueCheckHelper.builder()
                .projectId(projectId)
                .findCardById(cardQueryService::select)
                .findPlanById(planQueryService::select)
                .findStoryMapNodeById(storyMapQueryService::selectStoryMapNodeById)
                .build();
        cardDetail.entrySet().forEach(entry -> checker.check(schema.findCardField(entry.getKey()), entry.getValue()));
        // create card
        CardDraft draft = null;
        if (draftId != null && draftId > 0) {
            draft = cardDraftQueryService.select(draftId);
        }
        Card card = create(projectId, draft == null ? IdUtil.generateId() : draftId, cardDetail, sendEmail);
        cardRelateRelCmdService.initCardRelateRel(userService.currentUserName(), card.getId(), relateCardIds);
        if (draft != null) {
            // cardAttachmentCmdService.commitDraftAttachment(draftId, card.getId());
            cardDraftCmdService.onCommit(draftId);
        }
        return card;
    }

    private void sysFieldOpLimitPreProcess(Map<String, Object> cardDetail, SysFieldOpLimit.Op op) {
        Iterator<Map.Entry<String, Object>> it = cardDetail.entrySet().iterator();
        while (it.hasNext()) {
            if (!SysFieldOpLimit.canOp(it.next().getKey(), SysFieldOpLimit.Op.CREATE)) {
                it.remove();
            }
        }
    }

    public Card quickCreate(Long projectId, QuickCreateCardRequest request, Boolean sendEmail) throws IOException {
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        // check type&title;
        CardType cardType = schema.findCardType(request.getType());
        if (null == cardType || !cardType.isEnable()) {
            throw CodedException.ERROR_CARD_TYPE;
        }
        String title = request.getTitle();
        if (StringUtils.isEmpty(title)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "标题不能为空!");
        }
        // set default status
        String status = cardType.getStatuses().get(0).getKey();
        Map<String, Object> cardDetail = new HashMap<>();
        cardDetail.put(CardField.TYPE, request.getType());
        cardDetail.put(CardField.TITLE, request.getTitle());
        cardDetail.put(CardField.STATUS, status);
        cardDetail.put(CardField.PLAN_ID, request.getPlanId());
        cardDetail.put(CardField.PARENT_ID, request.getParentId());
        cardDetail.put(CardField.STORY_MAP_NODE_ID, request.getStoryMapNodeId());
        //
        cardDetail.put(CardField.CONTENT, FieldUtil.getContent(projectCardTemplateService.getProjectCardTemplate(projectId, request.getType())));
        // create
        return create(projectId, cardDetail, sendEmail);
    }

    public ExcelCardImport.Result importByExcel(Long projectId, Long defaultPlanId, List<String> cardTypes, InputStream excel) {
        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();
        Project project = projectQueryService.select(projectId);
        Plan defaultPlan = fieldRefValueLoader.plan(defaultPlanId);
        if (defaultPlan != null && !defaultPlan.getProjectId().equals(projectId)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "计划不在项目下");
        }
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        if (CollectionUtils.isNotEmpty(cardTypes)) {
            cardTypes.forEach(cardType -> {
                CardType cardTypeObj = schema.findCardType(cardType);
                if (null == cardTypeObj || !cardTypeObj.isEnable()) {
                    throw CodedException.ERROR_CARD_TYPE;
                }
            });
        }
        Function<String, Plan> findPlanByName = s -> defaultPlan;
        List<Plan> plans = planQueryService.selectByProjectId(projectId, true);
        if (CollectionUtils.isNotEmpty(plans)) {
            plans.forEach(plan -> fieldRefValueLoader.getLoadPlan().cache(plan.getId(), plan));
            Map<String, Plan> planNameIdMap = plans.stream()
                    .collect(Collectors.toMap(Plan::getName, p -> p, (p1, p2) -> p1));
            findPlanByName = s -> {
                Plan plan = planNameIdMap.get(s);
                return plan == null ? defaultPlan : plan;
            };
        }
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(project.getCompanyId());
        OperationContext opContext = OperationContext.instance(userService.currentUserName());
        Consumer<Map<String, Object>> setCalcFields = cardDetail -> FieldCalcGraph.INSTANCE.calcCreate(cardDetail, opContext, schema, fieldRefValueLoader);
        ProjectWorkloadSetting workloadSetting = projectSchemaQueryService.getProjectWorkloadSetting(projectId);
        ExcelCardImport cardImport = ExcelCardImport.builder()
                .permissions(permissions(projectId))
                .project(fieldRefValueLoader.project(projectId))
                .schema(schema)
                .cardTypes(cardTypes)
                .workloadSetting(workloadSetting)
                .opContext(opContext)
                .setCalcFields(setCalcFields)
                .batchSize(100)
                .cardMapper(cardMapper)
                .cardDao(cardDao)
                .cardHelper(cardHelper)
                .findPlanByName(findPlanByName)
                .eventDispatcher(eventDispatcher)
                .storage(storage)
                .projectCardDailyLimiter(projectCardDailyLimiter)
                .companyCardSchema(companyCardSchema)
                .build();
        return cardImport.importExcel(excel);
    }

    public void completelyUpdate(Card card, Map<String, Object> cardDetail) throws IOException {
        LoginUser user = userService.currentUser();
        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();
        OperationContext opContext = OperationContext.instance(user.getUsername());
        // Consumer<Map<String, Object>> setCalcFields = baseCardDetail -> FieldCalcGraph.INSTANCE.calcCreate(baseCardDetail, opContext, schema, fieldRefValueLoader);
        Long projectId = card.getProjectId();
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        Function<Long, Plan> findPlanById = fieldRefValueLoader.getLoadPlan();
        Function<Long, Card> findCardById = CacheableFunction.instance(cardQueryService::select);
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(card.getCompanyId());
        ProjectWorkloadSetting workloadSetting = projectSchemaQueryService.getProjectWorkloadSetting(projectId);
        BiConsumer<Map<String, Object>, List<FieldChange>> setCalcFields = (baseCardDetail, fieldChanges) -> FieldCalcGraph.INSTANCE.calcUpdate(
                baseCardDetail,
                fieldChanges.stream().map(fc -> fc.getField().getKey()).collect(Collectors.toSet()),
                opContext,
                schema,
                fieldRefValueLoader);
        eventDispatcher.dispatch(CompletelyFieldsUpdateHelper.builder()
                .opContext(opContext)
                .setCalcFields(setCalcFields)
                .companyCardSchema(companyCardSchema)
                .userService(userService)
                .schema(schema)
                .workloadSetting(workloadSetting)
                .schemaHelper(schemaHelper)
                .cardDao(cardDao)
                .cardMapper(cardMapper)
                .findCardById(findCardById)
                .findCardDescendant(cardQueryService::selectDescendant)
                .findPlanById(findPlanById)
                .findStoryMapNodeById(storyMapQueryService::selectStoryMapNodeById)
                .findStoryMapL2NodeInfoById(storyMapQueryService::selectStoryMapL2NodeInfoById)
                .card(card)
                .cardDetail(cardDetail)
                .checkProps(true)
                .cardNoticeService(cardNoticeService)
                .cardEndService(cardEndService)
                .cardHelper(cardHelper)
                .operationLogCmdService(operationLogCmdService)
                .systemSettingService(systemSettingService)
                .cardBpmCmdService(cardBpmCmdService)
                .build()
                .update());
        Project project = projectQueryService.select(projectId);
        webHookProjectCmdService.asyncSendCardWebHookMsg(cardDao.findAsMap(card.getId()), project, WebHookEventType.UPDATE_CARD, companyService.currentCompanyName(), user);
    }

    @SuppressWarnings("AlibabaTransactionMustHaveRollback")
    @Transactional()
    public void batchUpdateField(List<Card> cards, Long projectId, String field, String value) throws IOException {
        if (CollectionUtils.isEmpty(cards)) {
            return;
        }
        if (CardField.PARENT_ID.equals(field)) {
            Function<Long, Plan> findPlanById = CacheableFunction.instance(planQueryService::select);
            Function<Long, Card> findCardById = CacheableFunction.instance(cardQueryService::select);
            LoginUser user = userService.currentUser();
            Map<Long, Map<String, Object>> cardDetails = cardDao.findAsMap(cards.stream().map(Card::getId).collect(Collectors.toList()));
            CardsUpdateEvent cardsUpdateEvent = SingleFieldBatchUpdateHelper.builder()
                    .user(user.getUsername())
                    .schema(projectSchemaQueryService.getProjectCardSchema(projectId))
                    .cardDao(cardDao)
                    .cardMapper(cardMapper)
                    .findCardById(findCardById)
                    .findCardsDescendant(cardQueryService::selectDescendant)
                    .findCardsAncestor(cardQueryService::selectAncestor)
                    .findPlanById(findPlanById)
                    .findStoryMapNodeById(storyMapQueryService::selectStoryMapNodeById)
                    .findStoryMapL2NodeInfoById(storyMapQueryService::selectStoryMapL2NodeInfoById)
                    .cards(cards)
                    .cardsJson(cardDetails)
                    .projectQueryService(projectQueryService)
                    .projectId(projectId)
                    .build()
                    .update(field, value);
            eventDispatcher.asyncDispatch(() -> cardsUpdateEvent);
            Project project = projectQueryService.select(projectId);
            webHookProjectCmdService.asyncSendCardsWebHookMsg(cardDetails, project, WebHookEventType.UPDATE_CARD, companyService.currentCompanyName(), user);
        } else {
            throw new CodedException(HttpStatus.BAD_REQUEST, String.format("%s字段暂不支持批量修改!", field));
        }
    }

    @Transactional(propagation = Propagation.NEVER)
    public Map<Long, String> batchUpdateFields(Long projectId, List<Card> allCards, UpdateCardsFieldsRequest request) throws IOException {
        LoginUser user = userService.currentUser();
        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();
        Project project = fieldRefValueLoader.project(projectId);
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(project.getCompanyId());
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        ProjectWorkloadSetting workloadSetting = projectSchemaQueryService.getProjectWorkloadSetting(projectId);
        FieldOperationsChecker.builder()
                .companyCardSchema(companyCardSchema)
                .projectCardSchema(schema)
                .workloadSetting(workloadSetting)
                .typeFieldOperations(request.getTypeMap())
                .build()
                .check();
        UserProjectPermissions permissions = permissions(projectId);
        ResultCollector collector = new ResultCollector();
        Function<Long, Plan> findPlanById = CacheableFunction.instance(planQueryService::select);
        Function<Long, Card> findCardById = CacheableFunction.instance(cardQueryService::select);
        // partion
        for (List<Card> cards : ListUtils.partition(allCards, 1000)) {
            Map<Long, Map<String, Object>> cardDetails = cardDao.findAsMap(cards.stream().map(Card::getId).collect(Collectors.toList()));
            OperationContext opContext = OperationContext.instance(user.getUsername());
            BiConsumer<Map<String, Object>, List<FieldChange>> setCalcFields = (baseCardDetail, fieldChanges) -> FieldCalcGraph.INSTANCE.calcUpdate(
                    baseCardDetail,
                    fieldChanges.stream().map(fc -> fc.getField().getKey()).collect(Collectors.toSet()),
                    opContext,
                    schema,
                    fieldRefValueLoader);
            transactionHelper.runWithRequiresNew(() ->
                    eventDispatcher.asyncDispatch(BatchCardsFieldsUpdateHelper.builder()
                            .opContext(opContext)
                            .setCalcFields(setCalcFields)
                            .project(project)
                            .schema(schema)
                            .permissions(permissions)
                            .cards(cards)
                            .cardDetails(cardDetails)
                            .typeOpsMap(request.getTypeMap())
                            .resultCollector(collector)
                            .schemaHelper(schemaHelper)
                            .cardDao(cardDao)
                            .cardMapper(cardMapper)
                            .findCardById(findCardById)
                            .findCardDescendant(cardQueryService::selectDescendant)
                            .findPlanById(findPlanById)
                            .findStoryMapNodeById(storyMapQueryService::selectStoryMapNodeById)
                            .findStoryMapL2NodeInfoById(storyMapQueryService::selectStoryMapL2NodeInfoById)
                            .cardNoticeService(cardNoticeService)
                            .cardEndService(cardEndService)
                            .cardHelper(cardHelper)
                            .build()
                            .update())
            );
        }
        return collector.getFailures();
    }

    // 四舍五入RoundingMode.HALF_EVEN（统计均匀分布数据的情况舍入更公平精确）改为RoundingMode.HALF_UP（方便前端保持一致）
    private static final Function<Float, Float> ROUND_DOUBLE_DECIMAL =
            f -> BigDecimal.valueOf(f).setScale(2, RoundingMode.HALF_UP).floatValue();

    public Float incrActualWorkload(Card card, Map<String, Object> cardDetail, float incrHours) {
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(card.getProjectId());
        return incrActualWorkload(schema, card, cardDetail, incrHours);
    }

    public Float incrActualWorkload(ProjectCardSchema schema, Card card, Map<String, Object> cardDetail, float incrHours) {
        if (incrHours == 0f) {
            return FieldUtil.getActualWorkload(cardDetail);
        }
        if (FieldUtil.getDeleted(cardDetail)) {
            throw CodedException.DELETED;
        }
        Float actualWorkload = FieldUtil.getActualWorkload(cardDetail);
        if (actualWorkload == null) {
            actualWorkload = incrHours;
        } else {
            actualWorkload += incrHours;
        }
        actualWorkload = ROUND_DOUBLE_DECIMAL.apply(actualWorkload);
        Map<String, Object> cardProps = new HashMap<>();
        cardProps.put(CardField.ACTUAL_WORKLOAD, actualWorkload);
        try {
            updateFields(schema, card, cardProps, false);
        } catch (IOException e) {
            log.error("incrActualWorkload exception!", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        return actualWorkload;
    }

    public void updateField(Card card, String fieldKey, String valueStr) throws IOException {
        if (BooleanUtils.isTrue(card.getDeleted())) {
            throw CodedException.DELETED;
        }
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(card.getProjectId());
        CardField field = schema.findCardField(fieldKey);
        if (field == null) {
            throw new CodedException(HttpStatus.NOT_FOUND, String.format("未找到字段[%s]！", fieldKey));
        }
        if (!SysFieldOpLimit.canOp(field, SysFieldOpLimit.Op.UPDATE)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE,
                    String.format("不能直接修改字段:[%s]!", field.getName()));
        }
        Object value = FieldUtil.parse(
                field.getValueType() == null ? field.getType().getDefaultValueType() : field.getValueType(),
                valueStr);
        Map<String, Object> cardProps = new HashMap<>();
        cardProps.put(fieldKey, value);
        updateFields(schema, card, cardProps, true);
    }

    public void updateFields(Card card, Map<String, Object> cardProps) throws IOException {
        if (BooleanUtils.isTrue(card.getDeleted())) {
            throw CodedException.DELETED;
        }
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(card.getProjectId());
        updateFields(schema, card, cardProps, true);
    }

    private void updateFields(ProjectCardSchema schema, Card card, Map<String, Object> cardProps, boolean checkProps) throws IOException {
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(card.getCompanyId());
        LoginUser user = userService.currentUser();
        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();
        Long projectId = card.getProjectId();
        OperationContext opContext = OperationContext.instance(user.getUsername());
        Function<Long, Plan> findPlanById = fieldRefValueLoader.getLoadPlan();
        Function<Long, Card> findCardById = CacheableFunction.instance(cardQueryService::select);
        BiConsumer<Map<String, Object>, List<FieldChange>> setCalcFields = (baseCardDetail, fieldChanges) -> FieldCalcGraph.INSTANCE.calcUpdate(
                baseCardDetail,
                fieldChanges.stream().map(fc -> fc.getField().getKey()).collect(Collectors.toSet()),
                opContext,
                schema,
                fieldRefValueLoader);
        ProjectWorkloadSetting workloadSetting = projectSchemaQueryService.getProjectWorkloadSetting(projectId);
        eventDispatcher.dispatch(MultiFieldsUpdateHelper.builder()
                .opContext(opContext)
                .setCalcFields(setCalcFields)
                .schema(schema)
                .workloadSetting(workloadSetting)
                .schemaHelper(schemaHelper)
                .cardDao(cardDao)
                .cardMapper(cardMapper)
                .findCardById(findCardById)
                .findCardDescendant(cardQueryService::selectDescendant)
                .findPlanById(findPlanById)
                .findStoryMapNodeById(storyMapQueryService::selectStoryMapNodeById)
                .findStoryMapL2NodeInfoById(storyMapQueryService::selectStoryMapL2NodeInfoById)
                .card(card)
                .cardDetail(cardProps)
                .checkProps(checkProps)
                .cardNoticeService(cardNoticeService)
                .cardEndService(cardEndService)
                .cardHelper(cardHelper)
                .operationLogCmdService(operationLogCmdService)
                .systemSettingService(systemSettingService)
                .cardBpmCmdService(cardBpmCmdService)
                .companyCardSchema(companyCardSchema)
                .build()
                .update());
        Map<String, Object> cardDetail = cardDao.findAsMap(card.getId());
        Project project = projectQueryService.select(projectId);
        webHookProjectCmdService.asyncSendCardWebHookMsg(cardDetail, project, WebHookEventType.UPDATE_CARD, companyService.currentCompanyName(), user);
    }

    public void forceSetStatus(ProjectCardSchema schema, Card card, Map<String, Object> cardDetail, String status) throws IOException {
        String oldStatus = FieldUtil.toString(cardDetail.get(CardField.STATUS));
        LoginUser user = userService.currentUser();
        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();
        OperationContext opContext = OperationContext.instance(user.getUsername());
        Function<Long, Plan> findPlanById = fieldRefValueLoader.getLoadPlan();
        Function<Long, Card> findCardById = CacheableFunction.instance(cardQueryService::select);
        String type = FieldUtil.toString(cardDetail.get(CardField.TYPE));
        CardType cardType = schema.findCardType(type);
        // update es

        cardDetail = CardHelper.generatePropsForUpdate(cardDetail, opContext, CardField.STATUS, status);
        FieldCalcGraph.INSTANCE.calcUpdate(cardDetail, SetUtils.hashSet(CardField.STATUS), opContext, schema, fieldRefValueLoader);
        cardDao.saveOrUpdate(card.getId(), cardDetail);
        // event
        eventDispatcher.dispatch(UpdateEventHelper.builder()
                .findPlanById(findPlanById)
                .findCardById(findCardById)
                .findStoryMapL2NodeInfoById(storyMapQueryService::selectStoryMapL2NodeInfoById)
                .build()
                .cardUpdateEvent(
                        card,
                        user.getUsername(),
                        FieldChange.builder()
                                .field(schema.findCardField(CardField.STATUS))
                                .fromValue(oldStatus)
                                .toValue(status)
                                .build(),
                        cardHelper.generatePropsForUpdateStatus(cardDetail, opContext, status, schema)
                ));
        // notice
        cardNoticeService.noticeStatusFlow(user.getUsername(), cardDetail, schema, oldStatus, status);
        // cardEnd
        if (CardHelper.isChangeToEnd(schema, type, oldStatus, status)) {
            cardEndService.cardEnd(FieldUtil.toStringList(cardDetail.get(CardField.OWNER_USERS)), card);
        }
        Project project = projectQueryService.select(card.getProjectId());
        webHookProjectCmdService.asyncSendCardWebHookMsg(cardDetail, project, WebHookEventType.UPDATE_CARD, companyService.currentCompanyName(), user);
    }

    private void inactiveRelateField(Card card) {
        card.setDeleted(true);
        card.setParentId(0L);
        card.setAncestorId(0L);
        card.setPlanId(0L);
        card.setStoryMapNodeId(0L);
    }

    private Map<String, Object> inactiveRelateFieldProps(String user) {
        Map<String, Object> cardProps = CardHelper.generatePropsForUpdate(user, CardField.DELETED, true);
        cardProps.put(CardField.RELATED_CARD_IDS, null);
        cardProps.put(CardField.PARENT_ID, 0L);
        cardProps.put(CardField.PLAN_ID, 0L);
        cardProps.put(CardField.PLAN_IS_ACTIVE, true);
        cardProps.put(CardField.STORY_MAP_NODE_ID, 0L);
        cardProps.put(CardField.BPM_FLOW_ID, null);
        cardProps.put(CardField.BPM_FLOW_TO_STATUS, null);
        return cardProps;
    }

    public void inactive(Card card) throws IOException {
        LoginUser user = userService.currentUser();
        OperationContext opContext = OperationContext.instance(user.getUsername());
        Project project = projectQueryService.select(card.getProjectId());
        promoteForChildren(user.getUsername(), project, Arrays.asList(card));
        inactiveRelateField(card);
        cardMapper.updateByPrimaryKey(card);
        Map<String, Object> cardDetail = cardDao.findAsMap(card.getId());
        Long flowId = FieldUtil.getBpmFlowId(cardDetail);
        cardBpmCmdService.asyncCancelFlow(flowId, user.getUsername());
        Map<String, Object> cardProps = inactiveRelateFieldProps(user.getUsername());
        cardDao.updateSelective(card.getId(), cardProps);
        cardDetail.putAll(cardProps);
        List<Long> relatedCardIds = cardRelateRelQueryService.selectRelateCardIds(card.getId());
        if (CollectionUtils.isNotEmpty(relatedCardIds)) {
            cardRelateRelCmdService.deleteAll(card.getId());
            List<Card> cards = cardQueryService.select(relatedCardIds);
            cards.stream().collect(Collectors
                            .groupingBy(Card::getProjectId, Collectors.mapping(Card::getId, Collectors.toList())))
                    .entrySet().forEach(e -> rmRelatedCardIdFromCardDetail(e.getKey(), e.getValue(), card.getId()));
        }
        eventDispatcher.dispatch(CardDeleteEvent.builder()
                .user(user.getUsername())
                .card(card)
                .cardDetail(cardDetail)
                .build());
        webHookProjectCmdService.asyncSendCardWebHookMsg(cardDetail, project, WebHookEventType.DEL_CARD, companyService.currentCompanyName(), user);
        operationLogCmdService.deleteCard(opContext, card, cardDetail);
    }

    /**
     * Map<Long: FromParent, Card{id, ancestorId}: ToParent>
     */
    private Map<Long, Card> promoteForChildren(String user, Project project, List<Card> cards) throws IOException {
        return TreePromoteForDelete.builder()
                .user(user)
                .cards(cards)
                .cardFieldParentId(getCardFieldParentId())
                .cardQueryService(cardQueryService)
                .cardMapper(cardMapper)
                .cardDao(cardDao)
                .project(project)
                .eventDispatcher(eventDispatcher)
                .build()
                .promote();
    }

    public int inactive(Long projectId, List<Card> cards) throws IOException {
        cards = cards.stream().filter(card -> BooleanUtils.isNotTrue(card.getDeleted())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(cards)) {
            return 0;
        }
        UserProjectPermissions permissions = permissions(projectId);
        cards = canOperationCards(permissions, cards, cardProps -> permissions.hasLimitPermission(OperationType.CARD_DELETE, cardProps));
        if (CollectionUtils.isEmpty(cards)) {
            return 0;
        }
        LoginUser user = userService.currentUser();
        OperationContext opContext = OperationContext.instance(user.getUsername());
        Project project = projectQueryService.select(projectId);
        promoteForChildren(user.getUsername(), project, cards);
        List<Long> ids = new ArrayList<>();
        cards.stream().forEach(card -> {
            inactiveRelateField(card);
            cardMapper.updateByPrimaryKey(card);
            ids.add(card.getId());
        });
        List<Long> bpmFlowIds = cardDao
                .findAsMap(
                        cards.stream().map(Card::getId).collect(Collectors.toList()),
                        CardField.BPM_FLOW_ID)
                .values()
                .stream()
                .map(FieldUtil::getBpmFlowId)
                .filter(flowId -> flowId != null && flowId > 0)
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(bpmFlowIds)) {
            cardBpmCmdService.asyncCancelFlows(bpmFlowIds, user.getUsername());
        }
        Map<String, Object> cardProps = inactiveRelateFieldProps(user.getUsername());
        cardDao.updateSelective(ids, cardProps);
        List<CardRelateRel> rels = cardRelateRelQueryService.selectByCardId(ids);
        if (CollectionUtils.isNotEmpty(rels)) {
            cardRelateRelCmdService.deleteAll(ids);
            Map<Long, List<Long>> relateCardRelMap = rels.stream().collect(Collectors.groupingBy(
                    r -> r.getRelatedCardId(), Collectors.mapping(r -> r.getCardId(), Collectors.toList())));
            List<Card> relateCards = cardQueryService.select(new ArrayList<>(relateCardRelMap.keySet()));
            if (CollectionUtils.isNotEmpty(relateCards)) {
                relateCards.forEach(card -> rmRelatedCardIdFromCardDetail(card.getProjectId(), Arrays.asList(card.getId()), relateCardRelMap.get(card.getId())));
            }
        }
        Map<Long, Map<String, Object>> deleteCardDetails = cardDao.findAsMap(ids);
        eventDispatcher.dispatch(CardsDeleteEvent.builder()
                .user(user.getUsername())
                .project(project)
                .cardDetails(deleteCardDetails)
                .build());
        webHookProjectCmdService.asyncSendCardsWebHookMsg(deleteCardDetails, project, WebHookEventType.DEL_CARD, companyService.currentCompanyName(), user);
        operationLogCmdService.deleteCards(opContext, cards, deleteCardDetails);
        return ids.size();
    }

    public void recovery(Card card) throws IOException {
        card.setDeleted(false);
        cardMapper.updateByPrimaryKey(card);
        LoginUser user = userService.currentUser();
        cardDao.updateSelective(card.getId(),
                CardHelper.generatePropsForUpdate(user.getUsername(), CardField.DELETED, false));
        Map<String, Object> cardDetail = cardDao.findAsMap(card.getId());
        eventDispatcher.asyncDispatch(() -> CardRecoveryEvent.builder()
                .user(user.getUsername())
                .card(card)
                .cardDetail(cardDetail)
                .build());
        Project project = projectQueryService.select(card.getProjectId());
        webHookProjectCmdService.asyncSendCardWebHookMsg(cardDetail, project, WebHookEventType.RECOVERY, companyService.currentCompanyName(), user);
    }

    public void recovery(Long projectId, List<Card> cards) throws IOException {
        cards = cards.stream().filter(card -> BooleanUtils.isTrue(card.getDeleted())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(cards)) {
            return;
        }
        List<Long> ids = new ArrayList<>();
        cards.forEach(card -> {
            card.setDeleted(false);
            cardMapper.updateByPrimaryKey(card);
            ids.add(card.getId());
        });
        LoginUser user = userService.currentUser();
        cardDao.updateSelective(ids,
                CardHelper.generatePropsForUpdate(user.getUsername(), CardField.DELETED, false));
        Project project = projectQueryService.select(projectId);
        Map<Long, Map<String, Object>> cardDetails = cardDao.findAsMap(ids);
        eventDispatcher.dispatch(CardsRecoveryEvent.builder()
                .user(user.getUsername())
                .project(project)
                .cardDetails(cardDetails)
                .build());
        webHookProjectCmdService.asyncSendCardsWebHookMsg(cardDetails, project, WebHookEventType.RECOVERY, companyService.currentCompanyName(), user);
    }

    /**
     * 卡片可能太多，故做了分批处理，每批有事务，整体不要事务
     *
     * @param projectId
     * @throws IOException
     */
    @Transactional(propagation = Propagation.NEVER)
    public void clean(Long projectId) throws IOException {
        Project project = projectQueryService.select(projectId);
        if (null == project || project.getKeepDays() <= 0) {
            return;
        }
        Date date = DateUtils.addDays(new Date(), -project.getKeepDays().intValue());
        if (project.getCreateTime().after(date)) {
            return;
        }
        List<Long> ids = cardDao.searchIds(Arrays.asList(
                Eq.builder().field(CardField.PROJECT_ID).value(String.valueOf(projectId)).build(),
                Eq.builder().field(CardField.DELETED).value("true").build(),
                Lt.builder().field(CardField.LAST_MODIFY_TIME).value(String.valueOf(date.getTime())).build()));
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        // 需要代理类的事务能力，故不直接调用this.delete
        CardCmdService cardCmdService = Application.context().getBean(CardCmdService.class);
        ListUtils.partition(ids, 1000).forEach(batchIds -> {
            try {
                cardCmdService.delete(project.getCompanyId(), batchIds);
            } catch (Exception e) {
                log.error(String.format("Delete batch cards:[%s] exception!", batchIds), e);
            }
        });
    }

    public void deleteByProject(Long projectId) throws IOException {
        List<Long> ids = cardDao.searchIds(Arrays.asList(
                Eq.builder().field(CardField.PROJECT_ID).value(String.valueOf(projectId)).build()));
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        ListUtils.partition(ids, 1000).forEach(batchIds -> {
            try {
                delete(projectQueryService.getProjectCompany(projectId), batchIds);
            } catch (Exception e) {
                log.error(String.format("Delete batch cards:[%s] exception!", batchIds), e);
            }
        });
    }

    public void delete(Long company, List<Long> ids) throws IOException {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        CardExample cardExample = new CardExample();
        cardExample.createCriteria().andIdIn(ids);
        cardMapper.deleteByExample(cardExample);
        cardDao.delete(ids);
        CardTokenExample cardTokenExample = new CardTokenExample();
        cardTokenExample.createCriteria().andCardIdIn(ids);
        cardTokenMapper.deleteByExample(cardTokenExample);
        cardAttachmentCmdService.delete(company, ids);
        cardEventCmdService.deleteByCardIds(ids);
        cardCommentService.deleteByCardIds(ids);
        cardTestPlanService.deleteByCardIds(ids);
        cardDocService.deleteByCardIds(ids);
        cardWikiPageService.deleteByCardIds(ids);
        cardBpmCmdService.deleteByCardIds(ids);
        cardWorkloadBpmCmdService.deleteByCardIds(ids);
    }

    public int migrate(List<Card> cards, Long targetPlanId, RankLocation location) throws IOException {
        if (CollectionUtils.isEmpty(cards)) {
            return 0;
        }
        final Long projectId = cards.get(0).getProjectId();
        UserProjectPermissions permissions = permissions(projectId);
        cards = canOperationCards(permissions, cards, cardProps -> permissions.hasLimitPermission(OperationType.CARD_UPDATE, cardProps, CardField.PLAN_ID, targetPlanId));
        if (CollectionUtils.isEmpty(cards)) {
            return 0;
        }

        Map<Long, Long> fromCardPlanIds = new HashMap<>();
        List<Card> changePlanCards = new ArrayList<>();
        List<String> ranks = targetPlanCardRanks(projectId, targetPlanId, location, cards.size());


        Map<Long, Card> cardMap = cards.stream().collect(Collectors.toMap(Card::getId, c -> c));
        List<Long> ids = new ArrayList<>(cardMap.keySet());

        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            if (card.getPlanId().equals(targetPlanId)) {
                if (CollectionUtils.isNotEmpty(ranks)) {
                    card.setRank(ranks.get(i));
                    cardMapper.updateByPrimaryKey(card);
                }
                continue;
            }
            fromCardPlanIds.put(card.getId(), card.getPlanId());
            card.setPlanId(targetPlanId);
            if (CollectionUtils.isNotEmpty(ranks)) {
                card.setRank(ranks.get(i));
            }
            cardMapper.updateByPrimaryKey(card);
            changePlanCards.add(card);
        }
        if (CollectionUtils.isEmpty(changePlanCards)) {
            return cards.size();
        }

        LoginUser user = userService.currentUser();
        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        String fieldName = schema.findCardField(CardField.PLAN_ID).getName();

        Set<Long> planIds = new HashSet<>(fromCardPlanIds.values());
        if (targetPlanId != null) {
            planIds.add(targetPlanId);
        }
        Map<Long, Plan> plans = planQueryService.select(new ArrayList<>(planIds)).stream().collect(Collectors.toMap(Plan::getId, p -> p));
        fieldRefValueLoader.getLoadPlan().cache(plans);
        Function<Long, String> planName = planId -> {
            Plan plan = fieldRefValueLoader.plan(planId);
            return plan == null ? null : plan.getName();
        };
        String toPlanName = planName.apply(targetPlanId);
        Project project = projectQueryService.select(projectId);
        ListUtils.partition(ids, 1000).forEach(batchCardIds -> {
            try {
                OperationContext opContext = OperationContext.instance(user.getUsername());
                Consumer<Map<String, Object>> setCalcFields = (baseCardDetail) -> FieldCalcGraph.INSTANCE.calcUpdate(
                        baseCardDetail,
                        SetUtils.hashSet(CardField.STORY_MAP_NODE_ID),
                        opContext,
                        schema,
                        fieldRefValueLoader);
                List<CardEvent> cardEvents = new ArrayList<>();
                Map<Long, Map<String, Object>> cardDetails = cardDao.findAsMap(batchCardIds);
                cardDetails.entrySet().forEach(entry -> {
                    Map<String, Object> cardDetail = entry.getValue();
                    String fromPlanName = planName.apply(FieldUtil.getPlanId(cardDetail));
                    CardHelper.generatePropsForUpdate(cardDetail, opContext, CardField.PLAN_ID, targetPlanId);
                    setCalcFields.accept(entry.getValue());
                    CardEvent cardEvent = CardEvent.builder()
                            .id(IdUtil.generateId())
                            .cardId(entry.getKey())
                            .date(opContext.getTime())
                            .user(opContext.getUserName())
                            .eventType(EventType.UPDATE)
                            .eventMsg(UpdateEventMsg.builder()
                                    .fieldDetailMsg(UpdateEventMsg.FieldDetailMsg.builder()
                                            .fieldKey(CardField.PLAN_ID)
                                            .fieldMsg(fieldName)
                                            .fromMsg(fromPlanName)
                                            .toMsg(toPlanName)
                                            .build())
                                    .build())
                            .cardDetail(cardDetail)
                            .build();
                    cardEvents.add(cardEvent);
                });
                cardDao.saveOrUpdate(cardDetails);
                eventDispatcher.asyncDispatch(() -> CardsUpdateEvent.builder()
                        .project(project)
                        .user(user.getUsername())
                        .cardEvents(cardEvents)
                        .build());
                webHookProjectCmdService.asyncSendCardsWebHookMsg(cardDetails, project, WebHookEventType.UPDATE_CARD, companyService.currentCompanyName(), user);
            } catch (Exception e) {
                log.error(String.format("Migrate close status for batch cards:[%s] exception!", batchCardIds), e);
            }
        });
        return cards.size();
    }

    private List<String> targetPlanCardRanks(Long projectId, Long planId, RankLocation location, int num) {
        if (location == null) {
            return null;
        }
        Card referenceCard = null;
        switch (location) {
            case HIGHER:
                referenceCard = cardQueryService.selectHighestRankCard(projectId, planId);
                break;
            case LOWER:
                referenceCard = cardQueryService.selectLowestRankCard(projectId, planId);
                break;
            default:
                break;
        }
        if (referenceCard == null) {
            return null;
        }
        return cardHelper.ranks(projectId, referenceCard.getRank(), location, num);
    }

    public Map<String, Object> changeCardType(Long projectId, ChangeTypeRequest changeTypeRequest) throws IOException {
        Map<String, Object> result = new HashMap<>();

        List<String> errMessages = new ArrayList<>();
        List<Long> ids = changeTypeRequest.getIds();
        Map<String, ChangeTypeRequest.TypeChangeConfig> typeChangeConfigMap = changeTypeRequest.getTypeMap();
        Map<String, String> typeMap = typeChangeConfigMap.entrySet().stream().map(entity -> new AbstractMap.SimpleEntry<>(entity.getKey(), entity.getValue().getToType()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        List<Card> cards = cardQueryService.select(ids);
        Project project = projectQueryService.select(projectId);
        if (CollectionUtils.size(cards) < CollectionUtils.size(ids)) {
            throw new CodedException(HttpStatus.NOT_FOUND, "卡片未找到!");
        }
        for (Card card : cards) {
            if (!card.getProjectId().equals(projectId)) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "卡片属于不同项目!");
            }
        }
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        List<ChangeTypeCheckResult> checkResults = projectSchemaQueryService.checkSchemaForChangeType(projectId, typeMap);
        LoginUser user = userService.currentUser();
        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();
        OperationContext opContext = OperationContext.instance(user.getUsername());
        Map<Long, Map<String, Object>> cardDetails = cardDao.findAsMap(ids);
        Map<Long, String> cardIdFromTypeMap = new HashMap<>();
        Map<Long, String> cardIdFromStatusMap = new HashMap<>();
        UserProjectPermissions permissions = permissions(projectId);
        Map<Long, Map<String, Object>> finalCards = new HashMap<>();
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(project.getCompanyId());
        AtomicInteger approvalCount = new AtomicInteger(0);
        cardDetails.forEach((id, cardDetail) -> {
            String fromType = String.valueOf(cardDetail.get(CardField.TYPE));
            String fromStatus = String.valueOf(cardDetail.get(CardField.STATUS));
            ChangeTypeRequest.TypeChangeConfig typeChangeConfig = typeChangeConfigMap.get(fromType);
            if (typeChangeConfig == null) {
                throw new CodedException(HttpStatus.BAD_REQUEST, String.format("没有指定%s的转换配置", fromType));
            }

            String toType = typeChangeConfig.getToType();
            CardType cardType = schema.findCardType(toType);
            if (null == cardType || !cardType.isEnable()) {
                throw CodedException.ERROR_CARD_TYPE;
            }

            if (!permissions.hasLimitPermission(OperationType.CARD_DELETE, cardDetail)) {
                String msg = String.format("%s 卡片更新失败：%s", FieldUtil.getSeqNum(cardDetail), "无删除权限！");
                errMessages.add(msg);
                return;
            }

            if (!permissions.hasLimitPermission(OperationType.CARD_CREATE, cardDetail)) {
                String msg = String.format("%s 卡片更新失败：%s", FieldUtil.getSeqNum(cardDetail), "无创建权限！");
                errMessages.add(msg);
                return;
            }

            Long bpmFlowId = FieldUtil.getBpmFlowId(cardDetail);
            if (bpmFlowId != null && bpmFlowId > 0) {
                approvalCount.addAndGet(1);
                String msg = String.format("%s 卡片更新失败：%s", FieldUtil.getSeqNum(cardDetail), "待审批状态，不能修改类型！");
                errMessages.add(msg);
                return;
            }

            ChangeTypeCheckResult check = checkResults.stream()
                    .filter(c -> fromType.equals(c.getFromType()) && toType.equals(c.getToType()))
                    .findAny()
                    .get();
            Set<String> changeFields = new HashSet<>();
            check.getDisabledFields().forEach(field -> {
                if (!FieldUtil.isEmptyValue(cardDetail.get(field))) {
                    cardDetail.remove(field);
                    changeFields.add(field);
                }
            });

            cardDetail.put(CardField.TYPE, toType);
            changeFields.add(CardField.TYPE);

            String status = String.valueOf(cardDetail.get(CardField.STATUS));
            String toStatus = checkAndGetToStatus(status, typeChangeConfig, cardType.getStatuses());

            if (!toStatus.equals(status)) {
                cardDetail.put(CardField.STATUS, toStatus);
                changeFields.add(CardField.STATUS);
            }

            String fromCardTypeName = companyCardSchema.findCardTypeName(fromType);
            String toCardTypeName = companyCardSchema.findCardTypeName(toType);
            operationLogCmdService.updateCardDetailType(opContext, project, id, cardDetail, fromCardTypeName, toCardTypeName);

            cardDetail.put(CardField.LAST_MODIFY_USER, opContext.getUserName());
            cardDetail.put(CardField.LAST_MODIFY_TIME, opContext.getCurrentTimeMillis());

            cardIdFromTypeMap.put(id, fromType);
            cardIdFromStatusMap.put(id, fromStatus);
            FieldCalcGraph.INSTANCE.calcUpdate(cardDetail, changeFields, opContext, schema, fieldRefValueLoader);
            finalCards.put(id, cardDetail);
        });

        if (MapUtils.isNotEmpty(finalCards)) {
            cardDao.saveOrUpdate(finalCards);
            eventDispatcher.asyncDispatch(() -> CardsUpdateEvent.builder()
                    .project(project)
                    .user(user.getUsername())
                    .cardEvents(cardEventHelper.cardEventsForChangeType(finalCards, cardIdFromTypeMap, cardIdFromStatusMap, user.getUsername(), typeChangeConfigMap))
                    .build());
            webHookProjectCmdService.asyncSendCardsWebHookMsg(cardDao.findAsMap(ids), project, WebHookEventType.UPDATE_CARD, companyService.currentCompanyName(), user);
        }

        result.put("success", finalCards.size());
        result.put("err", errMessages.size());
        result.put("approvalCount", approvalCount.get());
        result.put("errMsg", errMessages);
        return result;
    }

    private String checkAndGetToStatus(String status, ChangeTypeRequest.TypeChangeConfig typeChangeConfig, List<CardType.StatusConf> statusConfs) {
        String toStatus = typeChangeConfig.getStatusMap().get(status);
        if (StringUtils.isEmpty(toStatus)) {
            throw new CodedException(HttpStatus.BAD_REQUEST, String.format("参数中未找到%s状态转换的目标状态", status));
        }
        Optional<CardType.StatusConf> statusConf = statusConfs.stream().filter(config -> config.getKey().equals(toStatus)).findAny();
        if (!statusConf.isPresent()) {
            throw new CodedException(HttpStatus.BAD_REQUEST, "转换目标状态错误");
        }
        return toStatus;
    }

    public int migrate(List<Long> ids, Long targetProjectId, Long targetPlanId, RankLocation location) throws IOException {
        List<Card> cards = cardQueryService.select(ids);
        if (CollectionUtils.isEmpty(cards)) {
            return 0;
        }
        if (targetPlanId > 0) {
            Plan targetPlan = planQueryService.select(targetPlanId);
            if (targetPlan == null) {
                throw new CodedException(HttpStatus.NOT_FOUND, "计划不存在!");
            }
            if (!targetPlan.getProjectId().equals(targetProjectId)) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "计划不在项目下!");
            }
        }
        cards.forEach(card -> {
            if (!card.getProjectId().equals(targetProjectId)) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "卡片不在同一项目下!");
            }
            if (BooleanUtils.isTrue(card.getDeleted())) {
                throw CodedException.DELETED;
            }
        });
        return migrate(cards.stream().collect(Collectors.toList()), targetPlanId, location);
    }

    public int copy(
            List<Long> ids, Long targetProjectId, Long targetPlanId, RankLocation location,
            boolean isDeleteSourceCards, BiConsumer<Project, OperationType> projectActiveChecker) throws IOException {
        if (location == null || location.equals(RankLocation.RETAIN)) {
            throw new CodedException(HttpStatus.NOT_FOUND, "非法的相对位置!");
        }
        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();
        Project targetProject = fieldRefValueLoader.getLoadProject().apply(targetProjectId);
        projectActiveChecker.accept(targetProject, OperationType.CARD_CREATE);
        List<Card> cards = cardQueryService.select(ids);
        if (CollectionUtils.isEmpty(cards)) {
            return 0;
        }

        Long fromProjectId = cards.get(0).getProjectId();
        if (cards.stream().anyMatch(card -> !card.getProjectId().equals(fromProjectId))) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "卡片不在同一项目下!");
        }
        Project fromProject = fieldRefValueLoader.getLoadProject().apply(fromProjectId);
        if (isDeleteSourceCards) {
            projectActiveChecker.accept(fromProject, OperationType.CARD_DELETE);
        }

        UserProjectPermissions permissions = permissions(fromProjectId);
        Long fromPlanId = cards.get(0).getPlanId();
        boolean planIsActive = true;
        if (targetPlanId > 0) {
            Plan targetPlan = fieldRefValueLoader.getLoadPlan().apply(targetPlanId);
            if (targetPlan == null) {
                throw new CodedException(HttpStatus.NOT_FOUND, "计划不存在!");
            }
            if (!targetPlan.getProjectId().equals(targetProjectId)) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "计划不在项目下!");
            }
            planIsActive = targetPlan.getIsActive();
        }

        Set<Long> canCopyCardIds = canCopyCardIds(fromProjectId.equals(targetProjectId) ? permissions : permissions(targetProjectId), ids, targetPlanId);
        cards = cards.stream().filter(card -> canCopyCardIds.contains(card.getId())).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(cards)) {
            return 0;
        }

        projectCardDailyLimiter.check(targetProjectId, cards.size());

        AbstractCardCopy.AbstractCardCopyBuilder builder;
        List<String> ranks;
        if (fromProjectId.equals(targetProjectId)) {
            if (fromPlanId.equals(targetPlanId)) {
                builder = PlanInternalCardCopy.builder();
                ranks = targetPlanCardRanks(targetProjectId, targetPlanId, location, cards.size());
            } else {
                builder = ProjectInternalCardCopy.builder();
                ranks = targetPlanCardRanks(targetProjectId, targetPlanId, location, cards.size());
            }
        } else {
            CheckSchemaResult checkSchemaResult = projectSchemaQueryService.checkSchemaForCopy(
                    fromProjectId, targetProjectId);
            builder = DiffProjectCardCopy.builder()
                    .fromProject(fromProject)
                    .checkSchemaResult(checkSchemaResult)
                    .toSchema(projectSchemaQueryService.getProjectCardSchema(targetProjectId));
            ranks = cardHelper.nextRanks(targetProjectId, cards.size());
        }

        ProjectCardSchema targetSchema = projectSchemaQueryService.getProjectCardSchema(targetProjectId);
        LoginUser user = userService.currentUser();
        OperationContext opContext = OperationContext.instance(user.getUsername());
        Consumer<Map<String, Object>> setCalcFields = baseCardDetail -> FieldCalcGraph.INSTANCE.calcCreate(baseCardDetail, opContext, targetSchema, fieldRefValueLoader);
        Map<Long, Card> reParentMap = MapUtils.EMPTY_MAP;
        Set<Long> deleteIds = SetUtils.emptySet();
        if (isDeleteSourceCards) {
            Set<Long> canDeleteIds = canOperationCardIds(permissions, new ArrayList<>(canCopyCardIds), cardProps -> permissions.hasLimitPermission(OperationType.CARD_DELETE, cardProps));
            reParentMap = promoteForChildren(opContext.getUserName(), fromProject, cards.stream().filter(card -> canDeleteIds.contains(card.getId())).collect(Collectors.toList()));
            deleteIds = canDeleteIds;
        }
        builder
                .opContext(opContext)
                .setCalcFields(setCalcFields)
                .cards(cards)
                .ranks(ranks)
                .targetProject(targetProject)
                .targetSchema(targetSchema)
                .targetPlanId(targetPlanId)
                .planIsActive(planIsActive)
                .isDeleteSourceCards(isDeleteSourceCards)
                .reParentMap(reParentMap)
                .deleteIds(deleteIds)
                .cardMapper(cardMapper)
                .cardAttachmentRelMapper(cardAttachmentRelMapper)
                .cardRelateRelMapper(cardRelateRelMapper)
                .cardDao(cardDao)
                .cardHelper(cardHelper)
                .cardBpmCmdService(cardBpmCmdService)
                .eventDispatcher(eventDispatcher)
                .operationLogCmdService(operationLogCmdService);
        return builder.build().run();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void remind(Card card, CardRemindRequest remindRequest) {
        if (CollectionUtils.isEmpty(remindRequest.getUserFields())) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "收件人为空!");
        }
        Map<String, Object> cardDetail = cardDao.findAsMap(card.getId());
        Set<String> users = new HashSet<>();
        remindRequest.getUserFields().stream()
                .filter(f -> StringUtils.isNotEmpty(f))
                .map(cardDetail::get)
                .forEach(value -> {
                    if (null == value) {
                        return;
                    }
                    if (value instanceof Collection) {
                        ((Collection<?>) value).stream()
                                .map(FieldUtil::toString)
                                .filter(StringUtils::isNotEmpty)
                                .forEach(users::add);
                    } else if (value instanceof String[]) {
                        Arrays.stream((String[]) value)
                                .map(FieldUtil::toString)
                                .filter(StringUtils::isNotEmpty)
                                .forEach(users::add);
                    } else {
                        users.add(FieldUtil.toString(value));
                    }
                });
        if (CollectionUtils.isEmpty(users)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "收件人为空!");
        }
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(card.getCompanyId());
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(card.getProjectId());
        cardNoticeService.noticeRemind(card, cardDetail, userService.currentUser(), users, companyCardSchema, schema, remindRequest.getContent());
    }

    public void tryAutoStatusFlow(Long projectId, List<Card> cards, String userName,
                                  AutoStatusFlowEventType eventType, AutoStatusFlowEventFilter eventFilter) throws IOException {
        BaseUser user = userService.user(userName);
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        Map<Long, Card> cardMap = new HashMap<>();
        List<Long> ids = new ArrayList<>();
        for (Card card : cards) {
            cardMap.put(card.getId(), card);
            ids.add(card.getId());
        }
        Map<Long, Map<String, Object>> cardDetails = cardDao.findAsMap(ids);
        Map<Long, Map<String, Object>> updateCardDetails = new HashMap<>();
        Map<Long, AutoStatusFlowEventMsg> eventMsgs = new HashMap<>();
        AutoStatusFlowMatcher autoStatusFlowMatcher = AutoStatusFlowMatcher.builder().eventFilter(eventFilter).build();
        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();
        OperationContext opContext = OperationContext.instance(user.getUsername());
        Consumer<Map<String, Object>> setCalcFields = baseCardDetail -> FieldCalcGraph.INSTANCE.calcUpdate(
                baseCardDetail,
                SetUtils.hashSet(CardField.STATUS),
                opContext,
                schema,
                fieldRefValueLoader);
        cardDetails.entrySet().forEach(entry -> {
            Long cardId = entry.getKey();
            Map<String, Object> cardDetail = entry.getValue();
            CardType cardType = schema.findCardType(FieldUtil.toString(cardDetail.get(CardField.TYPE)));
            if (null == cardType) {
                return;
            }
            String fromStatus = FieldUtil.toString(cardDetail.get(CardField.STATUS));
            CardType.AutoStatusFlowConf autoStatusFlowConf = autoStatusFlowMatcher.matchAutoStatusFlow(cardType, fromStatus);
            if (null == autoStatusFlowConf) {
                return;
            }
            String toStatus = autoStatusFlowConf.getTargetStatus();
            if (StringUtils.equals(fromStatus, toStatus)) {
                return;
            }
            String fromStatusName = schema.findCardStatusName(fromStatus);
            String toStatusName = schema.findCardStatusName(toStatus);
            cardHelper.generatePropsForUpdateStatus(cardDetail, opContext, toStatus, schema);
            setCalcFields.accept(cardDetail);
            updateCardDetails.put(cardId, cardDetail);
            eventMsgs.put(cardId, AutoStatusFlowEventMsg.builder()
                    .eventType(eventType)
                    .fromMsg(schema.findCardStatusName(fromStatus))
                    .toMsg(schema.findCardStatusName(toStatus))
                    .build());
            Card card = cardMap.get(cardId);
            // notice
            cardNoticeService.noticeStatusFlow(user.getUsername(), cardDetail, schema, fromStatus, toStatus);
            // cardEnd
            if (CardHelper.isChangeToEnd(schema, cardType.getKey(), fromStatus, toStatus)) {
                cardEndService.cardEnd(FieldUtil.toStringList(cardDetail.get(CardField.OWNER_USERS)), card);
            }
        });
        if (MapUtils.isEmpty(updateCardDetails)) {
            return;
        }
        cardDao.saveOrUpdate(updateCardDetails);
        Project project = projectQueryService.select(projectId);
        eventDispatcher.asyncDispatch(() -> CardsUpdateEvent.builder()
                .project(project)
                .user(userName)
                .cardEvents(updateCardDetails.entrySet().stream()
                        .map(entry -> CardEvent.builder()
                                .id(IdUtil.generateId())
                                .cardId(entry.getKey())
                                .date(new Date())
                                .user(userName)
                                .eventType(EventType.AUTO_STATUS_FLOW)
                                .eventMsg(eventMsgs.get(entry.getKey()))
                                .cardDetail(entry.getValue())
                                .build())
                        .collect(Collectors.toList()))
                .build());
        String companyName = companyService.companyName(projectQueryService.getProjectCompany(projectId));
        webHookProjectCmdService.asyncSendCardsWebHookMsg(updateCardDetails, project, WebHookEventType.UPDATE_CARD, companyName, user);
    }

    public void tryBpmStatusFlow(Long cardId, Long flowId, boolean approved) throws IOException {
        Card card = cardQueryService.select(cardId);
        if (card == null) {
            throw CodedException.NOT_FOUND;
        }
        if (BooleanUtils.isTrue(card.getDeleted())) {
            return;
        }
        Map<String, Object> cardDetail = cardDao.findAsMap(cardId);
        Long currentFlowId = FieldUtil.getBpmFlowId(cardDetail);
        if (!flowId.equals(currentFlowId)) {
            log.info("cardId[{}]: event flowId[{}], but current flowId[{}]", cardId, flowId, currentFlowId);
            return;
        }

        CardBpmFlow flow = cardBpmQueryService.cardBpmFlow(cardId, flowId);
        if (flow == null) {
            log.error("cardId[{}] flowId[{}] cannot find CardBpmFlow!", cardId, flowId);
            return;
        }

        BaseUser user = userService.user(flow.getUser());
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(card.getProjectId());
        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();
        OperationContext opContext = OperationContext.instance(user.getUsername());
        BiConsumer<Map<String, Object>, Set<String>> setCalcFields = (baseCardDetail, changedFields) -> FieldCalcGraph.INSTANCE.calcUpdate(
                baseCardDetail,
                changedFields,
                opContext,
                schema,
                fieldRefValueLoader);

        if (!approved) {
            cardDetail.put(CardField.BPM_FLOW_ID, null);
            cardDetail.put(CardField.BPM_FLOW_TO_STATUS, null);
            setCalcFields.accept(cardDetail, SetUtils.hashSet(CardField.BPM_FLOW_ID, CardField.BPM_FLOW_TO_STATUS));
            cardDao.saveOrUpdate(cardId, cardDetail);
            return;
        }

        CardType cardType = schema.findCardType(FieldUtil.toString(cardDetail.get(CardField.TYPE)));
        if (null == cardType) {
            return;
        }

        String fromStatus = FieldUtil.toString(cardDetail.get(CardField.STATUS));
        String toStatus = flow.getFlowDetail().getToStatus();
        if (StringUtils.equals(fromStatus, toStatus)) {
            return;
        }
        if (cardType.findStatusConf(toStatus) == null) {
            return;
        }
        cardHelper.generatePropsForUpdateStatus(cardDetail, opContext, toStatus, schema);
        cardDetail.put(CardField.BPM_FLOW_ID, null);
        cardDetail.put(CardField.BPM_FLOW_TO_STATUS, null);
        setCalcFields.accept(cardDetail, SetUtils.hashSet(CardField.BPM_FLOW_ID, CardField.BPM_FLOW_TO_STATUS, CardField.STATUS));
        cardDao.saveOrUpdate(cardId, cardDetail);
        // event
        eventDispatcher.dispatch(UpdateEventHelper.builder()
                .findPlanById(fieldRefValueLoader.getLoadPlan())
                .findCardById(cardQueryService::select)
                .findStoryMapL2NodeInfoById(storyMapQueryService::selectStoryMapL2NodeInfoById)
                .build()
                .cardUpdateEvent(
                        card,
                        user.getUsername(),
                        FieldChange.builder()
                                .field(schema.findCardField(CardField.STATUS))
                                .fromValue(fromStatus)
                                .toValue(toStatus)
                                .build(),
                        cardDetail
                ));
        // notice
        cardNoticeService.noticeStatusFlow(user.getUsername(), cardDetail, schema, fromStatus, toStatus);
        // cardEnd
        if (CardHelper.isChangeToEnd(schema, cardType.getKey(), fromStatus, toStatus)) {
            cardEndService.cardEnd(FieldUtil.toStringList(cardDetail.get(CardField.OWNER_USERS)), card);
        }
        String companyName = companyService.companyName(projectQueryService.getProjectCompany(card.getProjectId()));
        Project project = projectQueryService.select(card.getProjectId());
        webHookProjectCmdService.asyncSendCardWebHookMsg(cardDetail, project, WebHookEventType.UPDATE_CARD, companyName, user);
    }

    public void tryBpmStatusFlows(@NotNull StatusFlowResult[] results, @NotNull Long projectId) throws IOException {
        Map<Long, Long> cardFlowIdMap = new HashMap<>();
        Map<Long, Boolean> cardFlowResultMap = new HashMap<>();
        List<Long> cardIds = new ArrayList<>();
        for (StatusFlowResult result : results) {
            Long cardId = result.getCardId();
            cardFlowIdMap.put(cardId, result.getFlowId());
            cardFlowResultMap.put(cardId, result.isApproved());
            cardIds.add(cardId);
        }
        List<Card> cards = cardQueryService.select(cardIds);
        if (CollectionUtils.isEmpty(cards)) {
            throw CodedException.NOT_FOUND;
        }

        Map<Long, Map<String, Object>> cardDetails = cardDao.findAsMap(cardIds);
        Map<Long, Card> cardMap = cards.stream().collect(CollectorsV2.toMap(Card::getId, Function.identity()));
        Map<Long, CardBpmFlow> flowIdFlowMap = cardBpmQueryService.cardBpmFlows(new ArrayList<>(cardFlowIdMap.values()));
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        Project project = projectQueryService.select(projectId);
        Map<Long, Map<String, Object>> needCancelCardDetails = new HashMap<>();
        Map<Long, Map<String, Object>> needPassCardDetails = new HashMap<>();
        List<CardUpdateEvent> events = new ArrayList<>();
        List<Consumer> noticeStatusFlowConsumer = new ArrayList<>();
        List<Consumer> sendCardEndNotice = new ArrayList<>();
        List<Consumer> sendWebHooks = new ArrayList<>();
        for (Long cardId : cardIds) {
            Card card = cardMap.get(cardId);
            if (card == null) {
                throw CodedException.NOT_FOUND;
            }
            if (BooleanUtils.isTrue(card.getDeleted())) {
                continue;
            }
            Map<String, Object> cardDetail = cardDetails.get(cardId);
            Long currentFlowId = FieldUtil.getBpmFlowId(cardDetail);
            Long flowId = cardFlowIdMap.get(cardId);
            Boolean approved = cardFlowResultMap.get(cardId);
            if (!flowId.equals(currentFlowId)) {
                log.info("cardId[{}]: event flowId[{}], but current flowId[{}]", cardId, flowId, currentFlowId);
                continue;
            }
            if (!approved) {
                Map<String, Object> approvalCardProps = new HashMap<>();
                approvalCardProps.put(CardField.BPM_FLOW_ID, null);
                approvalCardProps.put(CardField.BPM_FLOW_TO_STATUS, null);
                needCancelCardDetails.put(cardId, approvalCardProps);
                continue;
            }
            CardBpmFlow flow = flowIdFlowMap.get(cardFlowIdMap.get(cardId));
            if (flow == null) {
                log.error("cardId[{}] flowId[{}] cannot find CardBpmFlow!", cardId, flowId);
                continue;
            }

            CardType cardType = schema.findCardType(FieldUtil.toString(cardDetail.get(CardField.TYPE)));
            if (null == cardType) {
                continue;
            }
            String fromStatus = FieldUtil.toString(cardDetail.get(CardField.STATUS));
            String toStatus = flow.getFlowDetail().getToStatus();
            if (StringUtils.equals(fromStatus, toStatus)) {
                continue;
            }
            if (cardType.findStatusConf(toStatus) == null) {
                continue;
            }

            cardHelper.generatePropsForUpdateStatus(cardDetail, flow.getUser(), toStatus, schema);
            cardDetail.put(CardField.BPM_FLOW_ID, null);
            cardDetail.put(CardField.BPM_FLOW_TO_STATUS, null);
            needPassCardDetails.put(cardId, cardDetail);

            BaseUser user = userService.user(flow.getUser());
            CardUpdateEvent cardUpdateEvent = UpdateEventHelper.builder()
                    .findPlanById(planQueryService::select)
                    .findCardById(cardQueryService::select)
                    .findStoryMapL2NodeInfoById(storyMapQueryService::selectStoryMapL2NodeInfoById)
                    .build()
                    .cardUpdateEvent(
                            card,
                            user.getUsername(),
                            FieldChange.builder()
                                    .field(schema.findCardField(CardField.STATUS))
                                    .fromValue(fromStatus)
                                    .toValue(toStatus)
                                    .build(),
                            cardDetail
                    );
            events.add(cardUpdateEvent);

            // notice
            cardNoticeService.noticeStatusFlow(user.getUsername(), cardDetail, schema, fromStatus, toStatus);
            // cardEnd
            if (CardHelper.isChangeToEnd(schema, cardType.getKey(), fromStatus, toStatus)) {
                sendCardEndNotice.add(o -> cardEndService.cardEnd(FieldUtil.toStringList(cardDetail.get(CardField.OWNER_USERS)), card));
            }
            // webhook
            String companyName = companyService.companyName(projectQueryService.getProjectCompany(card.getProjectId()));
            sendWebHooks.add(o -> webHookProjectCmdService.asyncSendCardWebHookMsg(cardDetail, project, WebHookEventType.UPDATE_CARD, companyName, user));
        }
        if (MapUtils.isNotEmpty(needCancelCardDetails)) {
            cardDao.updateSelective(needCancelCardDetails);
        }

        if (MapUtils.isNotEmpty(needPassCardDetails)) {
            cardDao.saveOrUpdate(needPassCardDetails);
        }
        if (CollectionUtils.isNotEmpty(events)) {
            events.forEach(event -> eventDispatcher.dispatch(event));
        }
        if (CollectionUtils.isNotEmpty(noticeStatusFlowConsumer)) {
            noticeStatusFlowConsumer.forEach(consumer -> consumer.accept(null));
        }
        if (CollectionUtils.isNotEmpty(sendCardEndNotice)) {
            sendCardEndNotice.forEach(consumer -> consumer.accept(null));
        }
        if (CollectionUtils.isNotEmpty(sendWebHooks)) {
            sendWebHooks.forEach(consumer -> consumer.accept(null));
        }
    }

    @AfterCommit
    @Async
    public void onPlanInActive(OperationContext opContext, Long projectId, List<Long> planIds) throws IOException {
        changePlanIsActive(opContext, projectId, planIds, false);
    }

    @AfterCommit
    @Async
    public void onPlanActive(OperationContext opContext, Long projectId, List<Long> planIds) throws IOException {
        changePlanIsActive(opContext, projectId, planIds, true);
    }

    private void changePlanIsActive(OperationContext opContext, Long projectId, List<Long> planIds, boolean planIsActive) throws IOException {
        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();
        List<Query> queries = Arrays.asList(
                In.builder().field(CardField.PLAN_ID).values(planIds.stream().map(String::valueOf).collect(Collectors.toList())).build(),
                Eq.builder().field(CardField.DELETED).value(String.valueOf(false)).build()
        );
        List<Long> cardIds = cardDao.searchIds(queries);
        if (CollectionUtils.isEmpty(cardIds)) {
            return;
        }
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        ListUtils.partition(cardIds, 1000).forEach(batchCardIds -> {
            List<CardEvent> cardEvents = new ArrayList<>();
            Consumer<Map<String, Object>> setCalcFields = (baseCardDetail) -> FieldCalcGraph.INSTANCE.calcUpdate(
                    baseCardDetail,
                    SetUtils.hashSet(CardField.PLAN_IS_ACTIVE),
                    opContext,
                    schema,
                    fieldRefValueLoader);
            try {
                Map<Long, Map<String, Object>> cardDetails = cardDao.findAsMap(batchCardIds);
                cardDetails.entrySet().forEach(entry -> {
                    entry.getValue().put(CardField.PLAN_IS_ACTIVE, planIsActive);
                    setCalcFields.accept(entry.getValue());
                    cardEvents.add(CardEvent.builder()
                            .id(IdUtil.generateId())
                            .cardId(entry.getKey())
                            .date(opContext.getTime())
                            .user(opContext.getUserName())
                            .eventType(EventType.PLAN_IS_ACTIVE)
                            .cardDetail(entry.getValue())
                            .build());
                });
                cardDao.saveOrUpdate(cardDetails);
                cardEventCmdService.asyncSave(cardEvents);
            } catch (IOException e) {
                log.error(String.format("process plan_is_active exception for batch-cards in plans[%s]", StringUtils.join(planIds, ",")), e);
            }
        });
    }

    @AfterCommit
    @Async
    public void onChangeProjectActive(OperationContext opContext, Long projectId, boolean projectIsActive) throws IOException {
        changeProjectIsActive(opContext, projectId, projectIsActive);
    }

    private void changeProjectIsActive(OperationContext opContext, Long projectId, boolean projectIsActive) throws IOException {
        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();
        List<Query> queries = Arrays.asList(
                Eq.builder().field(CardField.PROJECT_ID).value(String.valueOf(projectId)).build()
        );
        List<Long> cardIds = cardDao.searchIds(queries);
        if (CollectionUtils.isEmpty(cardIds)) {
            return;
        }
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        ListUtils.partition(cardIds, 1000).forEach(batchCardIds -> {
            List<CardEvent> cardEvents = new ArrayList<>();
            Consumer<Map<String, Object>> setCalcFields = (baseCardDetail) -> FieldCalcGraph.INSTANCE.calcUpdate(
                    baseCardDetail,
                    SetUtils.hashSet(CardField.PROJECT_IS_ACTIVE),
                    opContext,
                    schema,
                    fieldRefValueLoader);
            try {
                Map<Long, Map<String, Object>> cardDetails = cardDao.findAsMap(batchCardIds);
                cardDetails.entrySet().forEach(entry -> {
                    entry.getValue().put(CardField.PROJECT_IS_ACTIVE, projectIsActive);
                    setCalcFields.accept(entry.getValue());
                    cardEvents.add(CardEvent.builder()
                            .id(IdUtil.generateId())
                            .cardId(entry.getKey())
                            .date(opContext.getTime())
                            .user(opContext.getUserName())
                            .eventType(EventType.PROJECT_IS_ACTIVE)
                            .cardDetail(entry.getValue())
                            .build());
                });
                cardDao.saveOrUpdate(cardDetails);
                cardEventCmdService.asyncSave(cardEvents);
            } catch (IOException e) {
                log.error(String.format("process project_is_active exception for batch-cards in project[%s]", projectId), e);
            }
        });
    }

    @AfterCommit
    @Async
    public void asyncChangeStatusIsEnd(OperationContext opContext, Long companyId, Long projectId, String type, List<String> changeToEndStatuses, List<String> changeToNoEndStatuses) {
        try {
            changeStatusIsEnd(opContext, projectId, type, changeToEndStatuses, true);
            changeStatusIsEnd(opContext, projectId, type, changeToNoEndStatuses, false);
            operationLogCmdService.updateCardTypeWorkFlowEnd(opContext, companyId, projectId, type, changeToEndStatuses, changeToNoEndStatuses);
        } catch (Exception e) {
            log.error(String.format("change status is end for card exception for project:[%s]!", projectId), e);
            return;
        }
    }

    private void changeStatusIsEnd(OperationContext opContext, Long projectId, String type, List<String> statuses, boolean isEnd) throws IOException {
        if (CollectionUtils.isEmpty(statuses)) {
            return;
        }
        List<Query> queries = Arrays.asList(
                Eq.builder().field(CardField.PROJECT_ID).value(String.valueOf(projectId)).build(),
                Eq.builder().field(CardField.TYPE).value(type).build(),
                In.builder().field(CardField.STATUS).values(statuses).build()
        );
        List<Long> cardIds = cardDao.searchIds(queries);
        if (CollectionUtils.isEmpty(cardIds)) {
            return;
        }
        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        ListUtils.partition(cardIds, 1000).forEach(batchCardIds -> {
            List<CardEvent> cardEvents = new ArrayList<>();
            Consumer<Map<String, Object>> setCalcFields = (baseCardDetail) -> FieldCalcGraph.INSTANCE.calcUpdate(
                    baseCardDetail,
                    SetUtils.hashSet(CardField.CALC_IS_END),
                    opContext,
                    schema,
                    fieldRefValueLoader);
            try {
                Map<Long, Map<String, Object>> cardDetails = cardDao.findAsMap(batchCardIds);
                cardDetails.entrySet().forEach(entry -> {
                    entry.getValue().put(CardField.CALC_IS_END, isEnd);
                    setCalcFields.accept(entry.getValue());
                    cardEvents.add(CardEvent.builder()
                            .id(IdUtil.generateId())
                            .cardId(entry.getKey())
                            .date(opContext.getTime())
                            .user(opContext.getUserName())
                            .eventType(EventType.CALC_IS_END)
                            .cardDetail(entry.getValue())
                            .build());
                });
                cardDao.saveOrUpdate(cardDetails);
                cardEventCmdService.asyncSave(cardEvents);
            } catch (IOException e) {
                log.error(String.format("process calc_is_end exception for batch-cards in project[%d]", projectId), e);
            }
        });
    }

    @AfterCommit
    @Async
    public void asyncMigrateForCloseStatus(OperationContext opContext, Long projectId, String type, ProjectCardSchema schema, CardStatus closeStatus, CardStatus toStatus) {
        migrateForCloseStatus(opContext, projectId, type, schema, closeStatus, toStatus, true);
        migrateForCloseStatus(opContext, projectId, type, schema, closeStatus, toStatus, false);
    }

    private void migrateForCloseStatus(OperationContext opContext, Long projectId, String type, ProjectCardSchema schema, CardStatus closeStatus, CardStatus toStatus, boolean planIsActive) {
        List<Long> cardIds = null;
        try {
            cardIds = cardDao.searchIds(
                    Eq.builder().field(CardField.PROJECT_ID).value(String.valueOf(projectId)).build(),
                    Eq.builder().field(CardField.TYPE).value(type).build(),
                    Eq.builder().field(CardField.STATUS).value(closeStatus.getKey()).build(),
                    Eq.builder().field(CardField.PLAN_IS_ACTIVE).value(String.valueOf(planIsActive)).build());
        } catch (Exception e) {
            log.error(String.format("Search card for close status for project:[%s] exception!", projectId), e);
            return;
        }
        if (CollectionUtils.isEmpty(cardIds)) {
            return;
        }

        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();
        ListUtils.partition(cardIds, 1000).forEach(batchCardIds -> {
            try {
                BiConsumer<Map<String, Object>, Set<String>> setCalcFields = (baseCardDetail, changedFields) -> FieldCalcGraph.INSTANCE.calcUpdate(
                        baseCardDetail,
                        changedFields,
                        opContext,
                        schema,
                        fieldRefValueLoader);
                List<CardEvent> cardEvents = new ArrayList<>();
                Map<Long, Map<String, Object>> cardDetails = cardDao.findAsMap(batchCardIds);
                cardDetails.entrySet().forEach(entry -> {
                    Map<String, Object> cardDetail = entry.getValue();
                    Set<String> changedFields = new HashSet<>();
                    CardHelper.generatePropsForUpdate(cardDetail, opContext, CardField.STATUS, toStatus.getKey());
                    changedFields.add(CardField.STATUS);
                    Long bpmFlowId = FieldUtil.getBpmFlowId(cardDetail);
                    if (bpmFlowId != null && bpmFlowId > 0) {
                        cardDetail.put(CardField.BPM_FLOW_ID, null);
                        cardDetail.put(CardField.BPM_FLOW_TO_STATUS, null);
                        changedFields.add(CardField.BPM_FLOW_ID);
                        changedFields.add(CardField.BPM_FLOW_TO_STATUS);
                    }
                    setCalcFields.accept(entry.getValue(), changedFields);
                    cardEvents.add(CardEvent.builder()
                            .id(IdUtil.generateId())
                            .cardId(entry.getKey())
                            .date(opContext.getTime())
                            .user(opContext.getUserName())
                            .eventType(EventType.CALC_IS_END)
                            .cardDetail(entry.getValue())
                            .build());
                });
                cardDao.saveOrUpdate(cardDetails);
                eventDispatcher.asyncDispatch(() -> CardsCloseStatusEvent.builder()
                        .project(projectQueryService.select(projectId))
                        .user(opContext.getUserName())
                        .cardEvents(cardEvents)
                        .build());
            } catch (Exception e) {
                log.error(String.format("Migrate close status for batch cards:[%s] exception!", batchCardIds), e);
            }
        });
    }

    public void unBindStoryMap(List<Card> cards, Map<Long, String> storyMapNodeInfo) throws IOException {
        if (CollectionUtils.isEmpty(cards)) {
            return;
        }
        List<Long> ids = new ArrayList<>();
        Map<Long, String> cardStoryMapNodeInfo = new HashMap<>();
        cards.forEach(card -> {
            if (card.getStoryMapNodeId() > 0) {
                cardStoryMapNodeInfo.put(card.getId(), storyMapNodeInfo.get(card.getStoryMapNodeId()));
                card.setStoryMapNodeId(0L);
                cardMapper.updateByPrimaryKey(card);
                ids.add(card.getId());
            }
        });

        String user = userService.currentUserName();
        final Long projectId = cards.get(0).getProjectId();
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        String fieldName = schema.findCardField(CardField.STATUS).getName();
        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();
        ListUtils.partition(ids, 1000).forEach(batchCardIds -> {
            try {
                OperationContext opContext = OperationContext.instance(user);
                Consumer<Map<String, Object>> setCalcFields = (baseCardDetail) -> FieldCalcGraph.INSTANCE.calcUpdate(
                        baseCardDetail,
                        SetUtils.hashSet(CardField.STORY_MAP_NODE_ID),
                        opContext,
                        schema,
                        fieldRefValueLoader);
                List<CardEvent> cardEvents = new ArrayList<>();
                Map<Long, Map<String, Object>> cardDetails = cardDao.findAsMap(batchCardIds);
                cardDetails.entrySet().forEach(entry -> {
                    Map<String, Object> cardDetail = entry.getValue();
                    CardHelper.generatePropsForUpdate(cardDetail, opContext, CardField.STORY_MAP_NODE_ID, 0L);
                    setCalcFields.accept(entry.getValue());
                    Long cardId = entry.getKey();
                    CardEvent cardEvent = CardEvent.builder()
                            .id(IdUtil.generateId())
                            .cardId(entry.getKey())
                            .date(opContext.getTime())
                            .user(user)
                            .eventType(EventType.UPDATE)
                            .eventMsg(UpdateEventMsg.builder()
                                    .fieldDetailMsg(UpdateEventMsg.FieldDetailMsg.builder()
                                            .fieldKey(CardField.STORY_MAP_NODE_ID)
                                            .fieldMsg(fieldName)
                                            .fromMsg(cardStoryMapNodeInfo.get(cardId))
                                            .toMsg(null)
                                            .build())
                                    .build())
                            .cardDetail(cardDetail)
                            .build();
                    cardEvents.add(cardEvent);
                });
                cardDao.saveOrUpdate(cardDetails);
                eventDispatcher.asyncDispatch(() -> CardsUpdateEvent.builder()
                        .project(projectQueryService.select(projectId))
                        .user(user)
                        .cardEvents(cardEvents)
                        .build());
            } catch (Exception e) {
                log.error(String.format("Migrate close status for batch cards:[%s] exception!", batchCardIds), e);
            }
        });
    }

    public void changeStoryMapLocation(Card card, Long storyMapNodeId, Long planId) throws IOException {
        LoginUser user = userService.currentUser();
        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();
        OperationContext opContext = OperationContext.instance(user.getUsername());
        Long projectId = card.getProjectId();
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        Map<String, Object> cardProps = new HashMap<>();
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(card.getCompanyId());
        cardProps.put(CardField.STORY_MAP_NODE_ID, storyMapNodeId);
        cardProps.put(CardField.PLAN_ID, planId);
        Function<Long, Plan> findPlanById = fieldRefValueLoader.getLoadPlan();
        Function<Long, Card> findCardById = CacheableFunction.instance(cardQueryService::select);
        BiConsumer<Map<String, Object>, List<FieldChange>> setCalcFields = (baseCardDetail, fieldChanges) -> FieldCalcGraph.INSTANCE.calcUpdate(
                baseCardDetail,
                fieldChanges.stream().map(fc -> fc.getField().getKey()).collect(Collectors.toSet()),
                opContext,
                schema,
                fieldRefValueLoader);
        ProjectWorkloadSetting workloadSetting = projectSchemaQueryService.getProjectWorkloadSetting(projectId);
        eventDispatcher.dispatch(StoryMapLocationUpdateHelper.builder()
                .companyCardSchema(companyCardSchema)
                .userService(userService)
                .opContext(opContext)
                .setCalcFields(setCalcFields)
                .schema(schema)
                .workloadSetting(workloadSetting)
                .schemaHelper(schemaHelper)
                .cardDao(cardDao)
                .cardMapper(cardMapper)
                .findCardById(findCardById)
                .findCardDescendant(cardQueryService::selectDescendant)
                .findPlanById(findPlanById)
                .findStoryMapNodeById(storyMapQueryService::selectStoryMapNodeById)
                .findStoryMapL2NodeInfoById(storyMapQueryService::selectStoryMapL2NodeInfoById)
                .card(card)
                .cardDetail(cardProps)
                .checkProps(true)
                .cardNoticeService(cardNoticeService)
                .cardEndService(cardEndService)
                .cardHelper(cardHelper)
                .operationLogCmdService(operationLogCmdService)
                .systemSettingService(systemSettingService)
                .cardBpmCmdService(cardBpmCmdService)
                .build()
                .update());
        Project project = projectQueryService.select(projectId);
        webHookProjectCmdService.asyncSendCardWebHookMsg(cardDao.findAsMap(card.getId()), project, WebHookEventType.UPDATE_CARD, companyService.currentCompanyName(), user);
    }

    public void setWatch(Long cardId, boolean watch) throws IOException {
        List<String> watchUsers = FieldUtil.getWatchUsers(cardDao.findAsMap(cardId, CardField.WATCH_USERS));
        String user = userService.currentUserName();
        if ((null != watchUsers && watchUsers.contains(user)) ^ watch) {
            Set<String> newWatchUsers = new HashSet<>();
            if (CollectionUtils.isNotEmpty(watchUsers)) {
                newWatchUsers.addAll(watchUsers);
            }
            if (watch) {
                newWatchUsers.add(user);
            } else {
                newWatchUsers.remove(user);
            }
            cardDao.updateSelective(cardId, Collections.singletonMap(CardField.WATCH_USERS, newWatchUsers));
        }
    }

    private Card create(Long projectId, Map<String, Object> cardDetail, Boolean sendEmail) throws IOException {
        return create(projectId, IdUtil.generateId(), cardDetail, sendEmail);
    }

    private Card create(Long projectId, Long cardId, Map<String, Object> cardDetail, Boolean sendEmail) throws IOException {
        LoginUser user = userService.currentUser();
        OperationContext opContext = OperationContext.instance(user.getUsername());
        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();
        Project project = fieldRefValueLoader.project(projectId);
        Long planId = getLong(cardDetail, CardField.PLAN_ID);
        Boolean planIsActive = true;
        if (planId > 0) {
            planIsActive = fieldRefValueLoader.plan(planId).getIsActive();
            if (!planIsActive) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "不能绑定已归档计划！");
            }
        }
        Long parentId = getLong(cardDetail, CardField.PARENT_ID);
        Long ancestorId = 0L;
        if (parentId > 0L) {
            Card parent = cardMapper.selectByPrimaryKey(parentId);
            if (null != parent && parent.getProjectId().equals(project.getId())) {
                ancestorId = parent.getAncestorId() > 0 ? parent.getAncestorId() : parentId;
            } else {
                parentId = 0L;
            }
        }
        Long seqNum = cardHelper.seqNum(projectId);
        String rank = cardHelper.nextRank(projectId);
        Card card = Card.builder()
                .id(cardId)
                .projectId(projectId)
                .projectKey(project.getKey())
                .seqNum(seqNum)
                .planId(planId)
                .parentId(parentId)
                .ancestorId(ancestorId)
                .rank(rank)
                .companyId(companyService.currentCompany())
                .deleted(false)
                .maxCommentSeqNum(0L)
                .storyMapNodeId(FieldUtil.getStoryMapNodeId(cardDetail))
                .latestEventId(0L)
                .build();
        cardMapper.insert(card);
        CardHelper.setCardCreatedProps(cardDetail, opContext, card);
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        FieldCalcGraph.INSTANCE.calcCreate(cardDetail, opContext, schema, fieldRefValueLoader);
        cardDao.saveOrUpdate(card.getId(), cardDetail);
        eventDispatcher.dispatch(CardCreateEvent.builder().user(user.getUsername()).card(card).cardDetail(cardDetail).sendEmail(sendEmail).build());
        webHookProjectCmdService.asyncSendCardWebHookMsg(cardDetail, project, WebHookEventType.CREATE_CARD, companyService.currentCompanyName(), user);

        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(card.getCompanyId());
        cardNoticeService.noticeAtUsersInCard(card, cardDetail, user.getUsername(), FieldUtil.getAtUsers(cardDetail), companyCardSchema, schema, ProjectNoticeConfig.Type.CREATE);
        return card;
    }

    private void rmRelatedCardIdFromCardDetail(Long projectId, List<Long> cardIds, List<Long> relateCardIds) {
        if (CollectionUtils.isEmpty(cardIds)) {
            return;
        }
        try {
            Map<Long, Map<String, Object>> cardsProps = cardDao.findAsMap(cardIds, CardField.RELATED_CARD_IDS);
            if (MapUtils.isEmpty(cardsProps)) {
                return;
            }
            cardsProps.values().forEach(cardProps -> cardProps.put(CardField.RELATED_CARD_IDS, CardHelper.rmRelateCardIds(cardProps, relateCardIds)));
            cardDao.updateSelective(cardsProps);
        } catch (IOException e) {
            log.error("rmRelatedCardIdsFromCardDetail exception!", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void rmRelatedCardIdFromCardDetail(Long projectId, List<Long> cardIds, Long relateCardId) {
        rmRelatedCardIdFromCardDetail(projectId, cardIds, Arrays.asList(relateCardId));
    }

    private Long getLong(Map<String, Object> cardDetail, String name) {
        return NumberUtils.toLong(String.valueOf(cardDetail.get(name)), 0L);
    }

    @NotNull
    private UserProjectPermissions permissions(Long projectId) throws CodedException {
        UserProjectPermissions permissions = permissionService.permissions(userService.currentUserName(), projectId);
        if (permissions == null) {
            throw CodedException.FORBIDDEN;
        }
        return permissions;
    }

    private List<Card> canOperationCards(UserProjectPermissions permissions, List<Card> cards, Function<Map<String, Object>, Boolean> cardPropsChecker) throws IOException {
        if (permissions.isAdmin()) {
            return cards;
        }
        List<Long> cardIds = cards.stream().map(Card::getId).collect(Collectors.toList());
        Set<Long> canOperationCardIds = canOperationCardIds(permissions, cardIds, cardPropsChecker);
        return cards.stream()
                .filter(card -> canOperationCardIds.contains(card.getId()))
                .collect(Collectors.toList());
    }

    private Set<Long> canOperationCardIds(UserProjectPermissions permissions, List<Long> cardIds, Function<Map<String, Object>, Boolean> cardPropsChecker) throws IOException {
        if (permissions.isAdmin()) {
            return cardIds.stream().collect(Collectors.toSet());
        }
        Map<Long, Map<String, Object>> cardDetails = cardDao.findAsMap(
                cardIds,
                OperationType.CARD_OP_LIMIT_FIELDS);
        return cardDetails.entrySet().stream()
                .filter(e -> cardPropsChecker.apply(e.getValue()))
                .map(e -> e.getKey())
                .collect(Collectors.toSet());
    }

    private Set<Long> canCopyCardIds(UserProjectPermissions permissions, List<Long> cardIds, Long targetPlanId) throws IOException {
        if (permissions.isAdmin()) {
            return cardIds.stream().collect(Collectors.toSet());
        }
        Map<Long, Map<String, Object>> cardDetails = cardDao.findAsMap(
                cardIds,
                OperationType.CARD_OP_LIMIT_FIELDS);
        return cardDetails.entrySet().stream()
                .filter(e -> {
                    e.getValue().put(CardField.PLAN_ID, targetPlanId);
                    return permissions.hasLimitPermission(OperationType.CARD_CREATE, e.getValue());
                })
                .map(e -> e.getKey())
                .collect(Collectors.toSet());
    }

    public void updateCardInnerType(@Nonnull Long projectId) {
        projectSchemaQueryService.cleanProjectCardSchemaCache(projectId);
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        schema.getTypes().stream().forEach(cardType -> {
            try {
                updateCardInnerType(projectId, cardType.getKey(), cardType.getInnerType());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void updateCardInnerType(@Nonnull Long projectId, @Nonnull String cardType, @Nonnull InnerCardType innerCardType) throws IOException {
        List<Query> queries = new ArrayList<>(2);
        queries.add(Eq.builder().field(CardField.PROJECT_ID).value(projectId.toString()).build());
        queries.add(Eq.builder().field(CardField.TYPE).value(cardType).build());
        cardDao.updateByQuery(queries, CardField.INNER_TYPE, innerCardType);
    }

    public Map<String, Object> setStatus(StatusBatchUpdateRequest request, List<Card> cardInDb, Long projectId) throws IOException {
        Map<String, Object> result = new HashMap<>();
        if (cardInDb.isEmpty()) {
            result.put("success", 0);
            result.put("err", 0);
            result.put("errMsg", "");
            return result;
        }

        Map<Long, Card> cardsMap = new HashMap<>();
        for (Card card : cardInDb) {
            cardsMap.put(card.getId(), card);
        }

        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        String statusFieldName = schema.findCardField(CardField.STATUS).getName();
        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();

        LoginUser user = userService.currentUser();
        OperationContext opContext = OperationContext.instance(user.getUsername());

        Consumer<Map<String, Object>> setCalcFields = (baseCardDetail) -> FieldCalcGraph.INSTANCE.calcUpdate(
                baseCardDetail,
                SetUtils.hashSet(CardField.STATUS),
                opContext,
                schema,
                fieldRefValueLoader);

        Function<Long, Plan> findPlanById = fieldRefValueLoader.getLoadPlan();
        Function<Long, Card> findCardById = CacheableFunction.instance(cardQueryService::select);

        List<String> errMessages = new ArrayList<>();

        int success = 0;
        Map<Long, Map<String, Object>> sourceCardProps = new HashMap<>();
        Map<Long, Card> webHockSendCards = new HashMap<>();
        Map<Long, Map<String, Object>> toCardDetails = new HashMap<>();
        CardsUpdateEvent cardsUpdateEvent = null;
        List<CardEvent> cardEvents = new ArrayList<>();
        for (StatusBatchUpdateRequest.TypeCards typeCard : request.getTypeCards()) {
            List<Card> successCards = new ArrayList<>();
            List<Long> cardIds = typeCard.getCardIds();
            String status = typeCard.getStatus();
            Map<Long, Map<String, Object>> cardDetails = cardDao.findAsMap(cardIds);
            for (Map.Entry<Long, Map<String, Object>> entry : cardDetails.entrySet()) {
                Long cardId = entry.getKey();
                Map<String, Object> cardDetail = entry.getValue();
                String oldStatus = FieldUtil.toString(cardDetail.get(CardField.STATUS));
                if (oldStatus.equals(status)) {
                    success = success + 1;
                    continue;
                }
                sourceCardProps.put(cardId, new HashMap<>(cardDetail));
                try {
                    schemaHelper.checkChangeCardStatus(schema, cardDetail, user.getUsername(), status, systemSettingService.bpmIsOpen());

                    String type = FieldUtil.toString(cardDetail.get(CardField.TYPE));
                    CardType cardType = schema.findCardType(type);
                    checkRequiredFields(schema, status, cardDetail, cardType);

                    CardHelper.generatePropsForUpdate(cardDetail, opContext, CardField.STATUS, status);
                    setCalcFields.accept(cardDetail);

                    Card card = cardsMap.get(cardId);
                    webHockSendCards.put(cardId, card);
                    successCards.add(card);
                    toCardDetails.put(cardId, cardDetail);
                    success = success + 1;
                    cardNoticeService.noticeStatusFlow(user.getUsername(), cardDetail, schema, oldStatus, status);
                    cardEndService.cardEnd(user, card, cardDetail, schema, oldStatus, status);
                    CardEvent cardEvent = CardEvent.builder()
                            .id(IdUtil.generateId())
                            .cardId(entry.getKey())
                            .date(opContext.getTime())
                            .user(opContext.getUserName())
                            .eventType(EventType.UPDATE)
                            .eventMsg(UpdateEventMsg.builder()
                                    .fieldDetailMsg(UpdateEventMsg.FieldDetailMsg.builder()
                                            .fieldKey(CardField.STORY_MAP_NODE_ID)
                                            .fieldMsg(statusFieldName)
                                            .fromMsg(schema.findCardStatusName(oldStatus))
                                            .toMsg(schema.findCardStatusName(status))
                                            .build())
                                    .build())
                            .cardDetail(cardDetail)
                            .build();
                    cardEvents.add(cardEvent);
                } catch (CodedException e) {
                    String msg = String.format("%s 卡片更新失败：%s", FieldUtil.getSeqNum(cardDetail), e.getMessage());
                    errMessages.add(msg);
                }
            }

            if (CollectionUtils.isNotEmpty(successCards)) {
                CardsUpdateEvent addEvents = SingleFieldBatchUpdateHelper.builder()
                        .user(user.getUsername())
                        .schema(schema)
                        .cardDao(cardDao)
                        .cardMapper(cardMapper)
                        .findCardById(findCardById)
                        .findCardsDescendant(cardQueryService::selectDescendant)
                        .findCardsAncestor(cardQueryService::selectAncestor)
                        .findPlanById(findPlanById)
                        .findStoryMapNodeById(storyMapQueryService::selectStoryMapNodeById)
                        .findStoryMapL2NodeInfoById(storyMapQueryService::selectStoryMapL2NodeInfoById)
                        .cards(successCards)
                        .cardsJson(cardDao.findAsMap(successCards.stream().map(Card::getId).collect(Collectors.toList())))
                        .projectQueryService(projectQueryService)
                        .projectId(projectId)
                        .build()
                        .update(CardField.STATUS, status);
                if (cardsUpdateEvent == null) {
                    cardsUpdateEvent = addEvents;
                } else {
                    cardsUpdateEvent.getCardEvents().addAll(addEvents.getCardEvents());
                }
            }
        }
        if (!toCardDetails.isEmpty()) {
            cardDao.saveOrUpdate(toCardDetails);
            eventDispatcher.asyncDispatch(() -> CardsUpdateEvent.builder()
                    .project(fieldRefValueLoader.project(projectId))
                    .user(user.getUsername())
                    .cardEvents(cardEvents)
                    .build());
            Project project = projectQueryService.select(projectId);
            webHookProjectCmdService.asyncSendCardWebHookMsg(toCardDetails, project, companyService.currentCompanyName(), user);
            cardNoticeService.noticeStatusFlow(toCardDetails, sourceCardProps, schema, user);
        }

        result.put("success", success);
        result.put("err", cardsMap.size() - success);
        result.put("errMsg", errMessages);
        return result;
    }

    private void checkRequiredFields(ProjectCardSchema schema, String status, Map<String, Object> cardDetail, CardType cardType) {
        List<String> lackFields = cardType.findRequiredFields(status).stream()
                .filter(f -> FieldUtil.isEmptyValue(cardDetail.get(f)))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(lackFields)) {
            throw new CodedException(ErrorCode.REQUIRED_FIELDS,
                    String.format("%s 卡片更新失败:必填字段:[%s]!", FieldUtil.getSeqNum(cardDetail), StringUtils.join(schema.fieldNames(lackFields), ",")), lackFields);
        }
    }

    public List<Card> batchCreateChildrenCard(Long projectId, Long parentCardId, Map<Long, Map<String, Object>> cardDetails, Boolean sendEmail, Boolean useTemplateContent) throws IOException {
        Card parentCard = cardMapper.selectByPrimaryKey(parentCardId);
        if (parentCard == null) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "子卡片不能没有父卡片！");
        }

        batchCheckAndFillCardDetail(projectId, cardDetails);
        return batchSave(projectId, parentCard, cardDetails, sendEmail, useTemplateContent);
    }

    private void batchCheckAndFillCardDetail(Long projectId, Map<Long, Map<String, Object>> cardDetails) {
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        FieldValueCheckHelper checker = FieldValueCheckHelper.builder()
                .projectId(projectId)
                .findCardById(cardQueryService::select)
                .findPlanById(planQueryService::select)
                .findStoryMapNodeById(storyMapQueryService::selectStoryMapNodeById)
                .build();
        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();
        OperationContext opContext = OperationContext.instance(userService.currentUserName());
        Consumer<Map<String, Object>> setCalcFields = cardDetail -> FieldCalcGraph.INSTANCE.calcCreate(cardDetail, opContext, schema, fieldRefValueLoader);
        cardDetails.forEach((draftId, cardDetail) -> {
            // remove invalid props; check type&status;
            schemaHelper.preProcessCardProps(schema, cardDetail);
            Iterator<Map.Entry<String, Object>> it = cardDetail.entrySet().iterator();
            while (it.hasNext()) {
                if (!SysFieldOpLimit.canOp(it.next().getKey(), SysFieldOpLimit.Op.CREATE)) {
                    it.remove();
                }
            }
            // check required fields
            String type = FieldUtil.toString(cardDetail.get(CardField.TYPE));
            CardType cardType = schema.findCardType(type);
            if (null == cardType || !cardType.isEnable()) {
                throw CodedException.ERROR_CARD_TYPE;
            }
            String status = FieldUtil.toString(cardDetail.get(CardField.STATUS));
            List<String> lackFields = cardType.findRequiredFields(status).stream()
                    .filter(f -> FieldUtil.isEmptyValue(cardDetail.get(f)))
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(lackFields)) {
                throw new CodedException(ErrorCode.REQUIRED_FIELDS,
                        String.format("必填字段:[%s]", StringUtils.join(schema.fieldNames(lackFields), ",")), lackFields);
            }

            // check fields values
            cardDetail.forEach((field, value) -> checker.check(schema.findCardField(field), value));

            setCalcFields.accept(cardDetail);
            cardDetail.put(CardField.INNER_TYPE, cardType.getInnerType());
        });
    }

    private List<Card> batchSave(Long projectId, Card parentCard, Map<Long, Map<String, Object>> draftCardDetails, Boolean sendEmail, Boolean useTemplateContent) throws IOException {
        List<Card> addCards = new ArrayList<>();
        LoginUser user = userService.currentUser();
        OperationContext opContext = OperationContext.instance(userService.currentUserName());
        CacheableFieldRefValueLoader fieldRefValueLoader = fieldRefValueLoader();
        Project project = fieldRefValueLoader.project(projectId);

        Long ancestorId = parentCard.getAncestorId() > 0 ? parentCard.getAncestorId() : parentCard.getId();

        //检查计划ID
        List<Long> planIds = draftCardDetails.values().stream().map(FieldUtil::getPlanId).collect(Collectors.toList());
        planQueryService.select(planIds).forEach(plan -> {
            fieldRefValueLoader.getLoadPlan().cache(plan.getId(), plan);
            if (BooleanUtils.isFalse(plan.getIsActive())) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "不能绑定已归档计划！");
            }
        });

        Set<Long> validDraftIds = cardDraftQueryService.validDraftIds(draftCardDetails.keySet());
        Map<Long, Card> cardsMap = new HashMap<>();

        //先保存mysql
        draftCardDetails.forEach((draftId, cardDetail) -> {
            Long seqNum = cardHelper.seqNum(projectId);
            String rank = cardHelper.nextRank(projectId);
            Long planId = FieldUtil.getPlanId(cardDetail);
            Card card = Card.builder()
                    .id(IdUtil.generateId())
                    .projectId(projectId)
                    .projectKey(project.getKey())
                    .seqNum(seqNum)
                    .planId(planId)
                    .parentId(parentCard.getId())
                    .ancestorId(ancestorId)
                    .rank(rank)
                    .companyId(companyService.currentCompany())
                    .deleted(false)
                    .maxCommentSeqNum(0L)
                    .storyMapNodeId(FieldUtil.getStoryMapNodeId(cardDetail))
                    .latestEventId(0L)
                    .build();
            cardMapper.insert(card);
            List<Long> relateCardIds = FieldUtil.toLongList(cardDetail.get(CardField.RELATED_CARD_IDS));
            relateCardIds.forEach(relateCardId -> {
                cardRelateRelMapper.insert(CardRelateRel.builder()
                        .id(validDraftIds.contains(draftId) ? draftId : IdUtil.generateId())
                        .cardId(card.getId())
                        .relatedCardId(relateCardId)
                        .build());
                cardRelateRelMapper.insert(CardRelateRel.builder()
                        .id(IdUtil.generateId())
                        .cardId(relateCardId)
                        .relatedCardId(card.getId())
                        .build());
                if (validDraftIds.contains(draftId)) {
                    // 草稿附件指向正式卡片
                    // cardAttachmentCmdService.commitDraftAttachment(draftId, card.getId());
                    try {
                        cardDraftCmdService.onCommit(draftId);
                    } catch (IOException e) {
                        log.error("cardDraftCmdService onCommit error", e);
                        throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, "删除草稿异常！" + e.getMessage());
                    }
                }
            });
            cardsMap.put(draftId, card);
            addCards.add(card);
        });

        if (useTemplateContent) {
            try {
                Map<String, Map<String, Object>> projectCardTemplate = projectCardTemplateService.getProjectCardTemplates(projectId);
                draftCardDetails.forEach((draftId, cardDetail) -> {
                    Map<String, Object> template = projectCardTemplate.get(FieldUtil.getType(cardDetail));
                    cardDetail.put(CardField.CONTENT, template == null ? "" : FieldUtil.getContent(template));
                });
            } catch (IOException e) {
                log.error("[batchSave][" + " projectId :" + projectId + "; parentCard :" + parentCard + "; draftCardDetails :" + draftCardDetails + "; sendEmail :" + sendEmail + "; useTemplateContent :" + useTemplateContent + "][error][" + e.getMessage() + "]", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }
        //保存卡片到es
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
        Consumer<Map<String, Object>> setCalcFields = cardDetail -> FieldCalcGraph.INSTANCE.calcCreate(cardDetail, opContext, schema, fieldRefValueLoader);
        Map<Long, Map<String, Object>> cardDetails = new HashMap<>();
        draftCardDetails.forEach((draftId, cardDetail) -> {
            Card card = cardsMap.get(draftId);
            CardHelper.setCardCreatedProps(cardDetail, user.getUsername(), card);
            setCalcFields.accept(cardDetail);
            cardDetails.put(card.getId(), cardDetail);
        });
        try {
            cardDao.saveOrUpdate(cardDetails);
        } catch (IOException e) {
            log.error("cardDao saveOrUpdate error", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, "保存卡片到es异常！" + e.getMessage());
        }
        //如果有关联卡片，更新原卡片关联到es
        draftCardDetails.forEach((draftId, cardDetail) -> {
            Card card = cardsMap.get(draftId);
            Long cardId = card.getId();
            //双向关联，更新es中被关联卡片的关系
            List<Long> relateCardIds = FieldUtil.toLongList(cardDetail.get(CardField.RELATED_CARD_IDS));
            if (CollectionUtils.isNotEmpty(relateCardIds)) {
                try {
                    Map<Long, Map<String, Object>> cards = cardDao.findAsMap(relateCardIds, CardField.RELATED_CARD_IDS);
                    Map<Long, Map<String, Object>> cardsProps = new HashMap<>();
                    cards.forEach((oldId, oldDetail) -> cardsProps.put(oldId, CardHelper.generatePropsForUpdate(
                            CardField.RELATED_CARD_IDS, CardHelper.addRelateCardId(oldDetail, cardId))));
                    cardDao.updateSelective(cardsProps);
                } catch (IOException e) {
                    log.error("cardDao updateSelective error", e);
                    throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, "更新es中被关联卡片的关系异常！" + e.getMessage());
                }
            }
        });

        //处理通知
        draftCardDetails.forEach((draftId, cardDetail) -> {
            Card card = cardsMap.get(draftId);
            eventDispatcher.dispatch(CardCreateEvent.builder().user(user.getUsername()).card(card).cardDetail(cardDetail).sendEmail(sendEmail).build());
            webHookProjectCmdService.asyncSendCardWebHookMsg(cardDetail, project, WebHookEventType.CREATE_CARD, companyService.currentCompanyName(), user);
            CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(card.getCompanyId());
            cardNoticeService.noticeAtUsersInCard(card, cardDetail, user.getUsername(), FieldUtil.getAtUsers(cardDetail), companyCardSchema, schema, ProjectNoticeConfig.Type.CREATE);

        });
        return addCards;
    }

    public CardIncrWorkloadResult incrWorkload(Card card, IncrWorkloadRequest request) throws IOException {
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(card.getProjectId());
        Map<String, Object> cardDetail = cardDao.findAsMap(card.getId());
        checkCanUpdateActualWorkload(schema, cardDetail);
        CardIncrWorkload workload = cardWorkloadBpmCmdService.incrWorkload(card, request);
        Float successActualWorkload = null;
        if (CardIncrWorkload.IncrResult.SUCCESS.equals(workload.getIncrResult())) {
            successActualWorkload = incrActualWorkload(schema, card, cardDetail, workload.getIncrHours());
        }
        return CardIncrWorkloadResult.builder()
                .workload(workload)
                .successActualWorkload(successActualWorkload)
                .build();
    }

    public CardIncrWorkloadResult approvalIncrWorkload(Card card, IncrWorkloadRequest request) throws IOException {
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(card.getProjectId());
        Map<String, Object> cardDetail = cardDao.findAsMap(card.getId());
        checkCanUpdateActualWorkload(schema, cardDetail);
        CardIncrWorkload workload = cardWorkloadBpmCmdService.approvalIncrWorkload(card, request);
        Float successActualWorkload = null;
        if (CardIncrWorkload.IncrResult.SUCCESS.equals(workload.getIncrResult())) {
            successActualWorkload = incrActualWorkload(schema, card, cardDetail, workload.getIncrHours());
        }
        return CardIncrWorkloadResult.builder()
                .workload(workload)
                .successActualWorkload(successActualWorkload)
                .build();
    }

    public CardIncrWorkloadResult updateIncrWorkload(Card card, Long workloadId, IncrWorkloadRequest request) throws IOException {
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(card.getProjectId());
        Map<String, Object> cardDetail = cardDao.findAsMap(card.getId());
        checkCanUpdateActualWorkload(schema, cardDetail);
        CardIncrWorkload workload = cardWorkloadBpmCmdService.updateIncrWorkload(card, workloadId, request);
        Float successActualWorkload = null;
        if (CardIncrWorkload.IncrResult.SUCCESS.equals(workload.getIncrResult())) {
            successActualWorkload = incrActualWorkload(schema, card, cardDetail, workload.getIncrHours());
        }
        return CardIncrWorkloadResult.builder()
                .workload(workload)
                .successActualWorkload(successActualWorkload)
                .build();
    }

    public CardIncrWorkloadResult approvalUpdateIncrWorkload(Card card, Long workloadId, IncrWorkloadRequest request) throws IOException {
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(card.getProjectId());
        Map<String, Object> cardDetail = cardDao.findAsMap(card.getId());
        checkCanUpdateActualWorkload(schema, cardDetail);
        CardIncrWorkload workload = cardWorkloadBpmCmdService.approvalUpdateIncrWorkload(card, workloadId, request);
        Float successActualWorkload = null;
        if (CardIncrWorkload.IncrResult.SUCCESS.equals(workload.getIncrResult())) {
            successActualWorkload = incrActualWorkload(schema, card, cardDetail, workload.getIncrHours());
        }
        return CardIncrWorkloadResult.builder()
                .workload(workload)
                .successActualWorkload(successActualWorkload)
                .build();
    }

    public CardIncrWorkloadResult revertWorkload(Card card, Long workloadId) throws IOException {
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(card.getProjectId());
        Map<String, Object> cardDetail = cardDao.findAsMap(card.getId());
        checkCanUpdateActualWorkload(schema, cardDetail);
        CardIncrWorkload workload = cardWorkloadBpmQueryService.cardWorkload(card.getId(), workloadId);
        float deltaWorkload = cardWorkloadBpmCmdService.revertWorkload(card, workloadId);
        float successActualWorkload = incrActualWorkload(schema, card, cardDetail, -deltaWorkload);
        return CardIncrWorkloadResult.builder()
                .workload(workload)
                .successActualWorkload(successActualWorkload)
                .build();
    }

    public CardIncrWorkloadResult approvalRevertWorkload(Card card, Long workloadId, BpmUserChoosesRequest bpmUserChoosesRequest) throws IOException {
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(card.getProjectId());
        Map<String, Object> cardDetail = cardDao.findAsMap(card.getId());
        checkCanUpdateActualWorkload(schema, cardDetail);
        CardIncrWorkload workload = cardWorkloadBpmQueryService.cardWorkload(card.getId(), workloadId);
        float deltaWorkload = cardWorkloadBpmCmdService.approvalRevertWorkload(card, workloadId, bpmUserChoosesRequest);
        float successActualWorkload = incrActualWorkload(schema, card, cardDetail, -deltaWorkload);
        return CardIncrWorkloadResult.builder()
                .workload(workload)
                .successActualWorkload(successActualWorkload)
                .build();
    }

    private CacheableFieldRefValueLoader fieldRefValueLoader() {
        return CacheableFieldRefValueLoader.builder()
                .loadProject(CacheableFunction.instance(projectQueryService::select))
                .loadPlan(CacheableFunction.instance(planQueryService::select))
                .loadStoryMap(CacheableFunction.instance(storyMapQueryService::selectStoryMapById))
                .loadStoryMapNode(CacheableFunction.instance(storyMapQueryService::selectStoryMapNodeById))
                .build();
    }

    private void checkCanUpdateActualWorkload(ProjectCardSchema schema, Map<String, Object> cardDetail) {
        CardType.FieldConf fieldConf = schema.findCardType(FieldUtil.getType(cardDetail)).findFieldConf(CardField.ACTUAL_WORKLOAD);
        if (fieldConf == null) {
            return;
        }
        CardType.FieldLimit fieldLimit = fieldConf.findStatusLimit(FieldUtil.getStatus(cardDetail));
        if (CardType.FieldLimit.READ_ONLY.equals(fieldLimit) || CardType.FieldLimit.HIDE.equals(fieldLimit)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "当前状态下实际工时为只读状态，请联系管理员将此状态下实际工时修改为选填、必填、推荐填写即可！");
        }
    }

}
