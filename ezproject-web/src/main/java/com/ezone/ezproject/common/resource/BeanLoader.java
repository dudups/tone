package com.ezone.ezproject.common.resource;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

public final class BeanLoader {
    private ObjectMapper mapper;

    private BeanLoader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public <T> T fromResource(String path, Class<T> clazz) {
        try {
            return this.mapper.readValue(IOUtils.toByteArray(
                    BeanLoader.class.getResource(path)), clazz);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory()
            .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static final BeanLoader YAML = new BeanLoader(YAML_MAPPER);
    public static final BeanLoader JSON = new BeanLoader(JSON_MAPPER);
}
