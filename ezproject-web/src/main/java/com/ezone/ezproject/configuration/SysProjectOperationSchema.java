package com.ezone.ezproject.configuration;

import com.ezone.ezproject.es.entity.ProjectOperationSchema;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class SysProjectOperationSchema {
    @Getter(lazy = true)
    private final byte[] sysProjectOperationSchemaContent = sysProjectOperationSchemaContent();

    private byte[] sysProjectOperationSchemaContent() {
        try {
            return IOUtils.toByteArray(
                    SysProjectOperationSchema.class.getResource("/sys-operation-schema.yaml"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Bean
    public ProjectOperationSchema projectOperationSchema() throws IOException {
        return ProjectOperationSchema.YAML_MAPPER.readValue(
                getSysProjectOperationSchemaContent(),
                ProjectOperationSchema.class
        );
    }

}
