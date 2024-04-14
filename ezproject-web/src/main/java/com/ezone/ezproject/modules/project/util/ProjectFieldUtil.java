package com.ezone.ezproject.modules.project.util;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.ProjectField;
import com.ezone.ezproject.es.entity.enums.FieldType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public final class ProjectFieldUtil {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private ProjectFieldUtil() { }

    public static Long toLong(Object value) {
        if (null == value) {
            return 0L;
        } else if (value instanceof Long) {
            return (Long) value;
        }
        return NumberUtils.createLong(value.toString());
    }

    public static Float toFloat(Object value) {
        if (null == value) {
            return 0f;
        } else if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return NumberUtils.createFloat(value.toString());
    }

    public static boolean toBoolean(Object value) {
        return value instanceof Boolean && (Boolean) value;
    }

    public static String toString(Object value) {
        if (null == value) {
            return null;
        } else if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Collection) {
            return StringUtils.join((Collection) value, " ");
        } else if (value instanceof String[]) {
            return StringUtils.join((String[]) value, " ");
        } else if (value instanceof Object[]) {
            return StringUtils.join((Object[]) value, " ");
        }
        return value.toString();
    }

    private static <T> T getFieldValue(Map<String, Object> cardProps, String field, Function<Object, T> converter, T defaultValue) {
        if (cardProps == null) {
            return defaultValue;
        }
        return converter.apply(cardProps.get(field));
    }

    private static <T> T getFieldValue(Map<String, Object> cardProps, String field, Function<Object, T> converter) {
        if (cardProps == null) {
            return null;
        }
        return converter.apply(cardProps.get(field));
    }

    public static List<String> toStringList(Object value) {
        if (null == value) {
            return ListUtils.EMPTY_LIST;
        } else if (value instanceof String) {
            return Arrays.asList((String) value);
        } else if (value instanceof Collection) {
            List<String> strings = new ArrayList<>();
            ((Collection) value).forEach(v -> strings.add(ProjectFieldUtil.toString(v)));
            return strings;
        } else if (value instanceof String[]) {
            return Stream.of((String[]) value).map(ProjectFieldUtil::toString).collect(Collectors.toList());
        }
        return Arrays.asList(value.toString());
    }

    public static List<Long> toLongList(Object value) {
        if (null == value) {
            return ListUtils.EMPTY_LIST;
        } else if (value instanceof Long) {
            return Arrays.asList((Long) value);
        } else if (value instanceof Collection) {
            List<Long> longs = new ArrayList<>();
            ((Collection) value).forEach(v -> longs.add(ProjectFieldUtil.toLong(v)));
            return longs;
        } else if (value instanceof String[]) {
            return Stream.of((String[]) value).map(ProjectFieldUtil::toLong).collect(Collectors.toList());
        } else if (value instanceof Long[]) {
            return Arrays.asList((Long[]) value);
        }
        return Arrays.asList(ProjectFieldUtil.toLong(value));
    }

    public static boolean isEmptyValue(Object value) {
        if (null == value) {
            return true;
        } else if (value instanceof String) {
            return StringUtils.isEmpty((String) value);
        } else if (value instanceof Collection) {
            return ((Collection) value).size() == 0;
        }
        return false;
    }

    public static boolean checkInOptions(Object value, List<ProjectField.Option> options) {
        if (null == value) {
            return true;
        }
        return options.stream().anyMatch(o -> o.getName().equals(value));
    }

    public static boolean checkAllInOptions(Object value, List<ProjectField.Option> options) {
        if (null == value) {
            return true;
        }
        if (!(value instanceof List)) {
            return false;
        }
        for (Object v : ((List) value)) {
            if (!options.stream().anyMatch(o -> o.getName().equals(v))) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(ProjectField field, Object v1, Object v2) {
        if (isEmptyValue(v1) && isEmptyValue(v2)) {
            return true;
        }
        if (v1 instanceof Collection) {
            return v2 instanceof List && ((Collection) v1).containsAll((Collection) v2) && ((Collection) v2).containsAll((Collection) v1);
        }
        if (isEmptyValue(v1)) {
            return isEmptyValue(v2);
        }
        return String.valueOf(v1).equals(String.valueOf(v2));
    }

    public static Object parse(FieldType.ValueType valueType, String value) throws CodedException {
        if (null == valueType || null == value) {
            return value;
        }
        try {
            switch (valueType) {
                case LONG:
                case DATE:
                    return NumberUtils.toLong(value);
                case LONGS:
                    return JSON_MAPPER.readValue(value, new TypeReference<List<Long>>() {});
                case FLOAT:
                    return NumberUtils.toFloat(value);
                case BOOLEAN:
                    return BooleanUtils.toBoolean(value);
                case STRINGS:
                    return JSON_MAPPER.readValue(value, new TypeReference<List<String>>() {});
                default:
                    return value;
            }
        } catch (Exception e) {
            log.error("Parse field value exception!", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
