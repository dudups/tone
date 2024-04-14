package com.ezone.ezproject.modules.card.bpm.service;

import com.ezone.ezbase.iam.bean.LoginUser;
import com.ezone.ezbase.iam.service.IAMCenterService;
import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.exception.ErrorCode;
import com.ezone.ezproject.common.transactional.AfterCommit;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.mapper.CardMapper;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.dao.CardIncrWorkloadDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.ProjectWorkloadSetting;
import com.ezone.ezproject.ez.context.SystemSettingService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bean.BpmUserChoosesRequest;
import com.ezone.ezproject.modules.card.bean.IncrWorkloadRequest;
import com.ezone.ezproject.modules.card.event.model.CardIncrWorkload;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.service.CardMessageHelper;
import com.ezone.ezproject.modules.cli.EzBpmCliService;
import com.ezone.ezproject.modules.cli.bean.ApprovalTarget;
import com.ezone.ezproject.modules.cli.bean.ApprovalType;
import com.ezone.ezproject.modules.cli.bean.CreateBpmFlow;
import com.ezone.ezproject.modules.cli.bean.CreateBpmFlowResult;
import com.ezone.ezproject.modules.cli.bean.CreateWorkloadBpmFlow;
import com.ezone.ezproject.modules.common.EndpointHelper;
import com.ezone.ezproject.modules.project.service.ProjectCardSchemaHelper;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Service
@Slf4j
@AllArgsConstructor
public class CardWorkloadBpmCmdService {
    public static final ObjectMapper JSON_MAPPER = CardDao.JSON_MAPPER;

    private CardMapper cardMapper;
    private CardDao cardDao;
    private CardIncrWorkloadDao cardIncrWorkloadDao;

    private CardWorkloadBpmQueryService cardWorkloadBpmQueryService;

    private UserService userService;

    private ProjectQueryService projectQueryService;
    private ProjectSchemaQueryService projectSchemaQueryService;

    private EndpointHelper endpointHelper;
    private ProjectCardSchemaHelper schemaHelper;

    private EzBpmCliService ezBpmCliService;

    private CardMessageHelper cardMessageHelper;

    private IAMCenterService iamCenterService;
    private SystemSettingService systemSettingService;

    @AfterCommit
    @Async
    public void asyncCancelFlow(Long flowId, String user) {
        ezBpmCliService.cancelFlow(flowId, user);
    }

    @AfterCommit
    @Async
    public void asyncCancelFlows(List<Long> flowIds, String user) {
        ezBpmCliService.cancelFlows(flowIds, user);
    }

