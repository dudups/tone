package com.ezone.ezproject.external.ci.bean;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class RepoEventMessage {
    private Long companyId;
    private Long repoId;
    private String repoName;
    private OperationType operationType;
    private String operator;

    public enum OperationType {
        REPO_DELETE
    }
}
