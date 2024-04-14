package com.ezone.ezproject.modules.card.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.limit.incr.IncrLimit;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardStatus;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectWorkloadSetting;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.ez.context.SystemSettingService;
import com.ezone.ezproject.modules.card.bean.BatchBindResponse;
import com.ezone.ezproject.modules.card.bean.BpmUserChoosesRequest;
import com.ezone.ezproject.modules.card.bean.CardBean;
import com.ezone.ezproject.modules.card.bean.CardBeanWithRef;
import com.ezone.ezproject.modules.card.bean.CardBpmFlowBean;
import com.ezone.ezproject.modules.card.bean.CardBpmFlowsBean;
import com.ezone.ezproject.modules.card.bean.CardIdToken;
import com.ezone.ezproject.modules.card.bean.CardIncrWorkloadResult;
import com.ezone.ezproject.modules.card.bean.CardRemindRequest;
import com.ezone.ezproject.modules.card.bean.CardWorkloadBpmFlowBean;
import com.ezone.ezproject.modules.card.bean.ChangeTypeRequest;
import com.ezone.ezproject.modules.card.bean.CreateCardAndBindRequest;
import com.ezone.ezproject.modules.card.bean.CreateCardAndBindResponse;
import com.ezone.ezproject.modules.card.bean.ExportRequest;
import com.ezone.ezproject.modules.card.bean.IncrWorkloadRequest;
import com.ezone.ezproject.modules.card.bean.QuickCreateCardRequest;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.card.bean.StatusBatchUpdateRequest;
import com.ezone.ezproject.modules.card.bean.TotalBeanAndToken;
import com.ezone.ezproject.modules.card.bean.UpdateCardsFieldsRequest;
import com.ezone.ezproject.modules.card.bean.query.Between;
import com.ezone.ezproject.modules.card.bean.query.Contains;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.bean.query.Exist;
import com.ezone.ezproject.modules.card.bean.query.Gt;
import com.ezone.ezproject.modules.card.bean.query.Gte;
import com.ezone.ezproject.modules.card.bean.query.In;
import com.ezone.ezproject.modules.card.bean.query.Keyword;
import com.ezone.ezproject.modules.card.bean.query.KeywordOrSeqNum;
import com.ezone.ezproject.modules.card.bean.query.Lt;
import com.ezone.ezproject.modules.card.bean.query.Lte;
import com.ezone.ezproject.modules.card.bean.query.NotContains;
import com.ezone.ezproject.modules.card.bean.query.NotEq;
import com.ezone.ezproject.modules.card.bean.query.NotExist;
import com.ezone.ezproject.modules.card.bean.query.NotIn;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.card.bean.query.SeqNumOrTitle;
import com.ezone.ezproject.modules.card.bpm.bean.CardBpmFlow;
import com.ezone.ezproject.modules.card.bpm.service.CardBpmCmdService;
import com.ezone.ezproject.modules.card.bpm.service.CardBpmQueryService;
import com.ezone.ezproject.modules.card.bpm.service.CardWorkloadBpmCmdService;
import com.ezone.ezproject.modules.card.bpm.service.CardWorkloadBpmQueryService;
import com.ezone.ezproject.modules.card.event.model.CardIncrWorkload;
import com.ezone.ezproject.modules.card.excel.ExcelCardImport;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.rank.RankLocation;
import com.ezone.ezproject.modules.card.service.CardCmdService;
import com.ezone.ezproject.modules.card.service.CardDocService;
import com.ezone.ezproject.modules.card.service.CardDraftCmdService;
import com.ezone.ezproject.modules.card.service.CardRankCmdService;
import com.ezone.ezproject.modules.card.service.CardReferenceValueHelper;
import com.ezone.ezproject.modules.card.service.CardSearchService;
import com.ezone.ezproject.modules.card.service.CardTestPlanService;
import com.ezone.ezproject.modules.card.service.CardWikiPageService;
import com.ezone.ezproject.modules.card.service.ProjectCardDailyLimiter;
import com.ezone.ezproject.modules.cli.EzTestCliService;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import com.ezone.galaxy.framework.common.bean.PageResult;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApiOperation("卡片")
@RestController
@RequestMapping("/project/card")
@Slf4j
@AllArgsConstructor
public class CardController extends AbstractCardController {
    private CardCmdService cardCmdService;

    private CardDraftCmdService cardDraftCmdService;

    private CardSearchService cardSearchService;

    private CardRankCmdService cardRankCmdService;

    private PlanQueryService planQueryService;

    private ProjectSchemaQueryService schemaService;

    private CardDocService cardDocService;

    private CardReferenceValueHelper cardReferenceValueHelper;

    private EzTestCliService ezTestCliService;

    private CardTestPlanService cardTestPlanService;

    private CardWikiPageService cardWikiPageService;

    private CardBpmCmdService cardBpmCmdService;
    private CardBpmQueryService cardBpmQueryService;

    private CardWorkloadBpmQueryService cardWorkloadBpmQueryService;
    private CardWorkloadBpmCmdService cardWorkloadBpmCmdService;

    private SystemSettingService systemSettingService;
    private CompanyProjectSchemaQueryService companyProjectSchemaQueryService;

    @ApiOperation("新建卡片")
    @PostMapping
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    @IncrLimit(domainResourceKey = ProjectCardDailyLimiter.DOMAIN_RESOURCE_KEY, domainId = "#projectId")
    public BaseResponse<Card> create(@ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId,
                                     @ApiParam(value = "创建卡片时是否发送邮件", example = "false") @RequestParam(required = false, defaultValue = "false") Boolean sendEmail,
                                     @RequestParam(required = false, defaultValue = "0") Long draftId,
                                     @NotNull @RequestBody Map<String, Object> card) throws IOException {
        checkCanCreateCard(projectId, card);
        List<Long> relateCardIds = FieldUtil.toLongList(card.get(CardField.RELATED_CARD_IDS));
        checkCompanyCards(companyService.currentCompany(), relateCardIds);
        return success(cardCmdService.completelyCreate(projectId, draftId, card, relateCardIds, sendEmail));
    }

