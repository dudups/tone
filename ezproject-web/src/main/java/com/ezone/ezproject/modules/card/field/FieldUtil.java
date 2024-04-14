package com.ezone.ezproject.modules.card.field;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.enums.FieldType;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.card.excel.CustomerExcelAnalysisException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.http.HttpStatus;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public final class FieldUtil {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    public static final String JOIN_SEPARATOR = " ";

    private FieldUtil() { }

    @NotNull
    public static Long toLong(Object value) {
        if (null == value) {
            return 0L;
        } else if (value instanceof Long) {
            return (Long) value;
        }
        return NumberUtils.createLong(value.toString());
    }

    @NotNull
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
            return StringUtils.join((Collection) value, JOIN_SEPARATOR);
        } else if (value instanceof String[]) {
            return StringUtils.join((String[]) value, JOIN_SEPARATOR);
        } else if (value instanceof Object[]) {
            return StringUtils.join((Object[]) value, JOIN_SEPARATOR);
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

    public static String getTitle(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.TITLE, FieldUtil::toString);
    }

    public static String getContent(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.CONTENT, FieldUtil::toString);
    }

    public static Long getSeqNum(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.SEQ_NUM, FieldUtil::toLong);
    }

    public static Long getCompanyId(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.COMPANY_ID, FieldUtil::toLong);
    }

    public static Long getPlanId(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.PLAN_ID, FieldUtil::toLong);
    }

    public static Long getFirstPlanId(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.FIRST_PLAN_ID, FieldUtil::toLong);
    }

    public static Long getProjectId(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.PROJECT_ID, FieldUtil::toLong);
    }

    public static Long getStoryMapNodeId(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.STORY_MAP_NODE_ID, FieldUtil::toLong);
    }

    public static Long getBpmFlowId(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.BPM_FLOW_ID, FieldUtil::toLong);
    }

    public static String getBpmFlowToStatus(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.BPM_FLOW_TO_STATUS, FieldUtil::toString);
    }

    public static String getType(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.TYPE, FieldUtil::toString);
    }

    public static String getInnerType(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.INNER_TYPE, FieldUtil::toString);
    }

    public static String getStatus(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.STATUS, FieldUtil::toString);
    }

    public static String getCreateUser(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.CREATE_USER, FieldUtil::toString);
    }

    public static List<String> getAtUsers(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.AT_USERS, FieldUtil::toStringList);
    }

    public static List<String> getOwnerUsers(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.OWNER_USERS, FieldUtil::toStringList);
    }

    public static List<String> getWatchUsers(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.WATCH_USERS, FieldUtil::toStringList);
    }

    public static boolean getDeleted(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.DELETED, FieldUtil::toBoolean);
    }

    public static boolean getProjectIsActive(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.PROJECT_IS_ACTIVE, FieldUtil::toBoolean);
    }

    public static boolean getPlanIsActive(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.PLAN_IS_ACTIVE, FieldUtil::toBoolean);
    }

    public static boolean getCalcIsEnd(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.CALC_IS_END, FieldUtil::toBoolean);
    }

    @NotNull
    public static Long getLastEndTime(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.LAST_END_TIME, FieldUtil::toLong);
    }

    @NotNull
    public static Long getEndDate(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.END_DATE, FieldUtil::toLong);
    }

    public static boolean getLastEndDelay(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.LAST_END_DELAY, FieldUtil::toBoolean);
    }

    public static Long getParenId(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.PARENT_ID, FieldUtil::toLong);
    }

    public static Long getCreateTime(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.CREATE_TIME, FieldUtil::toLong);
    }

    public static String getPriority(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.PRIORITY, FieldUtil::toString);
    }

    public static Long getDateTypeFieldValue(Map<String, Object> cardProps, String key) {
        return getFieldValue(cardProps, key, FieldUtil::toLong);
    }

    public static List<String> getUserTypeFieldValues(Map<String, Object> cardProps, String key) {
        return getFieldValue(cardProps, key, FieldUtil::toStringList);
    }

    public static Float getActualWorkload(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.ACTUAL_WORKLOAD, FieldUtil::toFloat);
    }

    public static boolean calcLastEndDelay(Map<String, Object> cardProps) {
        if (getCalcIsEnd(cardProps)) {
            Long endDate = getEndDate(cardProps);
            if (endDate > 0 && getLastEndTime(cardProps) > endDate) {
                return true;
            }
        }
        return false;
    }

    public static @Nonnull List<String> toStringList(Object value) {
        if (null == value) {
            return ListUtils.EMPTY_LIST;
        } else if (value instanceof String) {
            return Arrays.asList((String) value);
        } else if (value instanceof Collection) {
            List<String> strings = new ArrayList<>();
            ((Collection) value).forEach(v -> strings.add(FieldUtil.toString(v)));
            return strings;
        } else if (value instanceof String[]) {
            return Stream.of((String[]) value).map(FieldUtil::toString).collect(Collectors.toList());
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
            ((Collection) value).forEach(v -> longs.add(FieldUtil.toLong(v)));
            return longs;
        } else if (value instanceof String[]) {
            return Stream.of((String[]) value).map(FieldUtil::toLong).collect(Collectors.toList());
        } else if (value instanceof Long[]) {
            return Arrays.asList((Long[]) value);
        }
        return Arrays.asList(FieldUtil.toLong(value));
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

    public static boolean checkInOptions(Object value, List<CardField.Option> options) {
        if (null == value) {
            return true;
        }
        return options.stream().anyMatch(o -> o.getKey().equals(value));
    }

    public static boolean checkAllInOptions(Object value, List<CardField.Option> options) {
        if (null == value) {
            return true;
        }
        if (!(value instanceof List)) {
            return false;
        }
        for (Object v : ((List) value)) {
            if (!options.stream().anyMatch(o -> o.getKey().equals(v))) {
                return false;
            }
        }
        return true;
    }

    public static String checkNameAndGetKeyInOptions(Object value, CardField field) {
        if (null == value) {
            return null;
        }
        CardField.Option option = field.getOptions().stream().filter(o -> o.getName().equals(value)).findAny().orElse(null);
        if (option == null) {
            throw new CustomerExcelAnalysisException(HttpStatus.NOT_ACCEPTABLE, field.getName() + "字段值非法！");
        }
        return option.getKey();
    }

    public static List<String> checkAllAndGetKeysInOptions(Object value, CardField field) {
        if (null == value) {
            return null;
        }
        if (!(value instanceof List)) {
            throw new CustomerExcelAnalysisException(HttpStatus.NOT_ACCEPTABLE, field.getName() + "字段值非法！");
        }
        List<String> keys = new ArrayList<>();
        for (Object v : ((List) value)) {
            CardField.Option option = field.getOptions().stream().filter(o -> o.getName().equals(v)).findAny().orElse(null);
            if (option == null) {
                throw new CustomerExcelAnalysisException(HttpStatus.NOT_ACCEPTABLE, field.getName() + "字段值非法！");
            }
            keys.add(option.getKey());
        }
        return keys;
    }

    public static String getValidNameInOptions(Object key, CardField field) {
        if (null == key) {
            return null;
        }
        CardField.Option option = field.getOptions().stream().filter(o -> o.getKey().equals(key)).findAny().orElse(null);
        if (option == null) {
            return null;
        }
        return option.getName();
    }

    public static List<String> getValidNamesInOptions(Object value, CardField field) {
        if (null == value) {
            return null;
        }
        if (!(value instanceof List)) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (Object v : ((List) value)) {
            CardField.Option option = field.getOptions().stream().filter(o -> o.getKey().equals(v)).findAny().orElse(null);
            if (option != null) {
                names.add(option.getName());
            }
        }
        return names;
    }

    private static float FLOAT_COMPARE_PRECISION = 0.0000000001f;

    public static boolean equals(CardField field, Object v1, Object v2) {
        if (isEmptyValue(v1) && isEmptyValue(v2)) {
            return true;
        }
        if (v1 instanceof Collection) {
            return v2 instanceof Collection && ((Collection) v1).containsAll((Collection) v2) && ((Collection) v2).containsAll((Collection) v1);
        }
        if (isEmptyValue(v1)) {
            return isEmptyValue(v2);
        }
        if (isEmptyValue(v2)) {
            return isEmptyValue(v1);
        }
        switch (field.getValueType()) {
            case FLOAT:
                return Math.abs(toFloat(v1) - toFloat(v2)) < FLOAT_COMPARE_PRECISION;
            default:
                return String.valueOf(v1).equals(String.valueOf(v2));
        }

    }

    public static boolean equals(Object v1, Object v2) {
        if (isEmptyValue(v1) && isEmptyValue(v2)) {
            return true;
        }
        if (v1 instanceof Collection) {
            return v2 instanceof Collection && ((Collection) v1).containsAll((Collection) v2) && ((Collection) v2).containsAll((Collection) v1);
        }
        if (isEmptyValue(v1)) {
            return isEmptyValue(v2);
        }
        return String.valueOf(v1).equals(String.valueOf(v2));
    }

    public static Object parse(FieldType.ValueType valueType, Object value) throws CodedException {
        if (null == valueType || null == value) {
            return value;
        }
        try {
            switch (valueType) {
                case LONG:
                case DATE:
                    return toLong(value);
                case LONGS:
                    return value instanceof String ? JSON_MAPPER.readValue(value.toString(), new TypeReference<List<Long>>() {}) : toLongList(value);
                case FLOAT:
                    return toFloat(value);
                case BOOLEAN:
                    return value instanceof String ? BooleanUtils.toBoolean(value.toString()) : toBoolean(value);
                case STRINGS:
                    return value instanceof String ? JSON_MAPPER.readValue(value.toString(), new TypeReference<List<String>>() {}) : toStringList(value);
                default:
                    return value;
            }
        } catch (JsonProcessingException e) {
            log.error("Parse field value exception!", e);
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, e.getMessage());
        }
    }

    private static String SCRIPT_SET_FIELD = "ctx._source[params.field]=params.value";

    public static Script setFieldScript(String field, Object value) {
        Map<String, Object> params = new HashMap<>();
        params.put("field", field);
        params.put("value", value);
        return new Script(
                ScriptType.INLINE,
                Script.DEFAULT_SCRIPT_LANG,
                SCRIPT_SET_FIELD,
                params);
    }

    private static String SCRIPT_SET_L2_FIELD = "ctx._source[params.field][params.childField]=params.value";

    public static Script setL2FieldScript(String field, String childField, Object value) {
        Map<String, Object> params = new HashMap<>();
        params.put("field", field);
        params.put("childField", childField);
        params.put("value", value);
        return new Script(
                ScriptType.INLINE,
                Script.DEFAULT_SCRIPT_LANG,
                SCRIPT_SET_L2_FIELD,
                params);
    }

    public static Script setCardDetailFieldScript(String field, Object value) {
        return setL2FieldScript(CardEvent.CARD_DETAIL, field, value);
    }


    public static Long getLastModifyTime(Map<String, Object> cardProps) {
        return getFieldValue(cardProps, CardField.LAST_MODIFY_TIME, FieldUtil::toLong);
    }
}
