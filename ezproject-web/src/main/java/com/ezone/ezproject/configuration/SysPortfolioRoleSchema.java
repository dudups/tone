package com.ezone.ezproject.configuration;

import com.ezone.ezproject.es.entity.PortfolioRoleSchema;
import com.ezone.ezproject.es.entity.ProjectRoleSchema;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class SysPortfolioRoleSchema {
    @Getter(lazy = true)
    private final byte[] sysProjectRoleSchemaContent = sysProjectRoleSchemaContent();

    private byte[] sysProjectRoleSchemaContent() {
        try {
            return IOUtils.toByteArray(
                    SysPortfolioRoleSchema.class.getResource("/sys-portfolio-role-schema.yaml"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Bean
    public PortfolioRoleSchema portfolioRoleSchema() throws IOException {
        return PortfolioRoleSchema.YAML_MAPPER.readValue(
                getSysProjectRoleSchemaContent(),
                PortfolioRoleSchema.class
        );
    }

}
