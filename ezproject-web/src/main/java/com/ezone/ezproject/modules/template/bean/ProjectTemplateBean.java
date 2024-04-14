package com.ezone.ezproject.modules.template.bean;

import com.ezone.ezproject.dal.entity.ProjectTemplate;
import com.ezone.ezproject.es.entity.ProjectTemplateDetail;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProjectTemplateBean {
    @ApiModelProperty(value = "项目模版基本信息")
    @NotNull
    private ProjectTemplate template;

    @ApiModelProperty(value = "项目模版详细定义")
    @NotNull
    private ProjectTemplateDetail detail;
}
