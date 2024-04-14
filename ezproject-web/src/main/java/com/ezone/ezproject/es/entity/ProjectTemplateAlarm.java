package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.modules.alarm.bean.AlarmItem;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProjectTemplateAlarm {
    @ApiModelProperty(value = "是否启用")
    @NotNull
    private Boolean active;

    @NotNull
    private AlarmItem alarmItem;
}
