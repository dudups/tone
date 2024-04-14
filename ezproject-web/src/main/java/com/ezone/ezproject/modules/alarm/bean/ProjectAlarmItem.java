package com.ezone.ezproject.modules.alarm.bean;

import com.ezone.ezproject.common.exception.CodedException;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class ProjectAlarmItem extends AlarmItem {
    public static final String FIELD_KEY_START_TIME = "startTime";
    public static final String FIELD_KEY_END_TIME = "endTime";

    public static String filedName(String fieldKey) {
        if (FIELD_KEY_START_TIME.equals(fieldKey)) {
            return "项目开始时间";
        } else if (FIELD_KEY_END_TIME.equals(fieldKey)) {
            return "项目结束时间";
        } else {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "暂不支持的预警字段");
        }
    }
}
