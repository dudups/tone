package com.ezone.ezproject.modules.project.bean;

import com.ezone.ezproject.es.entity.PlanNotice;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProjectConfigRequest<R> {

    @ApiModelProperty(value = "计划通知配置")
    private ProjectNoticeConfig.Config planNoticeConfig;

    @ApiModelProperty(value = "卡片通知配置")
    private ProjectNoticeConfig.Config cardNoticeConfig;

}
