package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.validate.Uniq;
import com.ezone.ezproject.es.entity.enums.FieldType;
import com.ezone.ezproject.es.entity.enums.Source;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.field.bean.FieldChange;
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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCardSchema {
    public static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory()
            .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static final String[] TYPES_STATUSES_FIELDS_FOR_UI = Stream
            .of("statuses", "types")
            .flatMap(p -> Stream
                    .of("key", "name", "description")
                    .map(f -> String.format("%s.%s", p, f)))
            .toArray(String[]::new);

    @ApiModelProperty(value = "项目下字段列表，所有类型卡片可用")
    @Singular
    @Uniq(field = "key", ignoreEmpty = true, message = "字段key不能重复设置！")
    private List<CardField> fields;
    @ApiModelProperty(value = "项目下字段联动，所有类型卡片可用")
    @Uniq(field = CardFieldFlow.FIELD_KEY, message = "触发字段不能重复设置！")
    private List<CardFieldFlow> fieldFlows;
    @ApiModelProperty(value = "项目下状态列表，所有类型卡片可用")
    @Singular
    @Uniq(field = "key", ignoreEmpty = true, message = "状态key不能重复设置！")
    private List<CardStatus> statuses;
    @ApiModelProperty(value = "项目下所有卡片类型，以及每种卡片的字段、状态等详细设置")
    @Singular
    @Uniq(field = "key", message = "类型key不能重复设置！")
    private List<CardType> types;

    @ApiModelProperty(value = "项目下所有卡片类型排序设置")
    @Singular
    private List<String> orderOfTypes;

    public static ProjectCardSchema from(String yaml) throws JsonProcessingException {
        return YAML_MAPPER.readValue(yaml, ProjectCardSchema.class);
    }

    public String yaml() throws JsonProcessingException {
        return YAML_MAPPER.writeValueAsString(this);
    }

    @Nullable
    public CardType findCardType(String type) {
        return types.stream().filter(t -> t.getKey().equals(type)).findAny().orElse(null);
    }

    public CardField findCardField(String key) {
        return fields.stream().filter(f -> f.getKey().equals(key)).findAny().orElse(null);
    }

    public CardStatus findCardStatus(String key) {
        return statuses.stream().filter(s -> s.getKey().equals(key)).findAny().orElse(null);
    }

    public String findCardStatusName(String key) {
        CardStatus status = findCardStatus(key);
        if (null == status) {
            return null;
        }
        return status.getName();
    }

    public boolean isEndStatus(String type, String status) {
        CardType cardType = this.findCardType(type);
        if (null == cardType) {
            return false;
        }
        CardType.StatusConf statusConf = cardType.findStatusConf(status);
        return null != statusConf && statusConf.isEnd();
    }

    public boolean isEndStatus(Map<String, Object> cardDetail) {
        CardType cardType = this.findCardType(FieldUtil.toString(cardDetail.get(CardField.TYPE)));
        if (null == cardType) {
            return false;
        }
        CardType.StatusConf statusConf = cardType.findStatusConf(FieldUtil.toString(cardDetail.get(CardField.STATUS)));
        return null != statusConf && statusConf.isEnd();
    }

    /**
     * @return 不保证顺序
     */
    @Nonnull
    public List<String> fieldNames(Collection<String> fieldKeys) {
        if (CollectionUtils.isEmpty(fieldKeys)) {
            return ListUtils.EMPTY_LIST;
        }
        return fields.stream().filter(f -> fieldKeys.contains(f.getKey())).map(CardField::getName).collect(Collectors.toList());
    }

    @Nonnull
    public Map<String, String> fieldKeyNames(Collection<String> fieldKeys) {
        Map<String, String> result = new HashMap<>();
        if (CollectionUtils.isNotEmpty(fieldKeys)) {
            fields.stream().filter(f -> fieldKeys.contains(f.getKey())).forEach(f ->
                    result.put(f.getKey(), f.getName())
            );
        }
        return result;
    }

    public Set<String> customFieldKeys() {
        if (CollectionUtils.isEmpty(fields)) {
            return SetUtils.EMPTY_SET;
        }
        return fields.stream().filter(f -> Source.CUSTOM.equals(f.getSource())).map(CardField::getKey).collect(Collectors.toSet());
    }

    public List<CardField> customFields() {
        if (CollectionUtils.isEmpty(fields)) {
            return ListUtils.EMPTY_LIST;
        }
        return fields.stream().filter(f -> Source.CUSTOM.equals(f.getSource())).collect(Collectors.toList());
    }

    public Set<String> memberFieldKeys(Set<String> excludedFields) {
        if (CollectionUtils.isEmpty(fields)) {
            return SetUtils.EMPTY_SET;
        }
        return fields.stream().filter(f -> {
            boolean isMember;
            if (excludedFields.contains(f.getKey())) {
                isMember = false;
            } else {
                FieldType type = f.getType();
                switch (type) {
                    case USER:
                    case USERS:
                    case MEMBER:
                    case MEMBERS:
                        isMember = true;
                        break;
                    default:
                        isMember = false;
                }
            }
            return isMember;
        }).map(field -> field.getKey()).collect(Collectors.toSet());
    }

    public Set<String> customStatusKeys() {
        if (CollectionUtils.isEmpty(statuses)) {
            return SetUtils.EMPTY_SET;
        }
        return statuses.stream().filter(s -> Source.CUSTOM.equals(s.getSource())).map(s -> s.getKey()).collect(Collectors.toSet());
    }

    public List<CardStatus> customStatuses() {
        if (CollectionUtils.isEmpty(statuses)) {
            return ListUtils.EMPTY_LIST;
        }
        return statuses.stream().filter(s -> Source.CUSTOM.equals(s.getSource())).collect(Collectors.toList());
    }

    public boolean existStatusName(String name) {
        if (CollectionUtils.isEmpty(statuses)) {
            return false;
        }
        return statuses.stream().anyMatch(s -> s.getName().equals(name));
    }

    public boolean existStatusName(String name, String excludeKey) {
        if (CollectionUtils.isEmpty(statuses)) {
            return false;
        }
        return statuses.stream().anyMatch(s -> s.getName().equals(name) && !s.getKey().equals(excludeKey));
    }

    public List<CardField> mergeFields(List<CardField> fields, Boolean isMergeCustomField) {
        Set<String> fromCustomFieldKeys = customFieldKeys();
        List<CardField> validFields = fields.stream()
                .filter(f -> Source.SYS == f.getSource() || StringUtils.isEmpty(f.getKey()) || fromCustomFieldKeys.contains(f.getKey()))
                .collect(Collectors.toList());
        List<CardField> mergeFields = new ArrayList<>(validFields);
        if (isMergeCustomField) {
            Set<String> toCustomFieldKeys = validFields.stream()
                    .filter(f -> StringUtils.startsWith(f.getKey(), "custom_"))
                    .map(CardField::getKey)
                    .collect(Collectors.toSet());

            List<CardField> retainCustomFields = this.fields.stream()
                    .filter(f -> StringUtils.startsWith(f.getKey(), "custom_"))
                    .filter(f -> !toCustomFieldKeys.contains(f.getKey()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(retainCustomFields)) {
                mergeFields.addAll(retainCustomFields);
            }
        }
        if (mergeFields.size() > mergeFields.stream().map(f -> f.getName()).distinct().count()) {
            throw new CodedException(HttpStatus.CONFLICT, "字段名冲突！");
        }
        return mergeFields;
    }

    public List<CardStatus> mergeStatuses(List<CardStatus> statuses) {
        Set<String> fromCustomStatusKeys = customStatusKeys();
        List<CardStatus> validStatuses = statuses.stream()
                .filter(s -> Source.SYS == s.getSource() || StringUtils.isEmpty(s.getKey()) || fromCustomStatusKeys.contains(s.getKey()))
                .collect(Collectors.toList());
        Set<String> toCustomStatusKeys = validStatuses.stream()
                .filter(f -> StringUtils.startsWith(f.getKey(), "custom_"))
                .map(f -> f.getKey())
                .collect(Collectors.toSet());
        List<CardStatus> retainCustomStatuses = this.statuses.stream()
                .filter(f -> StringUtils.startsWith(f.getKey(), "custom_"))
                .filter(f -> !toCustomStatusKeys.contains(f.getKey()))
                .collect(Collectors.toList());
        List<CardStatus> mergeStatuses;
        if (CollectionUtils.isEmpty(retainCustomStatuses)) {
            mergeStatuses = validStatuses;
        } else {
            mergeStatuses = new ArrayList<>(retainCustomStatuses);
            mergeStatuses.addAll(validStatuses);
        }
        if (mergeStatuses.size() > mergeStatuses.stream().map(s -> s.getName()).distinct().count()) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "状态名冲突！");
        }
        return mergeStatuses;
    }

    public List<CardType> enabledCardTypes() {
        return types.stream().filter(CardType::isEnable).collect(Collectors.toList());

    }

    public List<String> enabledCardTypeKeys() {
        return types.stream().filter(CardType::isEnable).map(CardType::getKey).collect(Collectors.toList());
    }

    /**
     * fieldChanges不能包含type/status
     *
     * @param type
     * @param status
     * @param fieldChanges
     */
    public void checkFieldChange(String type, String status, List<FieldChange> fieldChanges) {
        if (CollectionUtils.isEmpty(fieldChanges)) {
            return;
        }
        CardType cardType = findCardType(type);
        if (cardType == null) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("卡片类型[%s]不存在！", type));
        }
        fieldChanges.forEach(fieldChange -> {
            CardType.FieldConf fieldConf = cardType.findFieldConf(fieldChange.getField().getKey());
            if (fieldConf == null) {
                return;
            }
            if (!fieldConf.isEnable()) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("字段[%s]未启用！", fieldChange.getField().getName()));
            }
            CardType.FieldLimit limit = fieldConf.findStatusLimit(status);
            if (limit == null) {
                return;
            }
            switch (limit) {
                case HIDE:
                case READ_ONLY:
                    throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("字段[%s]只读或隐藏！", fieldChange.getField().getName()));
                case REQUIRED:
                    if (FieldUtil.isEmptyValue(fieldChange.getToValue())) {
                        throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("字段[%s]必填！", fieldChange.getField().getName()));
                    }
            }
        });
    }

    private static final boolean FIELD_FLOW_UI_ONLY = true;

    public List<FieldChange> mergeFlowFieldChange(Map<String, Object> cardDetail, String type, String status, List<FieldChange> fieldChanges) {
        // 产品更新确定仅触发单层联动且仅前端联动，故不需要。暂时通过开关关闭，观察看后续是否会有用户提后端触发字段联动
        if (FIELD_FLOW_UI_ONLY) {
            return fieldChanges;
        }
        if (CollectionUtils.isEmpty(fieldChanges) || CollectionUtils.isEmpty(fieldFlows)) {
            return fieldChanges;
        }
        CardType cardType = findCardType(type);
        if (cardType == null) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("卡片类型[%s]不存在！", type));
        }
        Map<CardFieldValue, List<CardFieldValue>> fieldFlowMap = new HashMap<>();
        for (CardFieldFlow fieldFlow : fieldFlows) {
            for (CardFieldValueFlow flow : fieldFlow.getFlows()) {
                fieldFlowMap.put(
                        CardFieldValue.builder()
                                .fieldKey(fieldFlow.getFieldKey())
                                .fieldValue(flow.getFieldValue())
                                .build(),
                        flow.getTargetFieldValues());
            }
        }
        return FieldChangeFlowMerge.builder()
                .schema(this)
                .cardDetail(cardDetail)
                .type(cardType)
                .status(status)
                .fieldChanges(fieldChanges)
                .fieldFlowMap(fieldFlowMap)
                .build()
                .merge();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class FieldChangeFlowMerge {
        private ProjectCardSchema schema;
        private Map<String, Object> cardDetail;
        private CardType type;
        private String status;
        private List<FieldChange> fieldChanges;
        private Map<CardFieldValue, List<CardFieldValue>> fieldFlowMap;

        public List<FieldChange> merge() {
            List<FieldChange> mergedChanges = new ArrayList<>(fieldChanges);
            Set<String> changedFields = fieldChanges.stream().map(c -> c.getField().getKey()).collect(Collectors.toSet());
            fieldChanges.forEach(fieldChange -> flowField(
                    CardFieldValue.builder()
                            .fieldKey(fieldChange.getField().getKey())
                            .fieldValue(fieldChange.getToValue())
                            .build(),
                    mergedChanges,
                    changedFields));
            return mergedChanges;
        }

        private void flowField(CardFieldValue fieldValue, List<FieldChange> mergedChanges, Set<String> changedFields) {
            List<CardFieldValue> targets = fieldFlowMap.get(fieldValue);
            if (CollectionUtils.isEmpty(targets)) {
                return;
            }
            for (CardFieldValue target : targets) {
                if (changedFields.contains(target.getFieldKey())) {
                    continue;
                }
                Object fromValue = cardDetail.get(target.getFieldKey());
                if (FieldUtil.equals(schema.findCardField(target.getFieldKey()), target.getFieldValue(), fromValue)) {
                    continue;
                }
                boolean isTargetValueValid = true;
                CardType.FieldLimit limit = null;
                CardType.FieldConf fieldConf = type.findFieldConf(target.getFieldKey());
                if (fieldConf != null && fieldConf.isEnable()) {
                    limit = fieldConf.findStatusLimit(status);
                }
                if (limit != null) {
                    switch (limit) {
                        case HIDE:
                        case READ_ONLY:
                            isTargetValueValid = false;
                            break;
                        case REQUIRED:
                            if (FieldUtil.isEmptyValue(target.getFieldValue())) {
                                isTargetValueValid = false;
                            }
                            break;
                    }
                }
                if (!isTargetValueValid) {
                    continue;
                }
                mergedChanges.add(FieldChange.builder()
                        .field(schema.findCardField(target.getFieldKey()))
                        .fromValue(fromValue)
                        .toValue(target.getFieldValue())
                        .build());
                changedFields.add(target.getFieldKey());
                flowField(target, mergedChanges, changedFields);
            }
        }
    }
}
