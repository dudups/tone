package com.ezone.ezproject.modules.card.bpm.service;

import com.ezone.ezbase.iam.bean.LoginUser;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.service.CardMessageHelper;
import com.ezone.ezproject.modules.cli.EzBpmCliService;
import com.ezone.ezproject.modules.cli.bean.ApprovalTarget;
import com.ezone.ezproject.modules.cli.bean.ApprovalType;
import com.ezone.ezproject.modules.cli.bean.CreateBpmFlow;
import com.ezone.ezproject.modules.cli.bean.CreateBpmFlowResult;
import com.ezone.ezproject.modules.common.EndpointHelper;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Builder
public class BatchCreateCardBpmFlowHelp {
    private List<CardBpmReq> reqs;
    private Project project;
    private LoginUser user;
    private CardMessageHelper cardMessageHelper;
    private EndpointHelper endpointHelper;
    private ProjectCardSchema schema;
    private EzBpmCliService ezBpmCliService;

    public @NotNull Map<Long, CreateBpmFlowResult> doSubmit() {
        HashMap<Long, CreateBpmFlowResult> resultMap = new HashMap<>();
        if (CollectionUtils.isEmpty(reqs)) {
            return resultMap;
        }
        List<CreateBpmFlow> flowReqs = new ArrayList<>();
        reqs.forEach(cardBpmReq -> {
            Map<String, Object> cardDetail = cardBpmReq.getCardDetail();
            String status = FieldUtil.getStatus(cardDetail);
            String oldStatusName = schema.findCardStatusName(status);
            String statusName = schema.findCardStatusName(cardBpmReq.getToStatus());
            CreateBpmFlow.ProjectApproval projectApproval = CreateBpmFlow.ProjectApproval.builder()
                    .projectId(project.getId())
                    .projectKey(project.getKey())
                    .projectName(project.getName())
                    .cardId(cardBpmReq.cardId)
                    .cardSeqNum(FieldUtil.getSeqNum(cardDetail))
                    .cardTitle(FieldUtil.getTitle(cardDetail))
                    .fromStatus(status)
                    .fromStatusName(schema.findCardStatusName(status))
                    .toStatus(status)
                    .toStatusName(statusName)
                    .content(cardMessageHelper.changeCardTypeAndStatusBpmFlowContent(user, project, cardDetail, oldStatusName, statusName))
                    .cardUrl(endpointHelper.cardDetailUrl(FieldUtil.getCompanyId(cardDetail), project.getKey(), FieldUtil.getSeqNum(cardDetail)))
                    .build();
            flowReqs.add(
                    CreateBpmFlow.builder()
                            .approvalType(ApprovalType.CARD_STATUS)
                            .callbackData(ApprovalTarget.builder()
                                    .approvalType(ApprovalType.CARD_STATUS)
                                    .targetId(cardBpmReq.cardId)
                                    .build())
                            .approvalFlowId(cardBpmReq.getFlowId())
                            .companyId(project.getCompanyId())
                            .username(user.getUsername())
                            .projectApproval(projectApproval).build());
        });
        return ezBpmCliService.batchCreateFlow(flowReqs);
    }

    @Data
    @Builder
    public static class CardBpmReq {
        String toStatus;
        Long cardId;
        Long flowId;
        Map<String, Object> cardDetail;
    }
}
