package com.ezone.ezproject.modules.project.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectMenuConfigReq {
    @ApiModelProperty(value = "菜单项")
    private List<ProjectMenu> menus;
    @ApiModelProperty(value = "默认打开菜单项")
    private ProjectMenu defaultMenu;
}
