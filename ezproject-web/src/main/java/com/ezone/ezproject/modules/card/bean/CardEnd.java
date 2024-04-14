package com.ezone.ezproject.modules.card.bean;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * EzData card-end mq msg
 */
@Data
@Accessors(chain = true)
public class CardEnd {
    private long id;
    private long companyId;
    private long time;
    private String user;

    private long cardId;
    private long seqNum;
    private long projectId;
    private String projectKey;
}
