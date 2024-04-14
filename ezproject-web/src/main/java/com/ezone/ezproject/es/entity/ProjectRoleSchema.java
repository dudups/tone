package com.ezone.ezproject.es.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRoleSchema extends RoleSchema {
    public static ProjectRoleSchema from(String yaml) throws JsonProcessingException {
        return YAML_MAPPER.readValue(yaml, ProjectRoleSchema.class);
    }

    @ApiModelProperty(value = "项目下角色列表")
    @Builder.Default
    private List<ProjectRole> roles = new ArrayList<>();

    @Override
    public List<Role> toBaseRoles() {
        return roles.stream().map(Role.class::cast).collect(Collectors.toList());
    }

    @Override
    public void resetRoles(List<Role> roles) {
        if (CollectionUtils.isNotEmpty(roles)) {
            this.roles = roles.stream().filter(role -> role instanceof ProjectRole).map(ProjectRole.class::cast).collect(Collectors.toList());
        } else {
            this.roles = new ArrayList<>();
        }
    }
}
