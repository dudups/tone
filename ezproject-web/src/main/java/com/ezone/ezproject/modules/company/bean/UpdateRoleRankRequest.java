package com.ezone.ezproject.modules.company.bean;

import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.modules.company.rank.RankLocation;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class UpdateRoleRankRequest {

//    @ApiModelProperty(value = "企业角色唯一标识")
//    private ProjectRoleId projectRoleId;
//
//    @NotNull
//    @ApiModelProperty(value = "参照角色ID")
//    private ProjectRoleId referenceRoleId;

    @ApiModelProperty(value = "项目角色标识key", example = "admin")
    private String roleKey;

    @ApiModelProperty(value = "项目角色标识key", example = "admin")
    private String referenceRoleKey;

    @NotNull
    @ApiModelProperty(value = "项目角色来源")
    private RoleSource source;

    @NotNull
    @ApiModelProperty(value = "相对参照角色排序位置更前/更后")
    private RankLocation location;
}
