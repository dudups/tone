package com.ezone.ezproject.modules.template.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UpdateProjectTemplateRequest {
    @ApiModelProperty(value = "模版名称", example = "template1")
    @NotNull
    @Size(min = 1, max = 32)
    private String name;

    @ApiModelProperty(value = "启用")
    private boolean enable;
}
