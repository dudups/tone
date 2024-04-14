package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.enums.PortfolioOperationType;
import com.ezone.ezproject.es.entity.enums.RoleType;
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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserPortfolioPermissions {
    private String user = StringUtils.EMPTY;

    private boolean isAdmin;

    private boolean isMember;

    @ApiModelProperty(value = "操作权限设置")
    @Builder.Default
    private Map<PortfolioOperationType, OperationConfig> operations = new HashMap<>();

    @ApiModelProperty(value = "操作权限设置，只包括：guest类型角色的、依赖于卡片字段判断权限的操作设置")
    @Builder.Default
    private Map<PortfolioOperationType, OperationConfig> guestLimitOperations = new HashMap<>();

    public static final CodedException UNSUPPORTED_OPERATION_TYPE = new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, "传入了不支持的操作类型！");

    public static UserPortfolioPermissions from(String user, List<PortfolioRole> roles) {
        UserPortfolioPermissions permissions = new UserPortfolioPermissions();
        permissions.user = user;
        permissions.mergeRoles(roles);
        return permissions;
    }

    public static UserPortfolioPermissions fromAdmin(String user) {
        UserPortfolioPermissions permissions = new UserPortfolioPermissions();
        permissions.user = user;
        permissions.isAdmin = true;
        return permissions;
    }

    public void mergeRoles(List<PortfolioRole> roles) {
        if (isAdmin || CollectionUtils.isEmpty(roles)) {
            return;
        }
        for (PortfolioRole role : roles) {
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
     * @param op
     * @return
     */
    public void checkPermission(PortfolioOperationType op) {
        if (!hasPermission(Arrays.asList(op))) {
            throw CodedException.FORBIDDEN;
        }
    }

    /**
     * @param op
     * @return
     */
    public boolean hasPermission(PortfolioOperationType op) {
        return hasPermission(Arrays.asList(op));
    }

    /**
     * @param ops
     * @return
     */
    public boolean hasPermission(List<PortfolioOperationType> ops) {
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

    private void mergeRole(PortfolioRole role) {
        Map<PortfolioOperationType, OperationConfig> roleOperations = role.getOperations();
        if (MapUtils.isEmpty(roleOperations)) {
            return;
        }
        roleOperations.entrySet().forEach(e -> {
            PortfolioOperationType op = e.getKey();
            OperationConfig config = e.getValue();
            mergeOperationConfig(operations, op, config);
        });
    }

    private void mergeOperationConfig(Map<PortfolioOperationType, OperationConfig> operations, PortfolioOperationType op, OperationConfig opConfig) {
        OperationConfig config = operations.get(op);
        if (config == null) {
            config = new OperationConfig();
            operations.put(op, config);
        }
        config.merge(opConfig);
    }

}