    @ApiOperation("新建卡片并绑定wiki、doc等")
    @PostMapping("createAndBind")
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    @IncrLimit(domainResourceKey = ProjectCardDailyLimiter.DOMAIN_RESOURCE_KEY, domainId = "#projectId")
    public BaseResponse<CreateCardAndBindResponse> createAndBind(@ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId,
                                                                 @ApiParam(value = "创建卡片时是否发送邮件", example = "false") @RequestParam(required = false, defaultValue = "false") Boolean sendEmail,
                                                                 @RequestParam(required = false, defaultValue = "0") Long draftId,
                                                                 @NotNull @RequestBody CreateCardAndBindRequest request) throws IOException {
        Map<String, Object> card = request.getCard();
        checkCanCreateCard(projectId, card);
        List<Long> relateCardIds = FieldUtil.toLongList(card.get(CardField.RELATED_CARD_IDS));
        checkCompanyCards(companyService.currentCompany(), relateCardIds);
        Card created = cardCmdService.completelyCreate(projectId, draftId, card, relateCardIds, sendEmail);
        List<BatchBindResponse> bindResponses = new ArrayList<>();
        request.getBinds().forEach(batchBindRequest -> {
            switch (batchBindRequest.getBindType()) {
                case DOC:
                    bindResponses.add(cardDocService.batchBind(created.getId(), batchBindRequest));
                    break;
                case WIKI:
                    bindResponses.add(cardWikiPageService.batchBind(created.getId(), batchBindRequest));
                    break;
                case TEST_CASE:
                    bindResponses.add(ezTestCliService.batchBindCases(created.getId(), batchBindRequest));
                    break;
                case TEST_API:
                    bindResponses.add(ezTestCliService.batchUpdateBindApis(created.getId(), batchBindRequest));
                    break;
                case TEST_PLAN:
                    bindResponses.add(cardTestPlanService.batchBind(created.getId(), batchBindRequest));
                    break;
                default:
            }
        });
        CreateCardAndBindResponse response = CreateCardAndBindResponse.builder()
                .card(created)
                .binds(bindResponses)
                .build();
        return success(response);
    }

    @ApiOperation("新建草稿")
    @PostMapping("draft")
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<Long> create(@ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId) throws IOException {
        checkHasProjectRead(projectId);
        checkProjectActive(projectId, OperationType.CARD_CREATE);
        return success(cardDraftCmdService.create(projectId));
    }


