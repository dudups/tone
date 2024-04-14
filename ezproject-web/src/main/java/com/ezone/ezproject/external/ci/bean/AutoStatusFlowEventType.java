package com.ezone.ezproject.external.ci.bean;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@Getter
public enum AutoStatusFlowEventType {
    CODE_PUSH("代码提交"),
    CODE_REVIEW_ADD("发起及更新评审"),
    // CODE_REVIEW_UPDATE("评审更新"),
    CODE_REVIEW_APPROVAL("评审通过"),
    CODE_REVIEW_REJECTION("评审驳回"),
    CODE_REVIEW_MERGE("评审合并"),
    CODE_TAG_ADD("发布版本"),;

    private String description;

    public static final Set<AutoStatusFlowEventType> CODE_BRANCH_FILTER_EVENTS = new HashSet<>(Arrays.asList(
            CODE_PUSH, CODE_REVIEW_ADD, CODE_REVIEW_APPROVAL, CODE_REVIEW_REJECTION, CODE_REVIEW_MERGE
    ));

}
