package com.ezone.ezproject.modules.portfolio.bean;

import com.ezone.ezproject.modules.project.bean.RoleKeySource;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class DeletePortfolioRoleOptions {
    @ApiModelProperty(value = "是否迁移用户", example = "true")
    private boolean migrate;
    @ApiModelProperty("迁移的目标角色")
    private RoleKeySource migrateToRole;
}
