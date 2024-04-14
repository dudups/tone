package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.es.entity.ProjectRole;
import com.ezone.ezproject.es.entity.ProjectRoleSchema;
import com.ezone.ezproject.es.entity.Role;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@Slf4j
public class ProjectRoleSchemaSettingHelper {
    private ProjectRoleSchemaHelper schemaHelper;

    public ProjectRoleSchema mergeRoles(ProjectRoleSchema schema, RoleSource source, List<ProjectRole> roles) {
        if (CollectionUtils.isNotEmpty(roles)) {
            schemaHelper.mergeRoles(schema, source, roles.stream().map(Role.class::cast).collect(Collectors.toList()));
        }

        schemaHelper.checkNameConflict(schema, source);
        return schema;
    }

    public ProjectRoleSchema saveRoles(ProjectRoleSchema schema, RoleSource source, List<ProjectRole> roles) {
        if (CollectionUtils.isNotEmpty(roles)) {
            schemaHelper.saveRoles(schema, source, roles.stream().map(Role.class::cast).collect(Collectors.toList()));
        }
        schemaHelper.checkNameConflict(schema, source);
        return schema;
    }

    public ProjectRoleSchema addRole(ProjectRoleSchema schema, RoleSource source, ProjectRole role) {
        schemaHelper.addRole(schema, source, role);
        schemaHelper.checkNameConflict(schema, source);
        return schema;
    }

    public ProjectRoleSchema updateRole(ProjectRoleSchema schema, RoleSource source, ProjectRole role) {
        schemaHelper.updateRole(schema, source, role);
        schemaHelper.checkNameConflict(schema, role.getSource());
        return schema;
    }

    public ProjectRoleSchema deleteRole(ProjectRoleSchema schema, RoleSource source, String roleKey) {
        schemaHelper.removeRole(schema, source, roleKey);
        return schema;
    }

    private <T> void filter(Collection<T> collection, Function<T, Boolean> filter) {
        if (collection != null && filter != null) {
            for (Iterator<T> it = collection.iterator(); it.hasNext(); ) {
                if (BooleanUtils.isNotTrue(filter.apply(it.next()))) {
                    it.remove();
                }
            }
        }
    }

    private <T> void forEach(Collection<T> collection, Consumer<? super T> action) {
        if (collection != null && action != null) {
            for (Iterator<T> it = collection.iterator(); it.hasNext(); ) {
                action.accept(it.next());
            }
        }
    }

}
