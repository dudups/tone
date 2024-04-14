package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.es.entity.enums.WebHookEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class WebHookProject {
    private Long webHookId;
    private Long projectId;
    private List<WebHookEventType> eventTypes;

    public static final String WEB_HOOK_ID = "webHookId";
    public static final String PROJECT_ID = "projectId";
    public static final String EVENT_TYPES = "eventTypes";
}
