package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.es.entity.enums.RoleType;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang.StringUtils;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Objects;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public abstract class Role {
    @ApiModelProperty(value = "项目角色标识key", example = "admin")
    private String key;
    @NotNull
    @Size(min = 2, max = 16, message = "角色名长度2-16位")
    @ApiModelProperty(value = "项目角色名称", example = "管理员")
    private String name;
    @NotNull
    @ApiModelProperty(value = "项目角色来源")
    private RoleSource source;
    @NotNull
    @ApiModelProperty(value = "项目角色类型")
    private RoleType type;
    @ApiModelProperty(value = "优先级排序地位，字典序")
    private String rank;

    public boolean is(ProjectRole role) {
        return role != null && is(role.getSource(), role.getKey());
    }

    public boolean is(RoleSource source, String key) {
        return this.source == source && StringUtils.equals(key, this.key);
    }

    public void mergeDefault(Role role) {
        if (role == null) {
            return;
        }
        this.name = role.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectRole role = (ProjectRole) o;
        return is(role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, key);
    }
}
