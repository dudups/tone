package com.ezone.ezproject.modules.alarm.bean;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "specUsers", value = NoticeSpecUsersConfig.class),
        @JsonSubTypes.Type(name = "specFields", value = NoticeFieldUsersConfig.class),
        @JsonSubTypes.Type(name = "specRoles", value = NoticeRoleUserConfig.class)
})
public interface NoticeUserConfig {
}
