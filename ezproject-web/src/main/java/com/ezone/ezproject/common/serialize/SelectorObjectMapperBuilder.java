package com.ezone.ezproject.common.serialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 支持selector的yaml/json反序列化ObjectMapper的构造器;<br>
 * 使用：<br>
 * 1. 配置反序列化规则：指定针对clazz A，可反序列化为哪些子类A1,A2...；配置selector，reference属性名以及reference的context容器;<br>
 * 2. yaml/json文件：selector: a2，通过实现类的SimpleName驼峰形式指定具体子类，配合a2: ...指定A2的属性及其值;<br>
 * 3. yaml/json文件：selector: reference，表示引用，配合reference: beanName指定所引用的spring容器对象;<br>
 */
public class SelectorObjectMapperBuilder {
    private String selector = "selector";

    private String reference = "reference";

    private SimpleModule simpleModule = new SimpleModule();

    private ApplicationContext context;

    private ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory()
            .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static SelectorObjectMapperBuilder builder() {
        return new SelectorObjectMapperBuilder();
    }

    /**
     * 指定selector属性名，默认selector
     * @param selector
     * @return
     */
    public SelectorObjectMapperBuilder selector(@NotNull String selector) {
        this.selector = selector;
        return this;
    }

    /**
     * 指定reference属性名，默认reference
     * @param reference
     * @return
     */
    public SelectorObjectMapperBuilder reference(@NotNull String reference) {
        this.reference = reference;
        return this;
    }

    /**
     * 指定用来查找reference-bean的context容器
     * @param context
     * @return
     */
    public SelectorObjectMapperBuilder context(ApplicationContext context) {
        this.context = context;
        return this;
    }

    /**
     * 注册反序列化类及其实现类
     * @param clazz
     * @param impls
     * @param <T>
     * @return
     */
    public <T> SelectorObjectMapperBuilder clazzImpls(@NotNull Class<T> clazz, @NotNull Class<? extends T>... impls) {
        Map<String, Class<? extends T>> clazzMap = Arrays.stream(impls).collect(Collectors.toMap(
                c -> StringUtils.uncapitalize(c.getSimpleName()),
                Function.identity()));
        simpleModule.addDeserializer(clazz, new JsonDeserializer<T>() {
            @Override
            public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                TreeNode rootNode = p.readValueAsTree();
                TreeNode selectorNode = rootNode.get(SelectorObjectMapperBuilder.this.selector);
                if (null == selectorNode || !(selectorNode instanceof TextNode)) {
                    throw new IllegalArgumentException(String.format("Not find or invalid selector property:[%s]!", selector));
                }
                String selectorText = ((TextNode) selectorNode).asText();
                if (reference.equals(selectorText)) {
                    if (null == context) {
                        throw new IllegalArgumentException(String.format("Not support for reference property:[%s] with null context!", reference));
                    }
                    TreeNode referenceNode = rootNode.get(reference);
                    if (null == referenceNode || !(referenceNode instanceof TextNode)) {
                        throw new IllegalArgumentException(String.format("Not find or invalid reference property:[reference]!", reference));
                    }
                    return (T) context.getBean(((TextNode) referenceNode).asText());
                }
                Class<? extends T> implClazz = clazzMap.get(selectorText);
                if (null == implClazz) {
                    throw new IllegalArgumentException(String.format("Not find class for selector value:[%s]!", selectorText));
                }
                TreeNode implNode = rootNode.get(StringUtils.uncapitalize(selectorText));
                return objectMapper.readValue(p.getCodec().treeAsTokens(implNode), implClazz);
            }
        });
        return this;
    }

    public ObjectMapper build() {
        return objectMapper.registerModule(simpleModule);
    }
}
