package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.es.entity.enums.OperationType;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProjectRole extends Role {
    public static final String ADMIN = "ADMIN";
    public static final String MEMBER = "MEMBER";
    public static final String GUEST = "GUEST";

    @Singular
    @ApiModelProperty(value = "项目角色的操作权限设置")
    private Map<OperationType, OperationConfig> operations;
}
