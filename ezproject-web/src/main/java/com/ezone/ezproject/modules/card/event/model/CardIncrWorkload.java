package com.ezone.ezproject.modules.card.event.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardIncrWorkload {
    private Long id;
    private Long cardId;
    private Long projectId;
    private Long companyId;
    private Date createTime;
    private String createUser;
    private String description;

    private String owner;
    private Date startTime;
    private Date endTime;
    /**
     * 新建或编辑（deltaIncrHours）
     */
    private float incrHours;
    private IncrResult incrResult;

    private Long flowId;
    private Long revertFlowId;

    public static final String CARD_ID = "cardId";
    public static final String PROJECT_ID = "projectId";
    public static final String COMPANY_ID = "companyId";
    public static final String CREATE_TIME = "createTime";
    public static final String OWNER = "owner";
    public static final String START_TIME = "startTime";
    public static final String INCR_RESULT = "incrResult";
    public static final String FLOW_ID = "flowId";
    public static final String REVERT_FLOW_ID = "flowId";

    public enum IncrResult {
        APPROVAL, REJECT, SUCCESS, REVERT
    }

    public float calcIncrHours() {
        return (endTime.getTime() - startTime.getTime()) / 3600000F;
    }
}
