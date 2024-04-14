package com.ezone.ezproject.es.entity.bean;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "code", value = CodeEventFilterConf.class)
})
public interface AutoStatusFlowEventFilterConf<T> {
    default boolean match(T eventData) {
        return true;
    }
}
