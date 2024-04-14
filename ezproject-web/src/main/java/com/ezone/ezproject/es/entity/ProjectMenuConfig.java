package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.modules.project.bean.ProjectMenu;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProjectMenuConfig {
    @ApiModelProperty(value = "最近修改时间")
    private Date lastModifyTime;

    @ApiModelProperty(value = "最近修改人")
    private String lastModifyUser;

    @ApiModelProperty(value = "菜单项")
    private List<ProjectMenu> menus;

    @ApiModelProperty(value = "默认打开菜单项")
    private ProjectMenu defaultMenu;
}
