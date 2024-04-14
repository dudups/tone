package com.ezone.ezproject.modules.card.bpm.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardBpmFlow {
    private Date date;
    private String user;
    private Long cardId;
    private Long flowId;
    private FlowDetail flowDetail;

    public static final String CARD_ID = "cardId";
    public static final String FLOW_ID = "flowId";

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlowDetail {
        private String fromStatus;
        private String toStatus;
        private String fromStatusName;
        private String toStatusName;
    }
}