    public CardIncrWorkload incrWorkload(Card card, IncrWorkloadRequest request) throws IOException {
        ProjectWorkloadSetting workloadSetting = projectSchemaQueryService.getProjectWorkloadSetting(card.getProjectId());
        boolean isEnableIncrWorkload = workloadSetting != null && workloadSetting.isEnableIncrWorkload();
        if (!isEnableIncrWorkload) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "未开启工时登记");
        }
        if (systemSettingService.bpmIsOpen()) {
            Map<String, Object> cardDetail = cardDao.findAsMap(card.getId(), CardField.TYPE, CardField.TITLE);
            ProjectWorkloadSetting.Rule rule = workloadSetting.findMatchRule(FieldUtil.getType(cardDetail));
            if (rule != null) {
                throw new CodedException(ErrorCode.REQUIRED_BPM_FLOW, "需要发起审批");
            }
        }
        return createIncrWorkload(card, request);
    }

    public CardIncrWorkload approvalIncrWorkload(Card card, IncrWorkloadRequest request) throws IOException {
        if (!systemSettingService.bpmIsOpen()) {
            return createIncrWorkload(card, request);
        }
        ProjectWorkloadSetting workloadSetting = projectSchemaQueryService.getProjectWorkloadSetting(card.getProjectId());
        boolean isEnableIncrWorkload = workloadSetting != null && workloadSetting.isEnableIncrWorkload();
        if (!isEnableIncrWorkload) {
            return createIncrWorkload(card, request);
        }
        Map<String, Object> cardDetail = cardDao.findAsMap(card.getId(), CardField.TYPE, CardField.TITLE);
        ProjectWorkloadSetting.Rule rule = workloadSetting.findMatchRule(FieldUtil.getType(cardDetail));
        if (rule == null) {
            return createIncrWorkload(card, request);
        }
        Long bpmFlowTemplateId = rule.getBpmFlowTemplateId();
        BpmUserChoosesRequest userChoosesRequest = request.getBpmUserChoosesRequest();
        CreateBpmFlow.ApproverChooseRequest[] userChoose = null;
        if (userChoosesRequest == null) {
            userChoose = new CreateBpmFlow.ApproverChooseRequest[0];
        } else {
            userChoose = userChoosesRequest.getUserChooses();
        }
        Project project = projectQueryService.select(card.getProjectId());
        String cardTitle = FieldUtil.getTitle(cardDetail);
        LoginUser user = userService.currentUser();
        CreateBpmFlowResult result = ezBpmCliService.createFlow(CreateWorkloadBpmFlow.builder()
                .approvalType(ApprovalType.CARD_INCR_WORKLOAD)
                .callbackData(ApprovalTarget.builder()
                        .approvalType(ApprovalType.CARD_INCR_WORKLOAD)
                        .targetId(card.getId())
                        .build())
                .approvalFlowId(bpmFlowTemplateId)
                .companyId(card.getCompanyId())
                .username(user.getUsername())
                .projectApproval(CreateWorkloadBpmFlow.ProjectApproval.builder()
                        .projectId(project.getId())
                        .projectKey(project.getKey())
                        .projectName(project.getName())
                        .cardId(card.getId())
                        .cardSeqNum(card.getSeqNum())
                        .cardTitle(cardTitle)
                        .cardUrl(endpointHelper.cardDetailUrl(card.getCompanyId(), card.getProjectKey(), card.getSeqNum()))
                        .owner(request.getOwner())
                        .startTime(request.getStartTime())
                        .endTime(request.getEndTime())
                        .incrHours(request.calcIncrHours())
                        .description(request.getDescription())
                        .build())
                .approversChooseRequest(userChoose)
                .build());
        Long flowId = result.getId();
        // card flow rel
        CardIncrWorkload workload = CardIncrWorkload.builder()
                .id(IdUtil.generateId())
                .cardId(card.getId())
                .projectId(card.getProjectId())
                .companyId(card.getCompanyId())
                .createUser(user.getUsername())
                .createTime(new Date())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .owner(request.getOwner())
                .incrHours(request.calcIncrHours())
                .description(request.getDescription())
                .incrResult(result.isProcessing() ? CardIncrWorkload.IncrResult.APPROVAL : CardIncrWorkload.IncrResult.SUCCESS)
                .flowId(flowId)
                .build();
        cardIncrWorkloadDao.saveOrUpdate(workload);
        return workload;
    }

    public CardIncrWorkload updateIncrWorkload(Card card, Long workloadId, IncrWorkloadRequest request) throws IOException {
        CardIncrWorkload workload = cardWorkloadBpmQueryService.cardWorkload(card.getId(), workloadId);
        if (workload == null) {
            throw new CodedException(HttpStatus.NOT_FOUND, "登记工时记录不存在！");
        }
        ProjectWorkloadSetting workloadSetting = projectSchemaQueryService.getProjectWorkloadSetting(card.getProjectId());
        boolean isEnableIncrWorkload = workloadSetting != null && workloadSetting.isEnableIncrWorkload();
        if (!isEnableIncrWorkload) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "未开启工时登记");
        }
        if (systemSettingService.bpmIsOpen()) {
            Map<String, Object> cardDetail = cardDao.findAsMap(card.getId(), CardField.TYPE, CardField.TITLE);
            ProjectWorkloadSetting.Rule rule = workloadSetting.findMatchRule(FieldUtil.getType(cardDetail));
            if (rule != null) {
                throw new CodedException(ErrorCode.REQUIRED_BPM_FLOW, "需要发起审批");
            }
        }
        return updateIncrWorkload(card, workload, request);
    }

    /**
     * 如果不需要审批，直接修改成功；如果需要审批，当前必须处于审批拒绝状态才可修改
     */
    public CardIncrWorkload approvalUpdateIncrWorkload(Card card, Long workloadId, IncrWorkloadRequest request) throws IOException {
        CardIncrWorkload workload = cardWorkloadBpmQueryService.cardWorkload(card.getId(), workloadId);
        if (workload == null) {
            throw new CodedException(HttpStatus.NOT_FOUND, "登记工时记录不存在！");
        }
        if (!systemSettingService.bpmIsOpen()) {
            return updateIncrWorkload(card, workload, request);
        }
        ProjectWorkloadSetting workloadSetting = projectSchemaQueryService.getProjectWorkloadSetting(card.getProjectId());
        boolean isEnableIncrWorkload = workloadSetting != null && workloadSetting.isEnableIncrWorkload();
        if (!isEnableIncrWorkload) {
            return updateIncrWorkload(card, workload, request);
        }
        Map<String, Object> cardDetail = cardDao.findAsMap(card.getId(), CardField.TYPE, CardField.TITLE);
        ProjectWorkloadSetting.Rule rule = workloadSetting.findMatchRule(FieldUtil.getType(cardDetail));
        if (rule == null) {
            return updateIncrWorkload(card, workload, request);
        }
        if (!workload.getIncrResult().equals(CardIncrWorkload.IncrResult.REJECT)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "登记工时记录不在审批拒绝状态！");
        }
        Long bpmFlowTemplateId = rule.getBpmFlowTemplateId();
        BpmUserChoosesRequest userChoosesRequest = request.getBpmUserChoosesRequest();
        CreateBpmFlow.ApproverChooseRequest[] userChoose = null;
        if (userChoosesRequest == null) {
            userChoose = new CreateBpmFlow.ApproverChooseRequest[0];
        } else {
            userChoose = userChoosesRequest.getUserChooses();
        }
        Project project = projectQueryService.select(card.getProjectId());
        String cardTitle = FieldUtil.getTitle(cardDetail);
        LoginUser user = userService.currentUser();
        CreateBpmFlowResult result = ezBpmCliService.createFlow(CreateWorkloadBpmFlow.builder()
                .approvalType(ApprovalType.CARD_INCR_WORKLOAD)
                .callbackData(ApprovalTarget.builder()
                        .approvalType(ApprovalType.CARD_INCR_WORKLOAD)
                        .targetId(card.getId())
                        .build())
                .approvalFlowId(bpmFlowTemplateId)
                .companyId(card.getCompanyId())
                .username(user.getUsername())
                .projectApproval(CreateWorkloadBpmFlow.ProjectApproval.builder()
                        .projectId(project.getId())
                        .projectKey(project.getKey())
                        .projectName(project.getName())
                        .cardId(card.getId())
                        .cardSeqNum(card.getSeqNum())
                        .cardTitle(cardTitle)
                        .cardUrl(endpointHelper.cardDetailUrl(card.getCompanyId(), card.getProjectKey(), card.getSeqNum()))
                        .owner(request.getOwner())
                        .startTime(request.getStartTime())
                        .endTime(request.getEndTime())
                        .incrHours(request.calcIncrHours())
                        .description(request.getDescription())
                        .build())
                .approversChooseRequest(userChoose)
                .build());
        Long flowId = result.getId();
        // card flow rel
        float incrHours = CardIncrWorkload.IncrResult.SUCCESS.equals(workload.getIncrResult()) ?
                request.calcIncrHours() - workload.calcIncrHours() : request.calcIncrHours();
        workload.setOwner(request.getOwner());
        workload.setStartTime(request.getStartTime());
        workload.setEndTime(request.getEndTime());
        workload.setIncrHours(incrHours);
        workload.setDescription(request.getDescription());
        workload.setFlowId(flowId);
        workload.setIncrResult(result.isProcessing() ? CardIncrWorkload.IncrResult.APPROVAL : CardIncrWorkload.IncrResult.SUCCESS);
        cardIncrWorkloadDao.saveOrUpdate(workload);
        return workload;
    }

    public void cancelApprovalIncrWorkload(Long cardId, Long workloadId) throws IOException {
        CardIncrWorkload workload = cardWorkloadBpmQueryService.cardWorkload(cardId, workloadId);
        cancelApprovalRevertWorkload(cardId, workload);
    }

    private void cancelApprovalIncrWorkload(Long cardId, CardIncrWorkload workload) throws IOException {
        if (workload == null) {
            throw new CodedException(HttpStatus.NOT_FOUND, "登记工时记录不存在！");
        }
        if (!workload.getIncrResult().equals(CardIncrWorkload.IncrResult.APPROVAL)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "登记工时记录不在登记审批状态！");
        }
        workload.setIncrResult(CardIncrWorkload.IncrResult.REJECT);
        cardIncrWorkloadDao.saveOrUpdate(workload);
        Long bpmFlowId = workload.getRevertFlowId();
        if (bpmFlowId == null || bpmFlowId <= 0) {
            return;
        }
        SpringBeanFactory.getBean(CardWorkloadBpmCmdService.class).asyncCancelFlow(bpmFlowId, userService.currentUserName());
    }

    public void cancelApprovalRevertWorkload(Long cardId, Long workloadId) throws IOException {
        CardIncrWorkload workload = cardWorkloadBpmQueryService.cardWorkload(cardId, workloadId);
        cancelApprovalRevertWorkload(cardId, workload);
    }

    private void cancelApprovalRevertWorkload(Long cardId, CardIncrWorkload workload) throws IOException {
        if (workload == null) {
            throw new CodedException(HttpStatus.NOT_FOUND, "登记工时记录不存在！");
        }
        if (!workload.getIncrResult().equals(CardIncrWorkload.IncrResult.REVERT)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "登记工时记录不在取消登记状态！");
        }
        workload.setIncrResult(CardIncrWorkload.IncrResult.SUCCESS);
        cardIncrWorkloadDao.saveOrUpdate(workload);
        Long bpmFlowId = workload.getRevertFlowId();
        if (bpmFlowId == null || bpmFlowId <= 0) {
            return;
        }
        SpringBeanFactory.getBean(CardWorkloadBpmCmdService.class).asyncCancelFlow(bpmFlowId, userService.currentUserName());
    }

    public void updateIncrWorkloadBpmFlowResult(Card card, Long flowId, boolean approved, BiConsumer<Map<String, Object>, CardIncrWorkload> incr) throws IOException {
        if (card == null) {
            return;
        }
        if (BooleanUtils.isTrue(card.getDeleted())) {
            return;
        }
        Long cardId = card.getId();
        CardIncrWorkload workload = cardWorkloadBpmQueryService.cardIncrWorkload(cardId, flowId);
        if (workload == null) {
            log.error("cardId[{}] flowId[{}] cannot find CardIncrWorkload!", cardId, flowId);
            return;
        }
        if (CardIncrWorkload.IncrResult.APPROVAL == workload.getIncrResult()) {
            workload.setIncrResult(approved ? CardIncrWorkload.IncrResult.SUCCESS : CardIncrWorkload.IncrResult.REJECT);
            cardIncrWorkloadDao.saveOrUpdate(workload);
            if (approved) {
                Map<String, Object> cardDetail = cardDao.findAsMap(cardId);
                incr.accept(cardDetail, workload);
            }
        }
    }

    public float revertWorkload(Card card, CardIncrWorkload workload) throws IOException {
        return revertWorkload(card, workload, false, null);
    }

    public float approvalRevertWorkload(Card card, CardIncrWorkload workload, BpmUserChoosesRequest bpmUserChoosesRequest) throws IOException {
        return revertWorkload(card, workload, true, bpmUserChoosesRequest);
    }

    public float revertWorkload(Card card, Long workloadId) throws IOException {
        CardIncrWorkload workload = cardWorkloadBpmQueryService.cardWorkload(card.getId(), workloadId);
        return revertWorkload(card, workload, false, null);
    }

    public float approvalRevertWorkload(Card card, Long workloadId, BpmUserChoosesRequest bpmUserChoosesRequest) throws IOException {
        CardIncrWorkload workload = cardWorkloadBpmQueryService.cardWorkload(card.getId(), workloadId);
        return revertWorkload(card, workload, true, bpmUserChoosesRequest);
    }

    private float revertWorkload(Card card, CardIncrWorkload workload, boolean approval, BpmUserChoosesRequest bpmUserChoosesRequest) throws IOException {
        if (workload == null) {
            throw new CodedException(HttpStatus.NOT_FOUND, "登记工时记录不存在！");
        }
        Long flowId = workload.getFlowId();
        ProjectWorkloadSetting.Rule rule = null;
        ProjectWorkloadSetting workloadSetting = projectSchemaQueryService.getProjectWorkloadSetting(card.getProjectId());
        boolean isEnableIncrWorkload = workloadSetting != null && workloadSetting.isEnableIncrWorkload();
        if (!isEnableIncrWorkload) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "未开启登记工时，禁止操作！");
        }
        Map<String, Object> cardDetail = cardDao.findAsMap(card.getId(), CardField.TYPE, CardField.TITLE);
        if (systemSettingService.bpmIsOpen()) {
            rule = workloadSetting.findMatchRevertRule(FieldUtil.getType(cardDetail));
        }
        switch (workload.getIncrResult()) {
            case APPROVAL:
                LoginUser user = userService.currentUser();
                ezBpmCliService.asyncCancelFlow(flowId, user.getUsername());
                if (rule == null) {
                    cardIncrWorkloadDao.delete(workload.getId());
                }
                return 0f;
            case REJECT:
                cardIncrWorkloadDao.delete(workload.getId());
                return 0f;
            case REVERT:
                if (rule == null) {
                    cardIncrWorkloadDao.delete(workload.getId());
                    return workload.calcIncrHours();
                } else {
                    throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "当前正处在登记工时回退的审批流程中！");
                }
            case SUCCESS:
            default:
                if (rule == null) {
                    cardIncrWorkloadDao.delete(workload.getId());
                    return workload.calcIncrHours();
                } else if (approval) {
                    return approvalRevertWorkload(card, FieldUtil.getTitle(cardDetail), workload, rule, bpmUserChoosesRequest);
                } else {
                    throw new CodedException(ErrorCode.REQUIRED_BPM_FLOW, "需要发起审批");
                }
        }
    }

    private float approvalRevertWorkload(
            Card card, String cardTitle, CardIncrWorkload workload,
            ProjectWorkloadSetting.Rule rule, BpmUserChoosesRequest userChoosesRequest) throws IOException {
        CreateBpmFlow.ApproverChooseRequest[] userChoose = null;
        if (userChoosesRequest == null) {
            userChoose = new CreateBpmFlow.ApproverChooseRequest[0];
        } else {
            userChoose = userChoosesRequest.getUserChooses();
        }
        Project project = projectQueryService.select(card.getProjectId());
        LoginUser user = userService.currentUser();
        CreateBpmFlowResult result = ezBpmCliService.createFlow(CreateWorkloadBpmFlow.builder()
                .approvalType(ApprovalType.CARD_REVERT_WORKLOAD)
                .callbackData(ApprovalTarget.builder()
                        .approvalType(ApprovalType.CARD_REVERT_WORKLOAD)
                        .targetId(card.getId())
                        .build())
                .approvalFlowId(rule.getBpmFlowTemplateId())
                .companyId(card.getCompanyId())
                .username(user.getUsername())
                .projectApproval(CreateWorkloadBpmFlow.ProjectApproval.builder()
                        .projectId(project.getId())
                        .projectKey(project.getKey())
                        .projectName(project.getName())
                        .cardId(card.getId())
                        .cardSeqNum(card.getSeqNum())
                        .cardTitle(cardTitle)
                        .cardUrl(endpointHelper.cardDetailUrl(card.getCompanyId(), card.getProjectKey(), card.getSeqNum()))
                        .owner(workload.getOwner())
                        .startTime(workload.getStartTime())
                        .endTime(workload.getEndTime())
                        .incrHours(workload.getIncrHours())
                        .description(workload.getDescription())
                        .build())
                .approversChooseRequest(userChoose)
                .build());
        Long flowId = result.getId();
        if (result.isProcessing()) {
            workload.setRevertFlowId(flowId);
            workload.setIncrResult(CardIncrWorkload.IncrResult.REVERT);
            cardIncrWorkloadDao.saveOrUpdate(workload);
            return 0f;
        } else {
            cardIncrWorkloadDao.delete(workload.getId());
            return workload.calcIncrHours();
        }
    }

    // todo
    public void updateRevertWorkloadBpmFlowResult(Card card, Long flowId, boolean approved, BiConsumer<Map<String, Object>, CardIncrWorkload> incr) throws IOException {
        if (card == null) {
            return;
        }
        if (BooleanUtils.isTrue(card.getDeleted())) {
            return;
        }
        Long cardId = card.getId();
        CardIncrWorkload workload = cardWorkloadBpmQueryService.cardRevertWorkload(cardId, flowId);
        if (workload == null) {
            log.error("cardId[{}] flowId[{}] cannot find CardIncrWorkload!", cardId, flowId);
            return;
        }
        if (CardIncrWorkload.IncrResult.APPROVAL == workload.getIncrResult()) {
            cardIncrWorkloadDao.delete(workload.getId());
        }
    }

    public void deleteByCardIds(List<Long> cardIds) throws IOException {
        cardIncrWorkloadDao.deleteByCardIds(cardIds);
    }

    private CardIncrWorkload createIncrWorkload(Card card, IncrWorkloadRequest request) throws IOException {
        LoginUser user = userService.currentUser();
        CardIncrWorkload workload = CardIncrWorkload.builder()
                .id(IdUtil.generateId())
                .cardId(card.getId())
                .projectId(card.getProjectId())
                .companyId(card.getCompanyId())
                .createUser(user.getUsername())
                .createTime(new Date())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .owner(request.getOwner())
                .incrHours(request.calcIncrHours())
                .description(request.getDescription())
                .incrResult(CardIncrWorkload.IncrResult.SUCCESS)
                .build();
        cardIncrWorkloadDao.saveOrUpdate(workload);
        return workload;
    }

    private CardIncrWorkload updateIncrWorkload(Card card, CardIncrWorkload workload, IncrWorkloadRequest request) throws IOException {
        float incrHours = CardIncrWorkload.IncrResult.SUCCESS.equals(workload.getIncrResult()) ?
                request.calcIncrHours() - workload.calcIncrHours() : request.calcIncrHours();
        workload.setOwner(request.getOwner());
        workload.setStartTime(request.getStartTime());
        workload.setEndTime(request.getEndTime());
        workload.setIncrHours(incrHours);
        workload.setDescription(request.getDescription());
        workload.setIncrResult(CardIncrWorkload.IncrResult.SUCCESS);
        cardIncrWorkloadDao.saveOrUpdate(workload);
        return workload;
    }
}
