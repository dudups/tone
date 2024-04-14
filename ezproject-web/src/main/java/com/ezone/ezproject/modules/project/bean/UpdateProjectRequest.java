package com.ezone.ezproject.modules.project.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.annotation.Nullable;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Date;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UpdateProjectRequest {
    @ApiModelProperty(value = "项目名", example = "project1")
    @Size(min = 1, max = 32)
    @NotEmpty
    private String name;

    @ApiModelProperty(value = "项目描述", example = "project1")
    @Size(max = 200)
    private String description;

    @Deprecated
    @ApiModelProperty(value = "企业内私有/公开")
    private boolean isPrivate;

    @Min(0)
    @ApiModelProperty(value = "逻辑删除后保留天数")
    private Long keepDays;

    @ApiModelProperty(value = "开始时间")
    private Date startTime;

    @ApiModelProperty(value = "结束时间")
    private Date endTime;

    @ApiModelProperty(value = "扩展信息")
    private Map<String, Object> extend;

    @Min(0)
    @ApiModelProperty(value = "计划归档后保留天数")
    private Long planKeepDays;

    @Nullable
    @ApiModelProperty(value = "关联项目集ID")
    private List<Long> portfolioIds;

    @ApiModelProperty(value = "严格模式")
    private Boolean isStrict;
}
