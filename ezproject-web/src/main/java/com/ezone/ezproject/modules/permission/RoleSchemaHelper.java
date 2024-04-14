package com.ezone.ezproject.modules.permission;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.Role;
import com.ezone.ezproject.es.entity.RoleSchema;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.es.entity.enums.RoleType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
@NoArgsConstructor
public abstract class RoleSchemaHelper {
    public static final int MAX_CUSTOM_ROLES = 15;

    public static final CodedException MAX_ROLES_EXCEPTION = new CodedException(HttpStatus.NOT_ACCEPTABLE, "创建角色失败，已超自定义角色数量上限：15个!");
    @Getter(lazy = true)
    private final byte[] sysRoleSchemaContent = sysRoleSchemaContent();
    @Getter(lazy = true)
    private final byte[] sysProjectCustomRoleSchemaContent = sysCustomRoleSchemaContent();
    @Getter(lazy = true)
    private final RoleSchema sysRoleSchema = newSysRoleSchema();
    @Getter(lazy = true)
    private final RoleSchema sysCustomRoleSchema = newSysCustomRoleSchema();
    @Getter(lazy = true)
    private final Map<String, Role> sysRoleMap = initSysRoleMap();
    @Getter(lazy = true)
    private final Map<RoleType, Role> templateRoleMap = initTemplateRoleMap();

    protected abstract byte[] sysRoleSchemaContent();

    protected abstract byte[] sysCustomRoleSchemaContent();

    protected abstract RoleSchema newSysCustomRoleSchema();

    protected abstract RoleSchema newSysRoleSchema();

    protected Map<String, Role> initSysRoleMap() {
        return Collections.unmodifiableMap(getSysRoleSchema().toBaseRoles()
                .stream()
                .collect(Collectors.toMap(Role::getKey, r -> r)));
    }

    protected Map<RoleType, Role> initTemplateRoleMap() {
        return Collections.unmodifiableMap(this.getSysCustomRoleSchema().toBaseRoles()
                .stream()
                .collect(Collectors.toMap(Role::getType, r -> r)));
    }

    /**
     * Fill sys roles to project-roles;
     *
     * @param schema
     * @return
     * @throws CodedException
     */
    public RoleSchema fillSysSchema(RoleSchema schema) throws CodedException {
        if (null == schema) {
            return getSysRoleSchema();
        }
        List<Role> roles = new ArrayList<>();
        if (CollectionUtils.isEmpty(schema.toBaseRoles())) {
            roles.addAll(getSysRoleSchema().toBaseRoles());
        } else {
            roles.addAll(schema.toBaseRoles());
            List<String> keys = schema.toBaseRoles().stream().map(Role::getKey).collect(Collectors.toList());
            for (Role role : getSysRoleSchema().toBaseRoles()) {
                if (!keys.contains(role.getKey())) {
                    roles.add(role);
                }
            }
        }
        schema.resetRoles(roles);
        return schema;
    }

    public String generateCustomRoleKey(RoleSchema schema, RoleSource source) throws CodedException {
        if (source == null || source == RoleSource.SYS) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("不支持新建源类型为[%s]的角色！", source));
        }
        int maxCustom = 0;
        if (schema != null) {
            if (schema.toBaseRoles().stream().filter(r -> source == r.getSource()).count() + 1 > MAX_CUSTOM_ROLES) {
                throw MAX_ROLES_EXCEPTION;
            }
            maxCustom = schema.toBaseRoles().stream()
                    .filter(role -> NumberUtils.isDigits(role.getKey()))
                    .mapToInt(role -> NumberUtils.toInt(role.getKey()))
                    .max()
                    .orElse(0);
        }
        return String.valueOf(maxCustom + 1);
    }

    /**
     * 无删除：schema中存在而roles中不存在的，继续在schema中保留
     *
     * @param schema
     * @param source
     * @param roles
     * @return
     */
    public RoleSchema mergeRoles(RoleSchema schema, RoleSource source, List<Role> roles) {
        if (schema == null || CollectionUtils.isEmpty(roles)) {
            return schema;
        }
        for (Role role : roles) {
            schema = mergeRole(schema, source, role);
        }
        return schema;
    }

    public RoleSchema addRole(RoleSchema schema, RoleSource source, Role role) {
        return mergeRole(schema, source, role);
    }

    public RoleSchema removeRole(RoleSchema schema, RoleSource source, String roleKey) {
        if (schema == null || CollectionUtils.isEmpty(schema.toBaseRoles()) || StringUtils.isEmpty(roleKey)) {
            return schema;
        }
        schema.resetRoles(schema.toBaseRoles().stream()
                .filter(role -> !(role.is(source, roleKey)))
                .collect(Collectors.toList()));
        return schema;
    }

    public RoleSchema updateRole(RoleSchema schema, RoleSource source, Role role) {
        return mergeRole(schema, source, role);
    }

    public void checkNameConflict(RoleSchema schema, RoleSource source) {
        if (schema == null) {
            return;
        }
        List<Role> roles = schema.findRoles(RoleSource.SYS, source);
        if (CollectionUtils.isEmpty(roles)) {
            return;
        }
        if (roles.size() > roles.stream().map(Role::getName).distinct().count()) {
            throw new CodedException(HttpStatus.CONFLICT, "角色名冲突！");
        }
    }

    protected abstract RoleSchema mergeRole(RoleSchema schema, RoleSource source, Role role);

