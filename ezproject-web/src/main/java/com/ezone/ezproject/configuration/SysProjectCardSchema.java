package com.ezone.ezproject.configuration;

import com.ezone.ezproject.es.entity.ProjectCardSchema;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class SysProjectCardSchema {
    @Getter(lazy = true)
    private final byte[] sysProjectCardSchemaContent = sysProjectCardSchemaContent();

    private byte[] sysProjectCardSchemaContent() {
        try {
            return IOUtils.toByteArray(
                    SysProjectCardSchema.class.getResource("/sys-card-schema.yaml"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Bean
    public ProjectCardSchema projectCardSchema() throws IOException {
        return ProjectCardSchema.YAML_MAPPER.readValue(
                getSysProjectCardSchemaContent(),
                ProjectCardSchema.class
        );
    }

}
