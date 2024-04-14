package com.ezone.ezproject.modules.alarm.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 报警配置项
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "cardAlarm", value = CardAlarmItem.class),
        @JsonSubTypes.Type(name = "planAlarm", value = PlanAlarmItem.class),
        @JsonSubTypes.Type(name = "projectAlarm", value = ProjectAlarmItem.class)
})
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public abstract class AlarmItem {

    @ApiModelProperty(value = "告警设置的名称")
    @NotNull
    String name;

    @ApiModelProperty(value = "告警设置针对的资源时间字段")
    @NotNull
    String dateFieldKey;

    @ApiModelProperty(value = "告警的时间配置")
    @NotNull
    AlarmDateConfig alarmDateConfig;

    /**
     * 通知用户，两种类型specUsers（指定用户）与specFields（指定相关资源用户字段）
     */
    @NotNull
    List<NoticeUserConfig> warningUsers;

    public enum Type {
        cardAlarm, planAlarm, projectAlarm
    }

    public String alarmDateRuleFormat() {
        Integer number = getAlarmDateConfig().getNumber();
        return (number > 0 ? "推后" : "提前") + (Math.abs(number)) + getAlarmDateConfig().getDateField().getName();
    }
}
