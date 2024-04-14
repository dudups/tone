package com.ezone.ezproject.es.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyWorkloadSetting extends ProjectWorkloadSetting {
    private boolean forceCompanyRule;

    public static CompanyWorkloadSetting from(String yaml) throws JsonProcessingException {
        return YAML_MAPPER.readValue(yaml, CompanyWorkloadSetting.class);
    }
}
