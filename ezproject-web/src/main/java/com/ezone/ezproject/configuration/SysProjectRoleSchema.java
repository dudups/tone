package com.ezone.ezproject.configuration;

import com.ezone.ezproject.es.entity.ProjectRoleSchema;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class SysProjectRoleSchema {
    @Getter(lazy = true)
    private final byte[] sysProjectRoleSchemaContent = sysProjectRoleSchemaContent();

    private byte[] sysProjectRoleSchemaContent() {
        try {
            return IOUtils.toByteArray(
                    SysProjectRoleSchema.class.getResource("/sys-role-schema.yaml"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Bean
    public ProjectRoleSchema projectRoleSchema() throws IOException {
        return ProjectRoleSchema.YAML_MAPPER.readValue(
                getSysProjectRoleSchemaContent(),
                ProjectRoleSchema.class
        );
    }

}
