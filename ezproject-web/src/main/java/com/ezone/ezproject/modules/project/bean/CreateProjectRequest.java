package com.ezone.ezproject.modules.project.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.annotation.Nullable;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {
    @ApiModelProperty(value = "项目标识", example = "project1")
    @Size(min = 1, max = 32)
    private String key;

    @ApiModelProperty(value = "项目名", example = "project1")
    @Size(min = 1, max = 32)
    private String name;

    @ApiModelProperty(value = "项目描述", example = "project1")
    @Size(max = 200)
    private String description;

    @ApiModelProperty(value = "企业内私有/公开")
    private Boolean isPrivate;

    @ApiModelProperty(value = "模版类型：项目模版/已存在项目")
    private TemplateType templateType = TemplateType.TEMPLATE;

    @ApiModelProperty(value = "项目模版ID")
    private Long projectTemplateId;

    @ApiModelProperty(value = "模版项目ID")
    private Long templateProjectId;
    @ApiModelProperty(value = "当templateType为PROJECT时，是否复制自定义报表，默认false")
    private Boolean copyCharts = Boolean.FALSE;

    @Deprecated
    @Min(0)
    @ApiModelProperty(value = "卡片逻辑删除后保留天数")
    private Long keepDays;

    @Nullable
    @ApiModelProperty(value = "关联项目集ID")
    private List<Long> portfolioIds;

    @ApiModelProperty(value = "开始时间")
    private Date startTime;

    @ApiModelProperty(value = "结束时间")
    private Date endTime;

    @ApiModelProperty(value = "扩展信息")
    private Map<String, Object> extend;

    @ApiModelProperty(value = "严格模式")
    private Boolean isStrict;

    public enum TemplateType {
        TEMPLATE, PROJECT
    }
}
