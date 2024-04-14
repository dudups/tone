package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class RoleSchema {
    public static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory()
            .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public abstract List<Role> toBaseRoles();

    public abstract void resetRoles(List<Role> roles);

    @ApiModelProperty(value = "优先级排序地位，字典序")
    private String maxRank;

    public String yaml() throws JsonProcessingException {
        return YAML_MAPPER.writeValueAsString(this);
    }

    public List<Role> findRoles(RoleSource... source) {
        List<RoleSource> sources = Arrays.asList(source);
        return toBaseRoles().stream().filter(r -> sources.contains(r.getSource())).collect(Collectors.toList());
    }

    public Role findRole(RoleSource source, String key) {
        return toBaseRoles().stream().filter(r -> r.is(source, key)).findAny().orElse(null);
    }

    public Role findRole(String source, String key) {
        return findRole(RoleSource.valueOf(source), key);
    }

    public int findRoleIndex(RoleSource source, String key) {
        for (int i = 0; i < toBaseRoles().size(); i++) {
            Role role = toBaseRoles().get(i);
            if (role.is(source, key)) {
                return i;
            }
        }
        return -1;
    }

    public int findRoleIndex(String source, String key) {
        return findRoleIndex(RoleSource.valueOf(source), key);
    }
}
