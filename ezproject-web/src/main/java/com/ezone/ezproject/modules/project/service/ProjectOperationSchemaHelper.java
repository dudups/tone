package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.configuration.SysProjectOperationSchema;
import com.ezone.ezproject.es.entity.ProjectOperationSchema;
import com.ezone.ezproject.es.entity.enums.OperationType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ProjectOperationSchemaHelper {
    @Getter(lazy = true)
    private final byte[] sysProjectOperationSchemaContent = sysProjectOperationSchemaContent();

    @Getter(lazy = true)
    private final ProjectOperationSchema sysProjectOperationSchema = newSysProjectOperationSchema();

    /**
     * 依赖于卡片类型/其它字段判断权限的操作
     */
    public static Set<OperationType> LIMIT_OPERATIONS;

    @PostConstruct
    public void setStaticProp() {
        LIMIT_OPERATIONS = getSysProjectOperationSchema()
                .getGroups().stream()
                .flatMap(group -> group.getOperations().stream())
                .filter(operation -> operation.isGuestLimit())
                .map(operation -> OperationType.valueOf(operation.getKey()))
                .collect(Collectors.toSet());
    }

    private byte[] sysProjectOperationSchemaContent() {
        try {
            return IOUtils.toByteArray(
                    SysProjectOperationSchema.class.getResource("/sys-operation-schema.yaml"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public ProjectOperationSchema newSysProjectOperationSchema() {
        try {
            return ProjectOperationSchema.YAML_MAPPER.readValue(
                    getSysProjectOperationSchemaContent(),
                    ProjectOperationSchema.class
            );
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
