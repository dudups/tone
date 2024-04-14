package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.modules.project.bean.CompanyCardTypeConf;
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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CompanyCardSchema {
    public static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory()
            .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @ApiModelProperty(value = "卡片类型设置")
    @Singular
    private List<CompanyCardTypeConf> types;

    public static CompanyCardSchema from(String yaml) throws JsonProcessingException {
        return YAML_MAPPER.readValue(yaml, CompanyCardSchema.class);
    }

    public String yaml() throws JsonProcessingException {
        return YAML_MAPPER.writeValueAsString(this);
    }

    public void mergeTypes(List<CompanyCardTypeConf> types) {
        if (CollectionUtils.isEmpty(types)) {
            return;
        }
        Map<String, CompanyCardTypeConf> typeMap = types.stream()
                .filter(type -> StringUtils.isNotEmpty(type.getKey()))
                .collect(Collectors.toMap(type -> type.getKey(), Function.identity()));
        this.types.stream().forEach(type -> {
            CompanyCardTypeConf mergeType = typeMap.get(type.getKey());
            if (mergeType != null) {
                type.setName(mergeType.getName());
                type.setDescription(mergeType.getDescription());
                type.setColor(mergeType.getColor());
            }
        });
    }

    public String findCardTypeName(String typeKey) {
        List<CompanyCardTypeConf> types = getTypes();
        if (CollectionUtils.isEmpty(types)) {
            return "";
        }
        return types.stream().filter(type -> type.getKey().equals(typeKey)).map(CompanyCardTypeConf::getName).findFirst().orElse("");
    }
}
