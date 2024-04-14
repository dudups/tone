package com.ezone.ezproject.external.ci.bean;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class CodeCardEventMessage {
    private Long companyId;
    private Long repoId;
    private String repoName;
    private List<String> cardKeys;
    private EventType event;
    private String username;
    private String branchName;

    public enum EventType {
        PUSH, REVIEW_ADD, REVIEW_UPDATE, REVIEW_PUSH, REVIEW_APPROVAL, REVIEW_REJECTION, REVIEW_MERGE, TAG_ADD
    }
}
