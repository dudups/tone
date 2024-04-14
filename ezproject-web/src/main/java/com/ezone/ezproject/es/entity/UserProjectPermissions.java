package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.es.entity.enums.RoleType;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.project.service.ProjectOperationSchemaHelper;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserProjectPermissions {
    private String user = StringUtils.EMPTY;

    private boolean isAdmin;

    private boolean isMember;

    @ApiModelProperty(value = "操作权限设置")
    @Builder.Default
    private Map<OperationType, OperationConfig> operations = new HashMap<>();

    @ApiModelProperty(value = "操作权限设置，只包括：guest类型角色的、依赖于卡片字段判断权限的操作设置")
    @Builder.Default
    private Map<OperationType, OperationConfig> guestLimitOperations = new HashMap<>();

    public static final CodedException UNSUPPORTED_OPERATION_TYPE = new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, "传入了不支持的操作类型！");

    public static UserProjectPermissions from(String user, List<ProjectRole> roles) {
        UserProjectPermissions permissions = new UserProjectPermissions();
        permissions.user = user;
        permissions.mergeRoles(roles);
        return permissions;
    }

    public static UserProjectPermissions fromAdmin(String user) {
        UserProjectPermissions permissions = new UserProjectPermissions();
        permissions.user = user;
        permissions.isAdmin = true;
        return permissions;
    }

    public void mergeRoles(List<ProjectRole> roles) {
        if (isAdmin || CollectionUtils.isEmpty(roles)) {
            return;
        }
        for (ProjectRole role : roles) {
            if (RoleType.ADMIN.equals(role.getType())) {
                isAdmin = true;
                operations.clear();
                guestLimitOperations.clear();
                return;
            } else if (!isMember && RoleType.MEMBER.equals(role.getType())) {
                isMember = true;
            }
            mergeRole(role);
        }
    }

    /**
     * @param op 不支持部分卡片操作：依赖于卡片类型/其它字段判断权限的操作
     * @return
     */
    public void checkPermission(OperationType op) {
        if (!hasPermission(Arrays.asList(op))) {
            throw CodedException.FORBIDDEN;
        }
    }

    /**
     * @param op 不支持部分卡片操作：依赖于卡片类型/其它字段判断权限的操作
     * @return
     */
    public boolean hasPermission(OperationType op) {
        return hasPermission(Arrays.asList(op));
    }

    /**
     * @param ops 不支持部分卡片操作：依赖于卡片类型/其它字段判断权限的操作
     * @return
     */
    public boolean hasPermission(List<OperationType> ops) {
        if (ops.stream().anyMatch(ProjectOperationSchemaHelper.LIMIT_OPERATIONS::contains)) {
            throw UNSUPPORTED_OPERATION_TYPE;
        }
        if (isAdmin) {
            return true;
        }

        if (MapUtils.isNotEmpty(operations)) {
            boolean find = ops.stream().anyMatch(op -> {
                OperationConfig config = operations.get(op);
                return config == null || !config.isEnable();
            });
            if (!find) {
                return true;
            }
        }
        return false;
    }

    public void checkLimitPermission(OperationType op, Map<String, Object> cardProps) {
        if (!hasLimitPermission(op, cardProps)) {
            throw CodedException.FORBIDDEN;
        }
    }

    public boolean hasLimitPermission(OperationType op, Map<String, Object> cardProps) {
        return hasLimitPermission(Arrays.asList(op), cardProps);
    }

    public void checkLimitPermission(OperationType op, Map<String, Object> fromCardProps, Map<String, Object> cardProps) {
        if (!hasLimitPermission(op, fromCardProps, cardProps)) {
            throw CodedException.FORBIDDEN;
        }
    }

    public boolean hasLimitPermission(OperationType op, Map<String, Object> fromCardProps, Map<String, Object> changeCardProps) {
        if (!hasLimitPermission(op, fromCardProps)) {
            return false;
        }
        Map<String, Object> toCardProps = new HashMap<>();
        toCardProps.putAll(fromCardProps);
        toCardProps.putAll(changeCardProps);
        return hasLimitPermission(op, toCardProps);
    }

    public boolean hasLimitPermission(OperationType op, Map<String, Object> fromCardProps, String field, Object value) {
        if (!hasLimitPermission(op, fromCardProps)) {
            return false;
        }
        Map<String, Object> toCardProps = new HashMap<>();
        toCardProps.putAll(fromCardProps);
        toCardProps.put(field, value);
        return hasLimitPermission(op, toCardProps);
    }

    public boolean hasLimitPermission(OperationType op, Map<String, Object> fromCardProps, String field1, Object value1, String field2, Object value2) {
        if (!hasLimitPermission(op, fromCardProps)) {
            return false;
        }
        Map<String, Object> toCardProps = new HashMap<>();
        toCardProps.putAll(fromCardProps);
        toCardProps.put(field1, value1);
        toCardProps.put(field2, value2);
        return hasLimitPermission(op, toCardProps);
    }

    public boolean hasLimitPermission(List<OperationType> ops, Map<String, Object> cardProps) {
        if (!ProjectOperationSchemaHelper.LIMIT_OPERATIONS.containsAll(ops)) {
            throw UNSUPPORTED_OPERATION_TYPE;
        }
        if (isAdmin) {
            return true;
        }
        String cardType = FieldUtil.getType(cardProps);
        List<OperationType> noPermissionOps = ops;
        if (MapUtils.isNotEmpty(operations)) {
            noPermissionOps = ops.stream()
                    .filter(op -> {
                        OperationConfig config = operations.get(op);
                        return config == null || !config.hasPermission(cardType);
                    })
                    .collect(Collectors.toList());
        }
        if (noPermissionOps.isEmpty()) {
            return true;
        }
        if (MapUtils.isNotEmpty(guestLimitOperations)) {
            boolean findNoPermissionOp = noPermissionOps.stream()
                    .anyMatch(op -> {
                        OperationConfig config = guestLimitOperations.get(op);
                        return config == null || !config.hasPermission(cardType) || !guestHasLimitPermission(op, cardProps);
                    });
            if (!findNoPermissionOp) {
                return true;
            }
        }
        return false;
    }

    private void mergeRole(ProjectRole role) {
        Map<OperationType, OperationConfig> roleOperations = role.getOperations();
        if (MapUtils.isEmpty(roleOperations)) {
            return;
        }
        roleOperations.entrySet().forEach(e -> {
            OperationType op = e.getKey();
            OperationConfig config = e.getValue();
            if (RoleType.GUEST.equals(role.getType()) && ProjectOperationSchemaHelper.LIMIT_OPERATIONS.contains(op)) {
                mergeOperationConfig(guestLimitOperations, op, config);
            } else {
                mergeOperationConfig(operations, op, config);
            }
        });
    }

    private void mergeOperationConfig(Map<OperationType, OperationConfig> operations, OperationType op, OperationConfig opConfig) {
        OperationConfig config = operations.get(op);
        if (config == null) {
            config = new OperationConfig();
            operations.put(op, config);
        }
        config.merge(opConfig);
    }

    private boolean guestHasLimitPermission(OperationType op, Map<String, Object> cardProps) {
        Long planId = FieldUtil.getPlanId(cardProps);
        if (!(planId == null || planId == 0)) {
            return false;
        }
        Long storyMapNodeId = FieldUtil.getStoryMapNodeId(cardProps);
        if (!(storyMapNodeId == null || storyMapNodeId == 0)) {
            return false;
        }
        List<String> ownerUsers = FieldUtil.getOwnerUsers(cardProps);
        if (CollectionUtils.isNotEmpty(ownerUsers)) {
            return false;
        }
        String status = FieldUtil.getStatus(cardProps);
        if (!CardStatus.FIRST.equals(status)) {
            return false;
        }
        if (!OperationType.CARD_CREATE.equals(op)) {
            String createUser = FieldUtil.getCreateUser(cardProps);
            if (StringUtils.isEmpty(createUser) || !createUser.equals(user)) {
                return false;
            }
        }
        return true;
    }
}
