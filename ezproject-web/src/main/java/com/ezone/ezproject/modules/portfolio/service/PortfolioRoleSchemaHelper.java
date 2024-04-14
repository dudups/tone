package com.ezone.ezproject.modules.portfolio.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.configuration.SysPortfolioRoleSchema;
import com.ezone.ezproject.es.entity.OperationConfig;
import com.ezone.ezproject.es.entity.PortfolioRole;
import com.ezone.ezproject.es.entity.PortfolioRoleSchema;
import com.ezone.ezproject.es.entity.ProjectRoleSchema;
import com.ezone.ezproject.es.entity.Role;
import com.ezone.ezproject.es.entity.RoleSchema;
import com.ezone.ezproject.es.entity.enums.PortfolioOperationType;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.es.entity.enums.RoleType;
import com.ezone.ezproject.modules.permission.RoleSchemaHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
@AllArgsConstructor
public class PortfolioRoleSchemaHelper extends RoleSchemaHelper {

    private PortfolioOperationSchemaHelper operationSchemaHelper;

    @Override
    protected byte[] sysRoleSchemaContent() {
        try {
            return IOUtils.toByteArray(
                    SysPortfolioRoleSchema.class.getResource("/sys-portfolio-role-schema.yaml"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected byte[] sysCustomRoleSchemaContent() {
        try {
            return IOUtils.toByteArray(
                    SysPortfolioRoleSchema.class.getResource("/sys-custom-portfolio-role-schema.yaml"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected PortfolioRoleSchema newSysCustomRoleSchema() {
        try {
            return PortfolioRoleSchema.YAML_MAPPER.readValue(
                    getSysProjectCustomRoleSchemaContent(),
                    PortfolioRoleSchema.class
            );
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected PortfolioRoleSchema newSysRoleSchema() {
        try {
            return ProjectRoleSchema.YAML_MAPPER.readValue(
                    getSysRoleSchemaContent(),
                    PortfolioRoleSchema.class
            );
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected RoleSchema mergeRole(RoleSchema schema, RoleSource source, Role role) {
        PortfolioRole portfolioRole = (PortfolioRole) role;
        if (schema == null || portfolioRole == null) {
            return schema;
        }
        if (StringUtils.isEmpty(portfolioRole.getKey())) {
            if (source != portfolioRole.getSource()) {
                throw new CodedException(HttpStatus.FORBIDDEN, String.format("当前操作无权限添加来源为[%s]的角色", portfolioRole.getSource()));
            }
            portfolioRole.setKey(generateCustomRoleKey(schema, portfolioRole.getSource()));
            ((PortfolioRoleSchema)schema).getRoles().add(portfolioRole);
        } else {
            int index = schema.findRoleIndex(portfolioRole.getSource(), portfolioRole.getKey());
            if (index < 0) {
                throw new CodedException(HttpStatus.NOT_FOUND, "未找到角色");
            } else {
                ((PortfolioRoleSchema)schema).getRoles().set(index, portfolioRole);
            }
        }
        Map<PortfolioOperationType, OperationConfig> operations = new HashMap<>();
        Map<PortfolioOperationType, OperationConfig> roleOps = portfolioRole.getOperations();
        switch (portfolioRole.getType()) {
            case ADMIN:
                break;
            case MEMBER:
                Map<PortfolioOperationType, OperationConfig> memberOps = RoleSource.SYS == portfolioRole.getSource()
                        ? ((PortfolioRole) getSysRoleMap().get("MEMBER")).getOperations()
                        : ((PortfolioRole) getTemplateRoleMap().get(RoleType.MEMBER)).getOperations();
                Set<PortfolioOperationType> memberRoleFixedOps = RoleSource.SYS == portfolioRole.getSource()
                        ? operationSchemaHelper.getSysPortfolioOperationSchema().getSysMemberFixedOps()
                        : operationSchemaHelper.getSysPortfolioOperationSchema().getCustomMemberFixedOps();
                memberRoleFixedOps.forEach(op -> {
                    OperationConfig config = memberOps.get(op);
                    if (config != null) {
                        operations.put(op, memberOps.get(op));
                    }
                });
                if (MapUtils.isNotEmpty(roleOps)) {
                    roleOps.entrySet().stream()
                            .filter(e -> !memberRoleFixedOps.contains(e.getKey()))
                            .forEach(e -> operations.put(e.getKey(), e.getValue()));
                }
                break;
            case GUEST:
            default:
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("不支持的角色类型：[%s]", portfolioRole.getType()));

        }
        portfolioRole.setOperations(operations);
        return schema;
    }
}