    @ApiOperation("快速新建卡片")
    @PostMapping("quick")
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    @IncrLimit(domainResourceKey = ProjectCardDailyLimiter.DOMAIN_RESOURCE_KEY, domainId = "#projectId")
    public BaseResponse<Card> create(@ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId,
                                     @ApiParam(value = "创建卡片时是否发送邮件", example = "false") @RequestParam(required = false, defaultValue = "false") Boolean sendEmail,
                                     @NotNull @RequestBody QuickCreateCardRequest request) throws IOException {
        Map<String, Object> card = new HashMap<>();
        card.put(CardField.TYPE, request.getType());
        card.put(CardField.PLAN_ID, request.getPlanId());
        card.put(CardField.STORY_MAP_NODE_ID, request.getStoryMapNodeId());
        card.put(CardField.STATUS, CardStatus.FIRST);
        checkCanCreateCard(projectId, card);
        if (BooleanUtils.isTrue(project(projectId).getIsStrict())) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "快速创建卡片功能已关闭！");
        }
        return success(cardCmdService.quickCreate(projectId, request, sendEmail));
    }

    @ApiOperation("下载导入卡片模版")
    @GetMapping(path = "import/excel", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @CheckAuthType(TokenAuthType.READ)
    public void importExcelTemplate(@ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId,
                                    @ApiParam(value = "卡片类型", example = "story") @RequestParam List<String> cardTypes,
                                    HttpServletResponse response)
            throws IOException {
        checkHasProjectRead(projectId);
        checkProjectActive(projectId, OperationType.CARD_CREATE);
        response.setHeader("Content-disposition", "attachment;filename=import-card.xlsx");
        ProjectCardSchema schema = schemaService.getProjectCardSchema(projectId);
        ProjectWorkloadSetting workloadSetting = schemaService.getProjectWorkloadSetting(projectId);
        cardQueryService.writeImportExcelTemplate(schema, workloadSetting, cardTypes, response.getOutputStream());
    }

    @ApiOperation("导入卡片")
    @PostMapping("import/excel")
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<ExcelCardImport.Result> importWithExcel(@ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId,
                                                                @ApiParam(value = "默认计划ID", example = "1") @RequestParam Long defaultPlanId,
                                                                @ApiParam(value = "卡片类型", example = "story") @RequestParam List<String> cardTypes,
                                                                @ApiParam(value = "excel文件") @RequestParam("file") MultipartFile file)
            throws IOException {
        checkProjectActive(projectId, OperationType.CARD_CREATE);
        return success(cardCmdService.importByExcel(projectId, defaultPlanId, cardTypes, file.getInputStream()));
    }

    @ApiOperation("下载导入失败卡片的详细原因excel")
    @GetMapping(value = "import/fail/excel", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @CheckAuthType(TokenAuthType.READ)
    public ResponseEntity downloadImportFailExcel(@ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId,
                                                  @ApiParam(value = "错误文件路径，来自于导入卡片请求失败后返回的结果信息") @RequestParam String errorStoragePath) {
        checkHasProjectRead(projectId);
        return cardQueryService.downloadImportFailExcel(errorStoragePath);
    }

    @ApiOperation("更新卡片")
    @PutMapping("{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse update(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long id,
                               @NotNull @RequestBody Map<String, Object> card) throws IOException {
        Card cardInDb = cardQueryService.select(id);
        if (null == cardInDb) {
            throw CodedException.NOT_FOUND;
        }
        if (BooleanUtils.isTrue(cardInDb.getDeleted())) {
            throw CodedException.DELETED;
        }
        checkCanUpdateCard(cardInDb.getProjectId(), id, card);
        cardCmdService.completelyUpdate(cardInDb, card);
        return success(cardQueryService.selectCardBeanWithRef(cardInDb));
    }

    @ApiOperation("更新卡片")
    @PutMapping("{id:[0-9]+}/fields")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse updateFields(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long id,
                                     @NotNull @RequestBody Map<String, Object> cardProps) throws IOException {
        Card cardInDb = cardQueryService.select(id);
        if (null == cardInDb) {
            throw CodedException.NOT_FOUND;
        }
        if (BooleanUtils.isTrue(cardInDb.getDeleted())) {
            throw CodedException.DELETED;
        }
        checkCanUpdateCard(cardInDb.getProjectId(), id, cardProps);
        cardCmdService.updateFields(cardInDb, cardProps);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("更新卡片属性: 不支持直接修改卡片类型/编号/是否删除等特殊字段")
    @PutMapping("{id:[0-9]+}/fields/{field}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse updateField(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long id,
                                    @ApiParam(value = "字段", example = "1") @PathVariable String field,
                                    @RequestBody(required = false) String value) throws IOException {
        Card cardInDb = cardQueryService.select(id);
        if (null == cardInDb) {
            throw CodedException.NOT_FOUND;
        }
        if (BooleanUtils.isTrue(cardInDb.getDeleted())) {
            throw CodedException.DELETED;
        }
        checkCanUpdateCard(cardInDb.getProjectId(), id, field, value);
        cardCmdService.updateField(cardInDb, field, value);
        return success(cardQueryService.selectCardBeanWithRef(cardInDb));
    }

    @ApiOperation("批量修改卡片字段")
    @PutMapping("/fields/batch/{field}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse batchUpdateField(@ApiParam(value = "卡片ID", example = "1") @RequestBody Long[] ids,
                                         @ApiParam(value = "字段", example = "1") @PathVariable String field,
                                         @RequestParam String targetValue) throws IOException {
        List<Card> cardsInDb = cardQueryService.select(Arrays.asList(ids));
        cardsInDb.forEach(card -> {
            if (BooleanUtils.isTrue(card.getDeleted())) {
                throw CodedException.DELETED;
            }
        });
        Long projectId = checkInSameProject(cardsInDb);
        checkCanUpdateCard(projectId, cardsInDb.stream().map(Card::getId).collect(Collectors.toList()), field, targetValue);
        cardCmdService.batchUpdateField(cardsInDb, projectId, field, targetValue);

        return SUCCESS_RESPONSE;
    }

    @ApiOperation("更新卡片")
    @PutMapping("batchUpdateFields")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<Map<Long, String>> batchUpdateFields(
            @NotNull @RequestBody UpdateCardsFieldsRequest request) throws IOException {
        List<Card> cardsInDb = cardQueryService.select(request.getIds());
        if (CollectionUtils.isEmpty(cardsInDb)) {
            return SUCCESS_RESPONSE;
        }
        checkProjectActive(cardsInDb.get(0).getProjectId(), OperationType.CARD_UPDATE);
        Long projectId = checkInSameProject(cardsInDb);
        return success(cardCmdService.batchUpdateFields(projectId, cardsInDb, request));
    }

    private Long checkInSameProject(List<Card> cards) {
        if (CollectionUtils.isEmpty(cards)) {
            throw CodedException.NOT_FOUND;
        }
        Long projectId = cards.get(0).getProjectId();
        if (cards.stream().anyMatch(c -> !projectId.equals(c.getProjectId()))) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "卡片不属于同一项目!");
        }
        return projectId;
    }

    @ApiOperation("改变卡片类型")
    @PutMapping("changeType")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<Map<String, Object>> changeType(@ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId,
                                                        @ApiParam(value = "类型转换映射") @RequestBody ChangeTypeRequest changeTypeRequest)
            throws IOException {
        List<Card> cards = cardQueryService.select(changeTypeRequest.getIds());
        if (CollectionUtils.isEmpty(cards)) {
            return success(MapUtils.EMPTY_MAP);
        }
        checkProjectActive(cards.get(0).getProjectId(), OperationType.CARD_UPDATE);
        for (Card card : cards) {
            if (!card.getProjectId().equals(projectId)) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "卡片属于不同项目!");
            }
        }
        Map<String, Object> result = cardCmdService.changeCardType(projectId, changeTypeRequest);
        return success(result);
    }

    @ApiOperation("更新卡片流程状态;错误码5010:需要同时设置其它必填字段;错误码5011:需要发起审批流;错误码5013:审批中")
    @PutMapping("{id:[0-9]+}/status")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse setStatus(@ApiParam(value = "卡片ID", example = "1") @NotNull @PathVariable Long id,
                                  @ApiParam(value = "卡片流程状态", example = "open") @NotNull @RequestParam String status) throws IOException {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkCanUpdateCard(card.getProjectId(), card.getId(), CardField.STATUS, status);
        cardCmdService.updateField(card, CardField.STATUS, status);
        return success(cardQueryService.selectCardBeanWithRef(card));
    }

    @ApiOperation("更新卡片流程状态;若返回5010错误码说明目标状态合法但需要同时设置其它必选字段")
    @PutMapping("cardType/status/batch")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse setStatus(@RequestBody @Valid StatusBatchUpdateRequest request) throws IOException {
        List<Long> allCardId = new ArrayList();
        for (StatusBatchUpdateRequest.TypeCards typeCard : request.getTypeCards()) {
            allCardId.addAll(typeCard.getCardIds());
        }

        List<Card> cardsInDb = cardQueryService.select(allCardId);
        checkProjectActive(cardsInDb.get(0).getProjectId(), OperationType.CARD_UPDATE);
        Long projectId = checkInSameProject(cardsInDb);
        for (StatusBatchUpdateRequest.TypeCards typeCard : request.getTypeCards()) {
            for (Long id : typeCard.getCardIds()) {
                // todo 权限判断性能有问题，待重构
                checkCanUpdateCard(projectId, id, CardField.STATUS, typeCard.getStatus());
            }
        }

        Map<String, Object> result = cardCmdService.setStatus(request, cardsInDb, projectId);
        return success(result);
    }

    @ApiOperation("获取卡片流程状态审批记录")
    @GetMapping("{id:[0-9]+}/status/approval")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<CardBpmFlowsBean> approvalStatusFlows(@ApiParam(value = "卡片ID", example = "1") @NotNull @PathVariable Long id) throws IOException {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(card.getProjectId());
        return success(cardBpmQueryService.cardBpmFlows(card.getId()));
    }

    @ApiOperation("获取卡片流程状态审批记录")
    @GetMapping("{id:[0-9]+}/status/approval/{flowId:[0-9]]}")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<CardBpmFlowBean> approvalStatusFlow(
            @ApiParam(value = "卡片ID", example = "1") @NotNull @PathVariable Long id,
            @PathVariable Long flowId) throws IOException {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(card.getProjectId());
        return success(cardBpmQueryService.cardBpmFlowBean(card.getId(), flowId));
    }

    @ApiOperation("发起卡片流程状态审批;错误码5012:需要发起审批流;错误码5013:审批中")
    @PostMapping("{id:[0-9]+}/status/approval")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<CardBpmFlow> approvalSetStatus(@ApiParam(value = "卡片ID", example = "1") @NotNull @PathVariable Long id,
                                                       @ApiParam(value = "卡片流程状态", example = "open") @NotNull @RequestParam String status,
                                                       @Nullable @RequestBody BpmUserChoosesRequest userChooses) throws IOException {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        if (BooleanUtils.isTrue(card.getDeleted())) {
            throw CodedException.DELETED;
        }
        if (!systemSettingService.bpmIsOpen()) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "流程审批功能已关闭，请联系后台管理员开启流程审批功能后才能使用！");
        }
        checkCanUpdateCard(card.getProjectId(), card.getId(), CardField.STATUS, status);
        return success(cardBpmCmdService.approvalSetStatus(card, status, userChooses));
    }

    @ApiOperation("解绑卡片流程当前的状态审批")
    @DeleteMapping("{id:[0-9]+}/status/approval")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse cancelApprovalStatus(@ApiParam(value = "卡片ID", example = "1") @NotNull @PathVariable Long id) throws IOException {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        if (BooleanUtils.isTrue(card.getDeleted())) {
            throw CodedException.DELETED;
        }
        checkCanUpdateCard(card.getProjectId(), card.getId());
        cardBpmCmdService.cancelApprovalStatus(card);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("登记工时;错误码5012:需要发起审批流;")
    @PostMapping("{id:[0-9]+}/incrWorkload")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<CardIncrWorkloadResult> incrWorkLoad(
            @ApiParam(value = "卡片ID", example = "1") @NotNull @PathVariable Long id,
            @Nullable @RequestBody IncrWorkloadRequest request) throws IOException {
        request.checkTime();
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        if (BooleanUtils.isTrue(card.getDeleted())) {
            throw CodedException.DELETED;
        }
        checkCanUpdateCard(card.getProjectId(), card.getId(), CardField.ACTUAL_WORKLOAD, request.calcIncrHours());
        return success(cardCmdService.incrWorkload(card, request));
    }

    @ApiOperation("发起登记工时审批流")
    @PostMapping("{id:[0-9]+}/incrWorkload/approval")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<CardIncrWorkloadResult> approvalIncrWorkLoad(
            @ApiParam(value = "卡片ID", example = "1") @NotNull @PathVariable Long id,
            @Nullable @RequestBody IncrWorkloadRequest request) throws IOException {
        request.checkTime();
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        if (BooleanUtils.isTrue(card.getDeleted())) {
            throw CodedException.DELETED;
        }
        checkCanUpdateCard(card.getProjectId(), card.getId(), CardField.ACTUAL_WORKLOAD, request.calcIncrHours());
        return success(cardCmdService.approvalIncrWorkload(card, request));
    }

    @ApiOperation("取消取消登记工时流程审批")
    @DeleteMapping("{id:[0-9]+}/incrWorkload/{workloadId:[0-9]+}/approval")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse cancelApprovalIncrWorkLoad(
            @ApiParam(value = "卡片ID", example = "1") @NotNull @PathVariable Long id,
            @PathVariable Long workloadId) throws IOException {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        if (BooleanUtils.isTrue(card.getDeleted())) {
            throw CodedException.DELETED;
        }
        checkCanUpdateCard(card.getProjectId(), card.getId());
        cardWorkloadBpmCmdService.cancelApprovalIncrWorkload(id, workloadId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("直接修改登记工时;错误码5012:需要发起审批流;")
    @PutMapping("{id:[0-9]+}/incrWorkload/{workloadId:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<CardIncrWorkloadResult> updateIncrWorkLoad(
            @ApiParam(value = "卡片ID", example = "1") @NotNull @PathVariable Long id,
            @ApiParam(value = "登记工时ID", example = "1") @NotNull @PathVariable Long workloadId,
            @Nullable @RequestBody IncrWorkloadRequest request) throws IOException {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        if (BooleanUtils.isTrue(card.getDeleted())) {
            throw CodedException.DELETED;
        }
        checkCanUpdateCard(card.getProjectId(), card.getId(), CardField.ACTUAL_WORKLOAD, 0f);
        return success(cardCmdService.updateIncrWorkload(card, workloadId, request));
    }

    @ApiOperation("发审批类修改被拒绝的登记工时")
    @PutMapping("{id:[0-9]+}/incrWorkload/{workloadId:[0-9]+}/approval")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<CardIncrWorkloadResult> approvalUpdateIncrWorkLoad(
            @ApiParam(value = "卡片ID", example = "1") @NotNull @PathVariable Long id,
            @ApiParam(value = "登记工时ID", example = "1") @NotNull @PathVariable Long workloadId,
            @Nullable @RequestBody IncrWorkloadRequest request) throws IOException {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        if (BooleanUtils.isTrue(card.getDeleted())) {
            throw CodedException.DELETED;
        }
        checkCanUpdateCard(card.getProjectId(), card.getId(), CardField.ACTUAL_WORKLOAD, 0f);
        return success(cardCmdService.approvalUpdateIncrWorkload(card, workloadId, request));
    }

    @ApiOperation("直接取消登记工时;错误码5012:需要发起审批流;")
    @DeleteMapping("{id:[0-9]+}/incrWorkload/{workloadId:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<CardIncrWorkloadResult> revertWorkLoad(
            @ApiParam(value = "卡片ID", example = "1") @NotNull @PathVariable Long id,
            @ApiParam(value = "登记工时ID", example = "1") @NotNull @PathVariable Long workloadId) throws IOException {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        if (BooleanUtils.isTrue(card.getDeleted())) {
            throw CodedException.DELETED;
        }
        checkCanUpdateCard(card.getProjectId(), card.getId(), CardField.ACTUAL_WORKLOAD, 0f);
        return success(cardCmdService.revertWorkload(card, workloadId));
    }

    @ApiOperation("发起审批来取消已登记工时")
    @PostMapping("{id:[0-9]+}/incrWorkload/{workloadId:[0-9]+}/revert/approval")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<CardIncrWorkloadResult> approvalRevertWorkLoad(
            @ApiParam(value = "卡片ID", example = "1") @NotNull @PathVariable Long id,
            @ApiParam(value = "登记工时ID", example = "1") @NotNull @PathVariable Long workloadId,
            @Nullable @RequestBody BpmUserChoosesRequest bpmUserChoosesRequest) throws IOException {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        if (BooleanUtils.isTrue(card.getDeleted())) {
            throw CodedException.DELETED;
        }
        checkCanUpdateCard(card.getProjectId(), card.getId(), CardField.ACTUAL_WORKLOAD, 0f);
        return success(cardCmdService.approvalRevertWorkload(card, workloadId, bpmUserChoosesRequest));
    }

    @ApiOperation("取消取消登记工时流程审批")
    @DeleteMapping("{id:[0-9]+}/incrWorkload/{workloadId:[0-9]+}/revert/approval")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse cancelApprovalRevertWorkLoad(
            @ApiParam(value = "卡片ID", example = "1") @NotNull @PathVariable Long id,
            @PathVariable Long workloadId) throws IOException {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        if (BooleanUtils.isTrue(card.getDeleted())) {
            throw CodedException.DELETED;
        }
        checkCanUpdateCard(card.getProjectId(), card.getId());
        cardWorkloadBpmCmdService.cancelApprovalRevertWorkload(id, workloadId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("获取卡片登记工时审批记录")
    @GetMapping("{id:[0-9]+}/incrWorkload")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<CardIncrWorkload>> approvalWorkloadFlows(
            @ApiParam(value = "卡片ID", example = "1") @NotNull @PathVariable Long id) throws IOException {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(card.getProjectId());
        return success(cardWorkloadBpmQueryService.cardIncrWorkloads(id));
    }

    @ApiOperation("获取卡片登记工时记录")
    @GetMapping("{id:[0-9]+}/incrWorkload/{workloadId:[0-9]]}")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<CardWorkloadBpmFlowBean> approvalWorkload(
            @ApiParam(value = "卡片ID", example = "1") @NotNull @PathVariable Long id,
            @PathVariable Long flowId) throws IOException {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(card.getProjectId());
        return success(cardWorkloadBpmQueryService.cardWorkloadBpmFlowBean(id, flowId));
    }

    @ApiOperation("更新卡片排序位置")
    @PutMapping("rankByCardRank")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<Map<Long, String>> rankByCardRank(@ApiParam(value = "卡片ID列表(降序)") @RequestParam Long[] ids,
                                                          @NotNull @ApiParam(value = "参照卡片ID", example = "1") @RequestParam Long referenceCardId,
                                                          @NotNull @ApiParam(value = "相对参照卡片排序位置更高/更低") @RequestParam RankLocation location) {
        Card card = cardQueryService.select(referenceCardId);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        if (BooleanUtils.isTrue(card.getDeleted())) {
            throw CodedException.DELETED;
        }
        checkPermission(card.getProjectId(), OperationType.CARD_SORT);
        return success(cardRankCmdService.rank(card.getProjectId(), Arrays.asList(ids), card.getRank(), location));
    }

    @ApiOperation("更新卡片排序位置")
    @PutMapping("rankByPlanDeliverLineRank")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<Map<Long, String>> rankByPlanDeliverLineRank(@ApiParam(value = "卡片ID列表(降序)") @RequestBody Long[] ids,
                                                                     @NotNull @ApiParam(value = "项目计划ID", example = "1") @RequestParam Long planId,
                                                                     @NotNull @ApiParam(value = "相对计划交付线排序位置更高/更低") @RequestParam RankLocation location) {
        Plan plan = planQueryService.select(planId);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        checkPermission(plan.getProjectId(), OperationType.CARD_SORT);
        return success(cardRankCmdService.rank(plan.getProjectId(), Arrays.asList(ids), plan.getDeliverLineRank(), location));
    }

    @ApiOperation("批量迁移卡片到同项目下的另一个计划")
    @PutMapping("migrate")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<Integer> migrate(@ApiParam(value = "卡片ID", example = "1") @RequestBody Long[] ids,
                                         @ApiParam(value = "目标项目ID", example = "1") @RequestParam Long targetProjectId,
                                         @ApiParam(value = "目标计划ID", example = "1") @RequestParam(defaultValue = "0") Long targetPlanId,
                                         @ApiParam(value = "迁移到新计划后重排序策略") @RequestParam RankLocation location) throws IOException {
        checkProjectActive(targetProjectId, OperationType.CARD_UPDATE);
        int count = cardCmdService.migrate(Arrays.asList(ids), targetProjectId, targetPlanId, location);
        return success(count);
    }

    @ApiOperation("批量复制卡片到另一个计划")
    @PutMapping("copy")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse copy(@ApiParam(value = "卡片ID", example = "1") @RequestBody Long[] ids,
                             @ApiParam(value = "目标项目ID", example = "1") @RequestParam Long targetProjectId,
                             @ApiParam(value = "目标计划ID", example = "1") @RequestParam(defaultValue = "0") Long targetPlanId,
                             @ApiParam(value = "复制到新计划后的排序策略") @RequestParam RankLocation location,
                             @ApiParam(value = "是否删除源卡片") @RequestParam boolean isDeleteSourceCards) throws IOException {
        int count = cardCmdService.copy(Arrays.asList(ids), targetProjectId, targetPlanId, location, isDeleteSourceCards, this::checkProjectActive);
        return success(count);
    }

    @ApiOperation("获取卡片详情")
    @GetMapping(value = "{id:[0-9]+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<CardBeanWithRef> select(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long id) throws IOException {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw new CodedException(HttpStatus.NOT_FOUND, "卡片不存在！");
        }
        checkHasProjectRead(card.getProjectId());
        return success(cardQueryService.selectCardBeanWithRef(card));
    }

    @ApiOperation("获取卡片详情")
    @GetMapping(value = "{projectKey:.+}-{seqNum:[0-9]+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<CardBeanWithRef> select(@ApiParam(value = "项目标示", example = "1") @PathVariable String projectKey,
                                                @ApiParam(value = "卡片编号", example = "1") @PathVariable Long seqNum) throws IOException {
        Card card = cardQueryService.select(companyService.currentCompany(), projectKey, seqNum);
        if (null == card) {
            throw new CodedException(HttpStatus.NOT_FOUND, "卡片不存在！");
        }
        checkHasProjectRead(card.getProjectId());
        return success(cardQueryService.selectCardBeanWithRef(card));
    }

    @ApiOperation("查询项目计划下卡片")
    @PostMapping("searchByPlan")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<TotalBean<CardBean>> searchByPlan(
            @ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId,
            @ApiParam(value = "计划ID，查未计划卡片传0", example = "1") @RequestParam(required = false, defaultValue = "0") Long planId,
            @ApiParam(value = "是否包含子孙计划") @RequestParam(required = false) boolean containsDescendantPlan,
            @RequestBody SearchEsRequest searchCardRequest,
            HttpServletResponse response) throws IOException {
        checkHasProjectRead(projectId);
        TotalBean<CardBean> totalBean;
        if (planId.equals(0L)) {
            totalBean = cardSearchService.searchNoPlan(projectId, searchCardRequest);
        } else {
            Plan plan = planQueryService.select(planId);
            if (null == plan) {
                throw CodedException.NOT_FOUND;
            }
            if (!plan.getProjectId().equals(projectId)) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "计划不属于项目!");
            }
            totalBean = cardSearchService.search(plan, containsDescendantPlan, searchCardRequest);
        }
        response.addHeader(PageResult.PAGE_TOTAL_COUNT, String.valueOf(totalBean.getTotal()));
        return success(totalBean);
    }

    @ApiOperation("查询项目计划下卡片")
    @PostMapping("searchByActiveAndNoPlan")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<TotalBean<CardBean>> searchByActiveAndNoPlan(
            @ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId,
            @RequestBody SearchEsRequest searchCardRequest,
            HttpServletResponse response) throws IOException {
        checkHasProjectRead(projectId);
        TotalBean<CardBean> totalBean = cardSearchService.searchByActiveAndNoPlan(projectId, searchCardRequest);
        response.addHeader(PageResult.PAGE_TOTAL_COUNT, String.valueOf(totalBean.getTotal()));
        return success(totalBean);
    }

    @ApiOperation("查询卡片")
    @PostMapping("searchByIds")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<TotalBean<CardBean>> searchByIds(@ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId,
                                                         @ApiParam(value = "卡片ID", example = "1") @RequestBody Long[] cardIds,
                                                         @ApiParam(value = "卡片字段") @RequestParam String[] fields)
            throws IOException {
        checkHasProjectRead(projectId);
        TotalBean<CardBean> totalBean = cardSearchService.search(projectId, Arrays.asList(cardIds), fields);
        return success(totalBean);
    }

    @ApiOperation("查询当前公司下当前用户有权限的项目中的卡片（企业管理员查询整个企业）")
    @PostMapping("searchByMemberProject")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<TotalBean<CardBean>> searchByProject(@RequestParam(required = false, defaultValue = "1") Integer pageNumber,
                                                             @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                                             @RequestBody SearchEsRequest searchCardRequest,
                                                             HttpServletResponse response) throws IOException {
        TotalBean<CardBean> totalBean = cardSearchService.searchUserProjectCards(searchCardRequest, false, pageNumber, pageSize);
        cardReferenceValueHelper.tryLoadFullProjectSchemas(totalBean.getRefs(), searchCardRequest.getFields());
        cardReferenceValueHelper.tryLoadProjectAlarm(totalBean.getRefs());
        response.addHeader(PageResult.PAGE_TOTAL_COUNT, String.valueOf(totalBean.getTotal()));
        return success(totalBean);
    }


    @ApiOperation("查询关联卡片")
    @GetMapping("searchRelateCards")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<TotalBean<CardBean>> searchRelateCards(@ApiParam(value = "卡片ID", example = "1") @RequestParam Long cardId,
                                                               @ApiParam(value = "卡片字段") @RequestParam String[] fields)
            throws IOException {
        Card card = cardQueryService.select(cardId);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(card.getProjectId());
        TotalBean<CardBean> totalBean = cardSearchService.searchRelateCards(cardId, fields);
        cardReferenceValueHelper.tryLoadProjectSchemas(totalBean.getRefs(), fields);
        cardReferenceValueHelper.tryLoadProjectAlarm(totalBean.getRefs());
        return success(totalBean);
    }

    @ApiOperation("查询卡片及其上下游层级卡片")
    @GetMapping("searchUpDownCards")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<TotalBean<CardBean>> searchUpDownCards(@ApiParam(value = "卡片ID", example = "1") @RequestParam Long cardId,
                                                               @ApiParam(value = "卡片字段") @RequestParam String[] fields)
            throws IOException {
        Card card = cardQueryService.select(cardId);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(card.getProjectId());
        TotalBean<CardBean> totalBean = cardSearchService.searchTreeCards(card, fields);
        return success(totalBean);
    }

    @ApiOperation("查询计划下卡片数量；用于归档或删除计划前检查相关卡片")
    @GetMapping("countByAncestorPlan")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<Long> countByAncestorPlan(@ApiParam(value = "计划ID", example = "1") @RequestParam Long planId,
                                                  @ApiParam(value = "仅统计非完成状态的卡片") @RequestParam(required = false, defaultValue = "false") boolean onlyNotEnd) throws IOException {
        Plan plan = planQueryService.select(planId);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(plan.getProjectId());
        if (onlyNotEnd) {
            return success(cardSearchService.countNotEnd(plan));
        } else {
            return success(cardQueryService.countByAncestorPlan(plan));
        }
    }

    @ApiOperation("查询项目下卡片")
    @PostMapping("searchByProject")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<TotalBean<CardBean>> searchByProject(@ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId,
                                                             @ApiParam(value = "是否删除到回收站的卡片") @RequestParam(required = false) Boolean deleted,
                                                             @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
                                                             @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                                             @RequestBody SearchEsRequest searchCardRequest,
                                                             HttpServletResponse response) throws IOException {
        checkHasProjectRead(projectId);
        TotalBean<CardBean> totalBean = cardSearchService.search(projectId, searchCardRequest, deleted, pageNumber, pageSize);
        response.addHeader(PageResult.PAGE_TOTAL_COUNT, String.valueOf(totalBean.getTotal()));
        return success(totalBean);
    }

    @ApiOperation("Query子类示例")
    @GetMapping("searchQueryExamples")
    @CheckAuthType(TokenAuthType.READ)
    public SearchEsRequest searchQueryExamples() {
        return SearchEsRequest.builder().queries(QUERY_EXAMPLES).build();
    }

    @ApiOperation("导出卡片")
    @PostMapping(path = "export/excel", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @CheckAuthType(TokenAuthType.READ)
    public void importExcelTemplate(@ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId,
                                    @Valid ExportRequest request,
                                    HttpServletResponse response)
            throws IOException {
        checkHasProjectRead(projectId);
        response.setHeader("Content-disposition", "attachment;filename=export-cards.xlsx");
        ProjectCardSchema schema = schemaService.getProjectCardSchema(projectId);
        cardQueryService.writeExportExcel(projectId, schema, request, response.getOutputStream());
    }

    @ApiOperation("逻辑删除")
    @DeleteMapping("{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse delete(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long id) throws IOException {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkCardLimitPermission(card.getProjectId(), OperationType.CARD_DELETE, id);
        cardCmdService.inactive(card);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("批量逻辑删除")
    @PostMapping("batch/delete")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<Integer> delete(@ApiParam(value = "卡片ID", example = "1") @RequestBody Long[] ids) throws IOException {
        List<Card> cards = cardQueryService.select(Arrays.asList(ids));
        Long projectId = checkInSameProject(cards);
        checkProjectActive(projectId, OperationType.CARD_DELETE);
        int count = cardCmdService.inactive(projectId, cards);
        return success(count);
    }

    @ApiOperation("还原")
    @PutMapping("{id:[0-9]+}/recovery")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse recovery(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long id) throws IOException {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkPermission(card.getProjectId(), OperationType.CARD_RECOVERY);
        cardCmdService.recovery(card);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("批量还原")
    @PostMapping("batch/recovery")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse recovery(@ApiParam(value = "卡片ID", example = "1") @RequestBody Long[] ids) throws IOException {
        List<Card> cards = cardQueryService.select(Arrays.asList(ids));
        if (CollectionUtils.isEmpty(cards)) {
            throw CodedException.NOT_FOUND;
        }
        Long projectId = cards.get(0).getProjectId();
        checkPermission(projectId, OperationType.CARD_RECOVERY);
        if (cards.stream().anyMatch(c -> !projectId.equals(c.getProjectId()))) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "Cards not in same project!");
        }
        cardCmdService.recovery(projectId, cards);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("催办")
    @PutMapping("{id:[0-9]+}/remind")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse remind(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long id,
                               @NotNull @RequestBody CardRemindRequest remindRequest) throws IOException {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(card.getProjectId());
        cardCmdService.remind(card, remindRequest);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("更新卡片在故事地图中的位置")
    @PutMapping("{id:[0-9]+}/changeStoryMapLocation")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse changeStoryMapLocation(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long id,
                                               @ApiParam(value = "目标计划ID", example = "1") @RequestParam Long targetPlanId,
                                               @ApiParam(value = "目标分类ID", example = "1") @RequestParam Long targetStoryMapNodeId) throws IOException {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkCanUpdateCard(card.getProjectId(), card.getId(), CardField.PLAN_ID, targetPlanId, CardField.STORY_MAP_NODE_ID, targetStoryMapNodeId);
        cardCmdService.changeStoryMapLocation(card, targetStoryMapNodeId, targetPlanId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("获取查询范围内用到的卡片类型列表")
    @PostMapping("listCardType")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<String>> listCardTypeByQuery(@RequestBody List<Query> queries) throws IOException {
        Long companyId = companyService.currentCompany();
        return success(cardSearchService.searchCardTypeByCompany(companyId, queries));
    }

    @ApiOperation("获取卡片accessToken")
    @GetMapping(value = "{projectKey:.+}-{seqNum:[0-9]+}/accessToken", produces = MediaType.APPLICATION_JSON_VALUE)
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<CardIdToken> cardAccessToken(
            @ApiParam(value = "项目标示", example = "1") @PathVariable String projectKey,
            @ApiParam(value = "卡片编号", example = "1") @PathVariable Long seqNum,
            @RequestParam Long accessFrom) throws IOException {
        Card card = cardQueryService.select(companyService.currentCompany(), projectKey, seqNum);
        if (null == card) {
            throw new CodedException(HttpStatus.NOT_FOUND, "卡片不存在！");
        }
        checkHasProjectRead(card.getProjectId());
        return success(CardIdToken.builder()
                .id(card.getId())
                .token(cardHelper.cardAccessToken(accessFrom, cardQueryService.selectEnsuredCardToken(card)))
                .build());
    }

    @ApiOperation("查询卡片")
    @PostMapping("searchCardsByAccessToken")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<TotalBean<CardBean>> searchUpDownCards(
            @RequestParam Long accessFrom,
            @ApiParam(value = "卡片字段") @RequestParam String[] fields,
            @ApiParam(value = "key: cardId, value: accessToken") @RequestBody List<CardIdToken> cardAccessTokens) throws IOException {
        Set<CardIdToken> validAccessTokens = validAccessTokens(accessFrom, cardAccessTokens);
        TotalBean<CardBean> totalBean = cardSearchService.search(validAccessTokens.stream().map(CardIdToken::getId).collect(Collectors.toList()), fields);
        cardReferenceValueHelper.tryLoadFullProjectSchemas(totalBean.getRefs(), fields);
        cardReferenceValueHelper.tryLoadProjectAlarm(totalBean.getRefs());
        return success(new TotalBeanAndToken<>(totalBean, validAccessTokens));
    }

    private void checkCompanyCards(Long companyId, List<Long> cardIds) {
        if (CollectionUtils.isEmpty(cardIds)) {
            return;
        }
        List<Card> cards = cardQueryService.select(cardIds);
        if (CollectionUtils.isEmpty(cards) || cards.size() < cardIds.size()) {
            throw CodedException.NOT_FOUND;
        }
        Set<Long> projectIds = new HashSet<>();
        cards.forEach(card -> {
            if (!card.getCompanyId().equals(companyId)) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "卡片不在公司下！");
            }
            projectIds.add(card.getProjectId());
        });
        projectIds.forEach(this::checkHasProjectRead);
    }

    private static final List<Query> QUERY_EXAMPLES = Arrays.asList(
            Between.builder().field("f1").start("1").end("2").build(),
            Contains.builder().field("f1").values("v1 v2").build(),
            Eq.builder().field("f1").value("abc").build(),
            Exist.builder().field("f1").build(),
            Gt.builder().field("f1").value("1").build(),
            Gte.builder().field("f1").value("1").build(),
            In.builder().field("f1").values(Arrays.asList("a", "b")).build(),
            Keyword.builder().values("a b").build(),
            KeywordOrSeqNum.builder().values("a b").build(),
            Lt.builder().field("f1").value("1").build(),
            Lte.builder().field("f1").value("1").build(),
            NotContains.builder().field("f1").values("v1 v2").build(),
            NotEq.builder().field("f1").value("abc").build(),
            NotExist.builder().field("f1").build(),
            NotIn.builder().field("f1").values(Arrays.asList("a", "b")).build(),
            SeqNumOrTitle.builder().values("abc").build()
    );


    @ApiOperation("批量新建子卡片")
    @PostMapping("batchCreateChildrenCard")
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    @IncrLimit(domainResourceKey = ProjectCardDailyLimiter.DOMAIN_RESOURCE_KEY, domainId = "#projectId")
    public BaseResponse<List<Card>> batchCreateChildrenCard(
            @ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId,
            @ApiParam(value = "创建卡片时是否发送邮件", example = "false") @RequestParam(required = false, defaultValue = "false") Boolean sendEmail,
            @ApiParam(value = "是否使用模板默认内容", example = "true") @RequestParam(required = false, defaultValue = "true") Boolean useTemplateContent,
            @NotNull @RequestBody Map<Long, Map<String, Object>> cards) throws IOException {
        Long parentCardId = null;
        checkCanCreateCards(projectId, cards);
        for (Map<String, Object> cardProps : cards.values()) {
            List<Long> relateCardIds = FieldUtil.toLongList(cardProps.get(CardField.RELATED_CARD_IDS));
            checkCompanyCards(companyService.currentCompany(), relateCardIds);
            Long tempParenId = FieldUtil.getParenId(cardProps);
            if (tempParenId == null) {
                throw new CodedException(HttpStatus.BAD_REQUEST, "未指定父卡片！");
            }
            parentCardId = parentCardId == null ? tempParenId : parentCardId;
            if (!parentCardId.equals(tempParenId)) {
                throw new CodedException(HttpStatus.BAD_REQUEST, "创建的子卡片需指定同一父卡片！");
            }
        }

        return success(cardCmdService.batchCreateChildrenCard(projectId, parentCardId, cards, sendEmail, useTemplateContent));
    }
}
