package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.configuration.SysProjectRoleSchema;
import com.ezone.ezproject.es.entity.MergedProjectRoleSchema;
import com.ezone.ezproject.es.entity.OperationConfig;
import com.ezone.ezproject.es.entity.ProjectRole;
import com.ezone.ezproject.es.entity.ProjectRoleSchema;
import com.ezone.ezproject.es.entity.Role;
import com.ezone.ezproject.es.entity.RoleSchema;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.es.entity.enums.RoleType;
import com.ezone.ezproject.modules.permission.RoleSchemaHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
@Component
@Slf4j
public class ProjectRoleSchemaHelper extends RoleSchemaHelper {

    private ProjectOperationSchemaHelper operationSchemaHelper;

    protected byte[] sysRoleSchemaContent() {
        try {
            return IOUtils.toByteArray(
                    SysProjectRoleSchema.class.getResource("/sys-role-schema.yaml"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected byte[] sysCustomRoleSchemaContent() {
        try {
            return IOUtils.toByteArray(
                    SysProjectRoleSchema.class.getResource("/sys-custom-role-schema.yaml"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected ProjectRoleSchema newSysCustomRoleSchema() {
        try {
            return ProjectRoleSchema.YAML_MAPPER.readValue(
                    getSysProjectCustomRoleSchemaContent(),
                    ProjectRoleSchema.class
            );
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected ProjectRoleSchema newSysRoleSchema() {
        try {
            return ProjectRoleSchema.YAML_MAPPER.readValue(
                    getSysRoleSchemaContent(),
                    ProjectRoleSchema.class
            );
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public ProjectRoleSchema mergeDefaultSchema(ProjectRoleSchema schema, RoleSource source) throws CodedException {
        return mergeDefaultSchema(schema, source, null);
    }

    public MergedProjectRoleSchema mergeDefaultSchema(ProjectRoleSchema schema, RoleSource source, ProjectRoleSchema defaultSchema) throws CodedException {
        if (null == schema) {
            return MergedProjectRoleSchema.from(defaultSchema == null ? (ProjectRoleSchema) getSysRoleSchema() : defaultSchema);
        }
        if (defaultSchema == null || CollectionUtils.isEmpty(defaultSchema.toBaseRoles())) {
            defaultSchema = (ProjectRoleSchema) getSysRoleSchema();
        }
        List<ProjectRole> roles = new ArrayList<>();
        List<ProjectRole> refCompanyRoles = new ArrayList<>();
        if (CollectionUtils.isEmpty(schema.toBaseRoles())) {
            roles = defaultSchema.getRoles();
        } else {
            refCompanyRoles = new ArrayList<>();
            for (Role defaultRole : defaultSchema.toBaseRoles()) {
                ProjectRole role = (ProjectRole) schema.findRole(defaultRole.getSource(), defaultRole.getKey());
                if (role != null) {
                    role.mergeDefault(defaultRole);
                    roles.add(role);
                } else {
                    roles.add((ProjectRole) defaultRole);
                    refCompanyRoles.add((ProjectRole) defaultRole);
                }
            }
            for (Role role : schema.toBaseRoles()) {
                if (role.getSource() == source && defaultSchema.findRole(role.getSource(), role.getKey()) == null) {
                    roles.add((ProjectRole) role);
                }
            }
        }
        return MergedProjectRoleSchema.builder()
                .roles(roles)
                .refCompanyRoles(refCompanyRoles)
                .maxRank(schema.getMaxRank())
                .build();
    }

    protected RoleSchema mergeRole(RoleSchema schema, RoleSource source, Role role) {
        if (schema == null || role == null || !(role instanceof ProjectRole)) {
            return schema;
        }
        if (StringUtils.isEmpty(role.getKey())) {
            if (source != role.getSource()) {
                throw new CodedException(HttpStatus.FORBIDDEN, String.format("当前操作无权限添加来源为[%s]的角色", role.getSource()));
            }
            role.setKey(generateCustomRoleKey(schema, role.getSource()));
            ((ProjectRoleSchema)schema).getRoles().add((ProjectRole)role);
        } else {
            int index = schema.findRoleIndex(role.getSource(), role.getKey());
            if (index < 0) {
                throw new CodedException(HttpStatus.NOT_FOUND, "未找到角色");
            } else {
                ((ProjectRoleSchema)schema).getRoles().set(index, (ProjectRole)role);
            }
        }
        Map<OperationType, OperationConfig> operations = new HashMap<>();
        ProjectRole projectRole = (ProjectRole) role;
        Map<OperationType, OperationConfig> roleOps = projectRole.getOperations();
        switch (role.getType()) {
            case ADMIN:
                break;
            case MEMBER:
                Map<OperationType, OperationConfig> memberOps = RoleSource.SYS == projectRole.getSource()
                        ? ((ProjectRole) (getSysRoleMap().get("MEMBER"))).getOperations()
                        : ((ProjectRole) getTemplateRoleMap().get(RoleType.MEMBER)).getOperations();
                Set<OperationType> memberRoleFixedOps = RoleSource.SYS == role.getSource()
                        ? operationSchemaHelper.getSysProjectOperationSchema().getSysMemberFixedOps()
                        : operationSchemaHelper.getSysProjectOperationSchema().getCustomMemberFixedOps();
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
                Map<OperationType, OperationConfig> guestOps = RoleSource.SYS == role.getSource()
                        ? ((ProjectRole) (getSysRoleMap().get("GUEST"))).getOperations()
                        : ((ProjectRole) (getTemplateRoleMap().get(RoleType.GUEST))).getOperations();
                operations.putAll(guestOps);
                operationSchemaHelper.getSysProjectOperationSchema().getGuestCustomOps().forEach(op -> {
                    if (MapUtils.isEmpty(roleOps) || !roleOps.containsKey(op)) {
                        operations.remove(op);
                    } else {
                        operations.put(op, roleOps.get(op));
                    }
                });
                break;
            default:
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("不支持的角色类型：[%s]", role.getType()));

        }
        ((ProjectRole) (role)).setOperations(operations);
        return schema;
    }
}
