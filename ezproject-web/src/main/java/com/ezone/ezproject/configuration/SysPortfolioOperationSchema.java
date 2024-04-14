package com.ezone.ezproject.configuration;

import com.ezone.ezproject.es.entity.PortfolioOperationSchema;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class SysPortfolioOperationSchema {
    @Getter(lazy = true)
    private final byte[] sysPortfolioOperationSchemaContent = sysPortfolioOperationSchemaContent();

    private byte[] sysPortfolioOperationSchemaContent() {
        try {
            return IOUtils.toByteArray(
                    SysPortfolioOperationSchema.class.getResource("/sys-portfolio-operation-schema.yaml"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Bean
    public PortfolioOperationSchema portfolioOperationSchema() throws IOException {
        return PortfolioOperationSchema.YAML_MAPPER.readValue(
                getSysPortfolioOperationSchemaContent(),
                PortfolioOperationSchema.class
        );
    }

}