//    private RoleSchema mergeRole(RoleSchema schema, RoleSource source, PortfolioRole role) {
//        if (schema == null || role == null) {
//            return schema;
//        }
//        if (StringUtils.isEmpty(role.getKey())) {
//            if (source != role.getSource()) {
//                throw new CodedException(HttpStatus.FORBIDDEN, String.format("当前操作无权限添加来源为[%s]的角色", role.getSource()));
//            }
//            role.setKey(generateCustomRoleKey(schema, role.getSource()));
//            schema.getBaseRoles().add(role);
//        } else {
//            int index = schema.findRoleIndex(role.getSource(), role.getKey());
//            if (index < 0) {
//                throw new CodedException(HttpStatus.NOT_FOUND, "未找到角色");
//            } else {
//                schema.getBaseRoles().set(index, role);
//            }
//        }
//        Map<PortfolioOperationType, OperationConfig> operations = new HashMap<>();
//        Map<PortfolioOperationType, OperationConfig> roleOps = role.getOperations();
//        switch (role.getType()) {
//            case ADMIN:
//                break;
//            case MEMBER:
//                Map<PortfolioOperationType, OperationConfig> memberOps = RoleSource.SYS == role.getSource()
//                        ? getSysRoleMap().get("MEMBER").getOperations()
//                        : getTemplateRoleMap().get(RoleType.MEMBER).getOperations();
//                Set<PortfolioOperationType> memberRoleFixedOps = RoleSource.SYS == role.getSource()
//                        ? operationSchemaHelper.getSysPortfolioOperationSchema().getSysMemberFixedOps()
//                        : operationSchemaHelper.getSysPortfolioOperationSchema().getCustomMemberFixedOps();
//                memberRoleFixedOps.forEach(op -> {
//                    OperationConfig config = memberOps.get(op);
//                    if (config != null) {
//                        operations.put(op, memberOps.get(op));
//                    }
//                });
//                if (MapUtils.isNotEmpty(roleOps)) {
//                    roleOps.entrySet().stream()
//                            .filter(e -> !memberRoleFixedOps.contains(e.getKey()))
//                            .forEach(e -> operations.put(e.getKey(), e.getValue()));
//                }
//                break;
//            case GUEST:
//            default:
//                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("不支持的角色类型：[%s]", role.getType()));
//
//        }
//        role.setOperations(operations);
//        return schema;
//    }

    /**
     * 支持删除：schema中存在而roles中不存在的，从schema中移除
     *
     * @param schema
     * @param roles
     * @return
     */
    public RoleSchema saveRoles(RoleSchema schema, RoleSource source, List<Role> roles) {
        if (schema == null || CollectionUtils.isEmpty(roles)) {
            return schema;
        }
        Iterator<Role> iterator = schema.toBaseRoles().iterator();
        while (iterator.hasNext()) {
            Role schemaRole = iterator.next();
            if (source == schemaRole.getSource()) {
                boolean exist = roles.stream().anyMatch(role -> role.getKey().equals(schemaRole.getKey()));
                if (!exist) {
                    iterator.remove();
                }
            }
        }
        for (Role role : roles) {
            schema = mergeRole(schema, source, role);
        }
        return schema;
    }
}
