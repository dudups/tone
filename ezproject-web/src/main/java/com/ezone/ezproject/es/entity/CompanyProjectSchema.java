package com.ezone.ezproject.es.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CompanyProjectSchema {
    public static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory()
            .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    @ApiModelProperty(value = "项目扩展字段列表")
    @Singular
    private List<ProjectField> fields;

    public ProjectField findProjectField(String key) {
        return fields.stream().filter(f -> f.getKey().equals(key)).findAny().orElse(null);
    }

    public static CompanyProjectSchema from(String yaml) throws JsonProcessingException {
        return YAML_MAPPER.readValue(yaml, CompanyProjectSchema.class);
    }

    public String yaml() throws JsonProcessingException {
        return YAML_MAPPER.writeValueAsString(this);
    }

    public List<ProjectField> mergeFields(List<ProjectField> fields) {
        Set<String> toCustomFieldKeys = fields.stream()
                .filter(f -> StringUtils.startsWith(f.getKey(), "custom_"))
                .map(f -> f.getKey())
                .collect(Collectors.toSet());
        List<ProjectField> retainCustomFields = this.fields.stream()
                .filter(f -> StringUtils.startsWith(f.getKey(), "custom_"))
                .filter(f -> !toCustomFieldKeys.contains(f.getKey()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(retainCustomFields)) {
            return fields;
        } else {
            List<ProjectField> mergeFields = new ArrayList<>(retainCustomFields);
            mergeFields.addAll(fields);
            return mergeFields;
        }
    }
}
