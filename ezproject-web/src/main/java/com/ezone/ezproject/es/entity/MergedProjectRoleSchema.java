package com.ezone.ezproject.es.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MergedProjectRoleSchema extends ProjectRoleSchema {
    private List<ProjectRole> refCompanyRoles = new ArrayList<>();

    public static MergedProjectRoleSchema from(ProjectRoleSchema schema) {
        if (schema == null) {
            return null;
        }
        MergedProjectRoleSchema mergedSchema = new MergedProjectRoleSchema();
        mergedSchema.resetRoles(schema.toBaseRoles());
        return mergedSchema;
    }
}
