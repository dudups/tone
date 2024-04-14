package com.ezone.ezproject.modules.portfolio.service;

import com.ezone.ezproject.configuration.SysPortfolioOperationSchema;
import com.ezone.ezproject.es.entity.PortfolioOperationSchema;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class PortfolioOperationSchemaHelper {
    @Getter(lazy = true)
    private final byte[] sysPortfolioOperationSchemaContent = sysPortfolioOperationSchemaContent();

    @Getter(lazy = true)
    private final PortfolioOperationSchema sysPortfolioOperationSchema = newSysPortfolioOperationSchema();

    private byte[] sysPortfolioOperationSchemaContent() {
        try {
            return IOUtils.toByteArray(
                    SysPortfolioOperationSchema.class.getResource("/sys-portfolio-operation-schema.yaml"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public PortfolioOperationSchema newSysPortfolioOperationSchema() {
        try {
            return PortfolioOperationSchema.YAML_MAPPER.readValue(
                    getSysPortfolioOperationSchemaContent(),
                    PortfolioOperationSchema.class
            );
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
