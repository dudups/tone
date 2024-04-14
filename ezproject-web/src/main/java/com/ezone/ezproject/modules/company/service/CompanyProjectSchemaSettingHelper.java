package com.ezone.ezproject.modules.company.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.CompanyProjectSchema;
import com.ezone.ezproject.es.entity.ProjectField;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class CompanyProjectSchemaSettingHelper {
    private CompanyProjectSchemaHelper schemaHelper;

    public CompanyProjectSchema setFields(CompanyProjectSchema schema, List<ProjectField> fields) {
        checkFieldNameConflict(schema);
        schema.setFields(fields);
        schemaHelper.generateCustomFieldKey(schema);
        return schema;
    }

    public void checkFieldNameConflict(CompanyProjectSchema schema) {
        List<ProjectField> fields = schema.getFields();
        if (CollectionUtils.isEmpty(fields)) {
            return;
        }
        if (fields.size() > fields.stream().map(f -> f.getName()).distinct().count()) {
            throw new CodedException(HttpStatus.CONFLICT, "字段名冲突！");
        }
    }
}
