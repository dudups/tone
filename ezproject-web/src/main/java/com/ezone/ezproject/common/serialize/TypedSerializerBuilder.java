package com.ezone.ezproject.common.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@NoArgsConstructor
public class TypedSerializerBuilder<T> {
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private String type = "type";
    private Function<Class<? extends T>, String> clazzToType = c -> StringUtils.uncapitalize(c.getSimpleName());
    private Class<T> clazz;
    private List<Class<? extends T>> impls;

    public TypedSerializerBuilder<T> type(@NotNull String type) {
        this.type = type;
        return this;
    }

    public TypedSerializerBuilder<T> clazz(@NotNull Class<T> clazz) {
        this.clazz = clazz;
        return this;
    }

    public TypedSerializerBuilder<T> clazzToType(@NotNull Function<Class<? extends T>, String> clazzToType) {
        this.clazzToType = clazzToType;
        return this;
    }

    public TypedSerializerBuilder<T> impls(@NotNull Class<? extends T>... impls) {
        this.impls = Arrays.asList(impls);
        return this;
    }

    public TypedSerializerBuilder<T> impls(@NotNull List<Class<? extends T>> impls) {
        this.impls = impls;
        return this;
    }

    public JsonDeserializer<T> deserializer() {
        Map<String, Class> clazzMap = impls.stream().collect(Collectors.toMap(clazzToType, Function.identity()));
        return new JsonDeserializer<T> () {
            @Override
            public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                ObjectNode rootNode = p.readValueAsTree();
                TreeNode typeNode = rootNode.remove(type);
                if (null == typeNode || !(typeNode instanceof TextNode)) {
                    throw new IllegalArgumentException(String.format("Null or invalid type property!"));
                }
                String type = ((TextNode) typeNode).asText();
                Class<? extends T> implClazz = clazzMap.get(type);
                if (null == implClazz) {
                    throw new IllegalArgumentException(String.format("Not find class for type:[%s]!", type));
                }
//                try {
//                    T value = implClazz.getDeclaredConstructor().newInstance();
//                    Iterator<Map.Entry<String, JsonNode>> iterator = rootNode.fields();
//                    while (iterator.hasNext()) {
//                        Map.Entry<String, JsonNode> entry = iterator.next();
//                        Class clazz = FieldUtils.getDeclaredField(implClazz, entry.getKey()).getType();
//                        FieldUtils.writeDeclaredField(entry.getKey(), MAPPER.treeToValue(entry.get)
//                    }
//                    return value;
//                } catch (ReflectiveOperationException e) {
//                    throw ValueInstantiationException.from(p, e.getMessage());
//                }


//                p = MAPPER.treeAsTokens(rootNode);
//                ctxt.getParser().setCodec(p.getCodec());
//                p.nextToken();
//                JavaType javaType = ctxt.getTypeFactory().constructType(implClazz);
//                return (T) ctxt.getFactory().createBeanDeserializer(ctxt, javaType, ctxt.getConfig().introspect(javaType)).deserialize(p, ctxt);

                return MAPPER.treeToValue(rootNode, implClazz);
            }
        };
    }

    public JsonSerializer<T> serializer() {
        return new JsonSerializer<T>() {
            @Override
            public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                ObjectNode node = MAPPER.valueToTree(value);
                node.put(type, StringUtils.uncapitalize(value.getClass().getSimpleName()));
                gen.writeTree(node);
            }
        };
    }
}
