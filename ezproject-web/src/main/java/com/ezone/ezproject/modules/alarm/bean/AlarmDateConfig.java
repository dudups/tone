package com.ezone.ezproject.modules.alarm.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AlarmDateConfig {

    @ApiModelProperty(value = "日期字段，天、日、小时", example = "HOUR")
    @NotNull
    DateField dateField;

    @ApiModelProperty(value = "指定日期字段的值，正值表示之后，负值表示之前")
    @NotNull
    Integer number;

    public enum DateField {
        DAY, HOUR, MINUTE;

        public String getName() {
            switch (this) {
                case DAY:
                    return "天";
                case HOUR:
                    return "小时";
                case MINUTE:
                default:
                    return "分钟";
            }
        }
    }

    public long numberOfMillisecond() {
        long crossTime;
        switch (dateField) {
            case DAY:
                crossTime = number * 24 * 60 * 60 * 1000L;
                break;
            case HOUR:
                crossTime = number * 60 * 60 * 1000L;
                break;
            case MINUTE:
            default:
                crossTime = number * 60 * 1000L;
                break;
        }
        return crossTime;
    }

}
