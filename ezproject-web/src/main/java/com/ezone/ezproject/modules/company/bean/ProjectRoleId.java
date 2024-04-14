package com.ezone.ezproject.modules.company.bean;

import com.ezone.ezproject.common.validate.ChineseStringSize;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ProjectRoleId {
    @ApiModelProperty(value = "项目角色标识key", example = "admin")
    private String key;

    @NotNull
    @ApiModelProperty(value = "项目角色来源")
    private RoleSource source;
}
