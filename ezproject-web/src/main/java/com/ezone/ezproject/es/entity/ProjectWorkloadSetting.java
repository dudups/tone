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
import lombok.experimental.SuperBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectWorkloadSetting {
    public static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory()
            .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private boolean enableIncrWorkload;
    private List<Rule> rules;

    private List<Rule> revertRules;

    public Rule findMatchRule(String cardType) {
        if (StringUtils.isEmpty(cardType) || CollectionUtils.isEmpty(rules)) {
            return null;
        }
        for (Rule rule : rules) {
            if (rule.isEnable() && CollectionUtils.isNotEmpty(rule.getCardTypes()) && rule.getCardTypes().contains(cardType)) {
                return rule;
            }
        }
        return null;
    }

    public Rule findMatchRevertRule(String cardType) {
        if (StringUtils.isEmpty(cardType) || CollectionUtils.isEmpty(revertRules)) {
            return null;
        }
        for (Rule rule : revertRules) {
            if (rule.isEnable() && CollectionUtils.isNotEmpty(rule.getCardTypes()) && rule.getCardTypes().contains(cardType)) {
                return rule;
            }
        }
        return null;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rule {
        @Size(min = 1)
        private List<String> cardTypes;
        private boolean enable;
        @ApiModelProperty(value = "绑定的bpm审批流模版ID")
        @Min(1)
        private Long bpmFlowTemplateId;
    }

    public static ProjectWorkloadSetting from(String yaml) throws JsonProcessingException {
        return YAML_MAPPER.readValue(yaml, ProjectWorkloadSetting.class);
    }

    public String yaml() throws JsonProcessingException {
        return YAML_MAPPER.writeValueAsString(this);
    }
}
