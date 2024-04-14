package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.es.entity.enums.PortfolioOperationType;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 项目集操作定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioOperationSchema {
    public static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory()
            .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @ApiModelProperty(value = "在系统内置MEMBER角色下，固化的不许自定义的操作")
    @Builder.Default
    private Set<PortfolioOperationType> sysMemberFixedOps = new HashSet<>();

    @ApiModelProperty(value = "在自定义MEMBER角色下，固化的不许自定义的操作")
    @Builder.Default
    private Set<PortfolioOperationType> customMemberFixedOps = new HashSet<>();

    @ApiModelProperty(value = "项目下角色列表")
    @Builder.Default
    private List<Group> groups = new ArrayList<>();

    public static PortfolioOperationSchema from(String yaml) throws JsonProcessingException {
        return YAML_MAPPER.readValue(yaml, PortfolioOperationSchema.class);
    }

    public String yaml() throws JsonProcessingException {
        return YAML_MAPPER.writeValueAsString(this);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Group {
        private String name;
        private String tooltip;
        private List<Operation> operations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Operation {
        private String key;
        private String name;
        private String tooltip;
    }
}
