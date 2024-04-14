package com.ezone.ezproject.modules.portfolio.service;

import com.ezone.ezproject.es.entity.PortfolioRole;
import com.ezone.ezproject.es.entity.PortfolioRoleSchema;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
@Slf4j
public class PortfolioRoleSchemaSettingHelper {
    private PortfolioRoleSchemaHelper schemaHelper;

    public PortfolioRoleSchema defaultRoleSchema() {
        return (PortfolioRoleSchema) schemaHelper.getSysRoleSchema();
    }

    public PortfolioRoleSchema addRole(PortfolioRoleSchema schema, RoleSource source, PortfolioRole role) {
        schemaHelper.addRole(schema, source, role);
        schemaHelper.checkNameConflict(schema, source);
        return schema;
    }

    public PortfolioRoleSchema updateRole(PortfolioRoleSchema schema, RoleSource source, PortfolioRole role) {
        schemaHelper.updateRole(schema, source, role);
        schemaHelper.checkNameConflict(schema, role.getSource());
        return schema;
    }

    public PortfolioRoleSchema deleteRole(PortfolioRoleSchema schema, RoleSource source, String roleKey) {
        schemaHelper.removeRole(schema, source, roleKey);
        return schema;
    }

}
