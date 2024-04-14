package com.ezone.ezproject.modules.card.bpm.service;

import com.ezone.ezbase.iam.bean.LoginUser;
import com.ezone.ezbase.iam.service.IAMCenterService;
import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.transactional.AfterCommit;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.dao.CardBpmFlowDao;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.ez.context.SystemSettingService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bean.BpmUserChoosesRequest;
import com.ezone.ezproject.modules.card.bpm.bean.CardBpmFlow;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.service.CardCmdService;
import com.ezone.ezproject.modules.card.service.CardMessageHelper;
import com.ezone.ezproject.modules.cli.EzBpmCliService;
import com.ezone.ezproject.modules.cli.bean.ApprovalTarget;
import com.ezone.ezproject.modules.cli.bean.ApprovalType;
import com.ezone.ezproject.modules.cli.bean.CreateBpmFlow;
import com.ezone.ezproject.modules.cli.bean.CreateBpmFlowResult;
import com.ezone.ezproject.modules.common.EndpointHelper;
import com.ezone.ezproject.modules.project.service.ProjectCardSchemaHelper;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class CardBpmCmdService {
    public static final ObjectMapper JSON_MAPPER = CardDao.JSON_MAPPER;

    private CardDao cardDao;
    private CardBpmFlowDao cardBpmFlowDao;

    private UserService userService;
    private SystemSettingService systemSettingService;

    private ProjectQueryService projectQueryService;
    private ProjectSchemaQueryService projectSchemaQueryService;
    private EndpointHelper endpointHelper;
    private ProjectCardSchemaHelper schemaHelper;

    private EzBpmCliService ezBpmCliService;

    private CardMessageHelper cardMessageHelper;

    private IAMCenterService iamCenterService;

    public void cancelApprovalStatus(Card card) throws IOException {
        Map<String, Object> cardDetail = cardDao.findAsMap(card.getId());
        Long bpmFlowId = FieldUtil.getBpmFlowId(cardDetail);
        if (bpmFlowId == null || bpmFlowId <= 0) {
            return;
        }
        Map<String, Object> props = new HashMap<>();
        props.put(CardField.BPM_FLOW_ID, null);
        props.put(CardField.BPM_FLOW_TO_STATUS, null);
        cardDao.updateSelective(card.getId(), props);
        SpringBeanFactory.getBean(CardBpmCmdService.class).asyncCancelFlow(bpmFlowId, userService.currentUserName());
    }

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

    public CardBpmFlow approvalSetStatus(Card card, String status, BpmUserChoosesRequest userChoosesRequest) throws IOException {
        CreateBpmFlow.ApproverChooseRequest[] userChoose = null;
        if (userChoosesRequest == null) {
            userChoose = new CreateBpmFlow.ApproverChooseRequest[0];
        } else {
            userChoose = userChoosesRequest.getUserChooses();
        }
        Map<String, Object> cardDetail = cardDao.findAsMap(card.getId());
        String oldStatus = FieldUtil.toString(cardDetail.get(CardField.STATUS));
        if (oldStatus.equals(status)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "已经是目标状态，无须审批！");
        }
        Project project = projectQueryService.select(card.getProjectId());
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(card.getProjectId());
        LoginUser user = userService.currentUser();
        // check status&flow&permission
        schemaHelper.checkChangeCardStatus(schema, cardDetail, user.getUsername(), status, true, systemSettingService.bpmIsOpen());
        String type = FieldUtil.getType(cardDetail);
        CardType cardType = schema.findCardType(type);
        String oldStatusName = schema.findCardStatusName(oldStatus);
        String statusName = schema.findCardStatusName(status);
        Long bpmFlowTemplateId = cardType.findStatusFlow(oldStatus, status).getBpmFlowTemplateId();
        String cardTitle = FieldUtil.getTitle(cardDetail);
        CreateBpmFlowResult result = ezBpmCliService.createFlow(CreateBpmFlow.builder()
                .approvalType(ApprovalType.CARD_STATUS)
                .callbackData(ApprovalTarget.builder()
                        .approvalType(ApprovalType.CARD_STATUS)
                        .targetId(card.getId())
                        .build())
                .approvalFlowId(bpmFlowTemplateId)
                .companyId(card.getCompanyId())
                .username(user.getUsername())
                .projectApproval(CreateBpmFlow.ProjectApproval.builder()
                        .projectId(project.getId())
                        .projectKey(project.getKey())
                        .projectName(project.getName())
                        .cardId(card.getId())
                        .cardSeqNum(card.getSeqNum())
                        .cardTitle(cardTitle)
                        .fromStatus(oldStatus)
                        .fromStatusName(oldStatusName)
                        .toStatus(status)
                        .toStatusName(statusName)
                        .content(cardMessageHelper.statusBpmFlowContent(userService.currentUser(), project, card, cardTitle, oldStatusName, statusName))
                        .cardUrl(endpointHelper.cardDetailUrl(card.getCompanyId(), card.getProjectKey(), card.getSeqNum()))
                        .build())
                .approversChooseRequest(userChoose)
                .build());
        Long flowId = result.getId();
        // card flow rel
        CardBpmFlow flow = CardBpmFlow.builder()
                .user(user.getUsername())
                .date(new Date())
                .cardId(card.getId())
                .flowId(flowId)
                .flowDetail(CardBpmFlow.FlowDetail.builder()
                        .fromStatus(oldStatus)
                        .fromStatusName(oldStatusName)
                        .toStatus(status)
                        .toStatusName(statusName)
                        .build())
                .build();
        cardBpmFlowDao.saveOrUpdate(IdUtil.generateId(), flow);
        if (result.isProcessing()) {
            // update es (!without card-event!)
            Map<String, Object> props = new HashMap<>();
            props.put(CardField.BPM_FLOW_ID, flowId);
            props.put(CardField.BPM_FLOW_TO_STATUS, status);
            cardDao.updateSelective(card.getId(), props);
        } else if (result.isFinished()) {
            SpringBeanFactory.getBean(CardCmdService.class).forceSetStatus(schema, card, cardDetail, status);
        }
        return flow;
    }

    public void deleteByCardIds(List<Long> cardIds) throws IOException {
        cardBpmFlowDao.deleteByCardIds(cardIds);
    }

    /**
     * 批量审核
     * todo 待联调 因变更、暂时不使用。
     * @param schema
     * @param project
     * @param user
     * @param bpmReqs
     * @return
     */
    public @NotNull Map<Long, CreateBpmFlowResult> batchCreateCardsBpm(ProjectCardSchema schema, Project project, LoginUser user, List<BatchCreateCardBpmFlowHelp.CardBpmReq> bpmReqs) {
        return BatchCreateCardBpmFlowHelp.builder()
                .cardMessageHelper(cardMessageHelper)
                .ezBpmCliService(ezBpmCliService)
                .reqs(bpmReqs)
                .endpointHelper(endpointHelper)
                .project(project)
                .user(user)
                .schema(schema)
                .build()
                .doSubmit();
    }
}
