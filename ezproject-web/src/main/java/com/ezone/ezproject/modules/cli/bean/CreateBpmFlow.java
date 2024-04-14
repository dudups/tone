package com.ezone.ezproject.modules.cli.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBpmFlow {
    private ApprovalType approvalType = ApprovalType.CARD_STATUS;
    private ApprovalTarget callbackData;
    private Long approvalFlowId;
    private Long companyId;
    private ProjectApproval projectApproval;
    private String username;
    private ApproverChooseRequest[] approversChooseRequest;

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
        private String fromStatus;
        private String toStatus;
        private String fromStatusName;
        private String toStatusName;
        // 原设计为生成页面展示信息，后改为结构化渲染展示，不再需要
        @Deprecated
        private String content;
        private String cardUrl;
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApproverChooseRequest {
        @ApiModelProperty(value = "审批流定义中的阶段ID")
        private Long approvalFlowStageId;
        @ApiModelProperty(value = "阶段中指的用户审批（用户组或角色）")
        private ApproversChoose[] approversChoose;
    }

    @Data
    public static class ApproversChoose {
        @ApiModelProperty(value = "审批流程配置中的用户组或角色名")
        private String approver;

        @ApiModelProperty(value = "所选用户组或角色中的具体用户")
        private List<String> usernames;
    }
}
