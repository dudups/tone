package com.ezone.ezproject.es.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum WebHookEventType {
    CREATE_CARD("创建卡片"), DEL_CARD("删除卡片"), UPDATE_CARD("更新卡片"), RECOVERY("还原卡片");

    private String description;
}
