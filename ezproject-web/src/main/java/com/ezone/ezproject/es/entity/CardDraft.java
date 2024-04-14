package com.ezone.ezproject.es.entity;

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
public class CardDraft {
    private Date createTime;
    private String user;
    private Long projectId;

    public static final String CREATE_TIME = "createTime";
    public static final String PROJECT_ID = "projectId";
}
