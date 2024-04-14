package com.ezone.ezproject.modules.company.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.modules.project.bean.CompanyCardTypeConf;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class CompanyCardSchemaHelper {
    @Getter(lazy = true)
    private final byte[] sysCompanyCardSchemaContent = sysCompanyCardSchemaContent();

    @Getter(lazy = true)
    private final CompanyCardSchema sysCompanyCardSchema = newSysCompanyCardSchema();

    public CompanyCardSchema newSysCompanyCardSchema() {
        try {
            return CompanyCardSchema.YAML_MAPPER.readValue(
                    getSysCompanyCardSchemaContent(),
                    CompanyCardSchema.class
            );
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public CompanyCardSchema fillSysSchema(CompanyCardSchema schema) throws CodedException {
        if (null == schema) {
            return getSysCompanyCardSchema();
        }

        List<CompanyCardTypeConf> mergeTypes = schema.getTypes();
        schema.setTypes(newSysCompanyCardSchema().getTypes());
        schema.mergeTypes(mergeTypes);
        return schema;
    }

    private byte[] sysCompanyCardSchemaContent() {
        try {
            return IOUtils.toByteArray(
                    CompanyCardSchema.class.getResource("/sys-company-card-schema.yaml"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
