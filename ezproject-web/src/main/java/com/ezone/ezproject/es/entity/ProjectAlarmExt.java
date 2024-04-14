package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.modules.alarm.bean.AlarmItem;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProjectAlarmExt {
    public static final String FIELD_ES_PROJECT_ID = "projectId";
    public static final String FIELD_ES_ACTIVE = "active";
    public static final String FIELD_ES_CREATE_TIME = "createTime";
    public static final String FIELD_ES_TYPE = "type";

    @NotNull
    private Long id;
    @NotNull
    private Long projectId;

    @ApiModelProperty(value = "是否启用")
    @NotNull
    private Boolean active;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "创建人")
    private String createUser;

    @ApiModelProperty(value = "最近修改时间")
    private Date lastModifyTime;

    @ApiModelProperty(value = "最近修改人")
    private String lastModifyUser;

    @ApiModelProperty(hidden = true, value = "预警类型")
    private String type;

    @NotNull
    private AlarmItem alarmItem;
}
