package com.ezone.ezproject.modules.log.bean;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogOperationTypeSelectVo {
    private LogOperationType key;
    private String value;
}
