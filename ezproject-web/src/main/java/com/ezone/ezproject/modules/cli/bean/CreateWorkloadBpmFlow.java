package com.ezone.ezproject.modules.cli.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkloadBpmFlow {
    private ApprovalType approvalType = ApprovalType.CARD_STATUS;
    private ApprovalTarget callbackData;
    private Long approvalFlowId;
    private Long companyId;
    private ProjectApproval projectApproval;
    private String username;
    private CreateBpmFlow.ApproverChooseRequest[] approversChooseRequest;

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectApproval {
        private Long projectId;
        private String projectKey;
        private String projectName;
        private Long cardId;
        private Long cardSeqNum;
        private String cardTitle;
        private String cardUrl;
        private String owner;
        private Date startTime;
        private Date endTime;
        private float incrHours;
        private String description;
    }
}
