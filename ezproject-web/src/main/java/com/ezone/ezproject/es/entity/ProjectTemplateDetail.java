package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.modules.alarm.bean.AlarmItem;
import com.ezone.ezproject.modules.alarm.bean.ProjectAlarmItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProjectTemplateDetail {
    public static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory()
            .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @ApiModelProperty(value = "项目下卡片Schema定义")
    @NotNull
    private ProjectCardSchema projectCardSchema;

    @ApiModelProperty(value = "项目下卡片角色定义")
    @NotNull
    private ProjectRoleSchema projectRoleSchema;

    @ApiModelProperty(value = "项目下卡片模版")
    private Map<String, Map<String, Object>> projectCardTemplates;

    @ApiModelProperty(value = "项目下概览设置模版")
    private ProjectSummaryTemplate projectSummaryConfigTemplates;

    @ApiModelProperty(value = "项目的菜单")
    private ProjectMenuConfig projectMenu;

    @ApiModelProperty(value = "项目下通知设置")
    private ProjectNoticeConfig projectNoticeConfig;

    @Singular
    @ApiModelProperty(value = "项目下预警设置")
    private List<ProjectTemplateAlarm> alarms = Collections.emptyList();

    public static ProjectTemplateDetail from(String yaml) throws JsonProcessingException {
        return YAML_MAPPER.readValue(yaml, ProjectTemplateDetail.class);
    }

    public String yaml() throws JsonProcessingException {
        return YAML_MAPPER.writeValueAsString(this);
   }

}
