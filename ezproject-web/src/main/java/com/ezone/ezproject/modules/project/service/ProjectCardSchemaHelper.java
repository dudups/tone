package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.exception.ErrorCode;
import com.ezone.ezproject.common.stream.CollectorsV2;
import com.ezone.ezproject.configuration.SysProjectCardSchema;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardFieldFlow;
import com.ezone.ezproject.es.entity.CardFieldValue;
import com.ezone.ezproject.es.entity.CardFieldValueFlow;
import com.ezone.ezproject.es.entity.CardStatus;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.bean.CodeEventFilterConf;
import com.ezone.ezproject.es.entity.enums.FieldType;
import com.ezone.ezproject.es.entity.enums.Source;
import com.ezone.ezproject.external.ci.bean.AutoStatusFlowEventType;
import com.ezone.ezproject.modules.card.bean.ChangeTypeCheckResult;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.field.check.FieldValueCheckHelper;
import com.ezone.ezproject.modules.project.bean.CheckSchemaResult;
import com.ezone.ezproject.modules.project.bean.StringMatchersConflictChecker;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ProjectCardSchemaHelper {
    private static final Pattern CUSTOM_FIELD_KEY_PATTERN = Pattern.compile("^custom_(?<num>[\\d]+)_[\\w]+$");
    private static final Pattern CUSTOM_STATUS_KEY_PATTERN = Pattern.compile("^custom_(?<num>[\\d]+)$");

    public static final int MAX_CUSTOM_FIELDS = 50;
    public static final int MAX_CUSTOM_STATUSES = 50;

    @Getter(lazy = true)
    private final byte[] sysProjectCardSchemaContent = sysProjectCardSchemaContent();

    @Getter(lazy = true)
    private final Map<String, Map<String, Object>> sysProjectCardTemplate = sysProjectCardTemplate();

    @Getter(lazy = true)
    private final ProjectCardSchema sysProjectCardSchema = newSysProjectCardSchema();

    @Getter(lazy = true)
    private final List<String> sysStatusKeys = Collections.unmodifiableList(getSysProjectCardSchema().getStatuses()
            .stream()
            .map(CardStatus::getKey)
            .collect(Collectors.toList()));

    @Getter(lazy = true)
    private final Map<String, CardField> sysFieldMap = Collections.unmodifiableMap(getSysProjectCardSchema().getFields()
            .stream()
            .collect(Collectors.toMap(CardField::getKey, f -> f)));

    @Getter(lazy = true)
    private final List<String> sysFieldKeys = Collections.unmodifiableList(getSysProjectCardSchema().getFields()
            .stream()
            .map(CardField::getKey)
            .collect(Collectors.toList()));

    @Getter(lazy = true)
    private final List<CardField> sysBuildInRequiredFields = Collections.unmodifiableList(getSysProjectCardSchema().getFields()
            .stream()
            .filter(field -> field.getLimit() == CardField.SysFieldLimit.BUILD_IN || field.getLimit() == CardField.SysFieldLimit.REQUIRED)
            .collect(Collectors.toList()));

    @Getter(lazy = true)
    private final List<String> sysBuildInRequiredFieldKeys = getSysBuildInRequiredFields()
            .stream()
            .map(CardField::getKey)
            .collect(Collectors.toList());

    @Getter(lazy = true)
    private final int typesSize = getSysProjectCardSchema().getTypes().size();

    private byte[] sysProjectCardSchemaContent() {
        try {
            return IOUtils.toByteArray(
                    SysProjectCardSchema.class.getResource("/sys-card-schema.yaml"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, Map<String, Object>> sysProjectCardTemplate() {
        try {
            return ProjectCardSchema.YAML_MAPPER.readValue(
                    ProjectCardSchema.class.getResource("/sys-card-template.yaml"),
                    new TypeReference<Map<String, Map<String, Object>>>() {
                    }
            );
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public ProjectCardSchema newSysProjectCardSchema() {
        try {
            return ProjectCardSchema.YAML_MAPPER.readValue(
                    getSysProjectCardSchemaContent(),
                    ProjectCardSchema.class
            );
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param schema
     * @return
     * @throws CodedException
     * @deprecated: 即使是系统内置字段，还可以更改字段描述信息，所以全存
     * Remove sys fields from project-fields;
     * Remove sys fixed-field-keys from card-field-keys;
     * // Remove sys statues from project-statues;
     */
    @Deprecated
    public ProjectCardSchema tripSysSchema(ProjectCardSchema schema) throws CodedException {
//        if (null == schema) {
//            return null;
//        }
//
//        if (CollectionUtils.isNotEmpty(schema.getFields())) {
//            schema.setFields(schema.getFields().stream()
//                    .filter(field -> !getSysFieldKeys().contains(field.getKey()))
//                    .collect(Collectors.toList())
//            );
//        }
//
//        schema.getTypes().forEach(cardType -> {
//            if (CollectionUtils.isNotEmpty(cardType.getFields())) {
//                cardType.setFields(ListUtils.subtract(cardType.getFields(), getSysBuildInFieldKeys()));
//            }
//        });

//        if (CollectionUtils.isNotEmpty(schema.getStatuses())) {
//            schema.setStatuses(schema.getStatuses().stream()
//                    .filter(status -> !getSysStatusKeys().contains(status.getKey()))
//                    .collect(Collectors.toList())
//            );
//        }

        return schema;
    }

    /**
     * Fill sys fields to project-fields;
     * Fill sys fixed-field-keys to card-field-keys;
     * // Fill sys statues to project-statues;
     *
     * @param schema
     * @return
     * @throws CodedException
     */
    public ProjectCardSchema fillSysSchema(ProjectCardSchema schema) throws CodedException {
        if (null == schema) {
            return getSysProjectCardSchema();
        }

        List<CardField> fields = new ArrayList<>();
        if (CollectionUtils.isEmpty(schema.getFields())) {
            fields = getSysProjectCardSchema().getFields();
        } else {
            //合并系统字段，如果是字段类型是SYS，直接使用系统字段。
            Map<String, CardField> systemFieldsMap = getSysProjectCardSchema().getFields().stream().collect(CollectorsV2.toMap(CardField::getKey, Function.identity()));
            for (CardField cardField : schema.getFields()) {
                if (cardField.getSource().equals(Source.SYS)) {
                    CardField systemCardField = systemFieldsMap.get(cardField.getKey());
                    if (systemCardField != null) {
                        fields.add(systemCardField);
                    } else {
                        throw new CodedException(HttpStatus.NOT_ACCEPTABLE, cardField.getKey() + "没有在系统卡片schema中定义。");
                    }
                } else {
                    fields.add(cardField);
                }
            }

            List<String> keys = schema.getFields().stream().map(CardField::getKey).collect(Collectors.toList());
            for (CardField field : getSysProjectCardSchema().getFields()) {
                if (!keys.contains(field.getKey())) {
                    fields.add(field);
                }
            }
        }
        schema.setFields(fields);

        List<CardType> addTypes = new ArrayList<>();
        List<CardType> sysProjectTemplateCardTypes = getSysProjectCardSchema().getTypes();
        Map<String, CardType> sysTemplateCardTypesMap = sysProjectTemplateCardTypes.stream().collect(Collectors.toMap(CardType::getKey, cardType -> cardType));
        schema.getTypes().stream().forEach(cardType -> {
            CardType sysTemplateCardType = sysTemplateCardTypesMap.get(cardType.getKey());
            if (cardType.getInnerType() == null || sysTemplateCardType != null) {
                cardType.setInnerType(sysTemplateCardType.getInnerType());
            }
        });
        if (schema.getTypes().size() < sysProjectTemplateCardTypes.size()) {
            Map<String, Integer> statusIndex = ProjectCardSchemaSettingHelper.getStatusKeyIndexMap(schema);
            List<String> typeKeys = schema.getTypes().stream()
                    .map(cardType -> cardType.getKey())
                    .collect(Collectors.toList());
            sysProjectTemplateCardTypes.forEach(sysCardType -> {
                CardType copyCardType = CardType.builder().build();
                BeanUtils.copyProperties(sysCardType, copyCardType);
                if (!typeKeys.contains(copyCardType.getKey())) {
                    ProjectCardSchemaSettingHelper.resetTypeStatus(statusIndex, copyCardType);
                    addTypes.add(copyCardType);
                }
            });
        }
        schema.getTypes().addAll(addTypes);

        schema.getTypes().forEach(cardType -> {
            List<CardType.FieldConf> fieldConfs = new ArrayList<>();
            List<String> fieldKeys = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(cardType.getFields())) {
                cardType.getFields().forEach(field -> {
                    if (getSysBuildInRequiredFieldKeys().contains(field.getKey())) {
                        field.setEnable(true);
                        fieldKeys.add(field.getKey());
                    }
                    fieldConfs.add(field);
                });
            }
            getSysBuildInRequiredFields().stream()
                    .filter(fieldConf -> !fieldKeys.contains(fieldConf.getKey()))
                    .forEach(field -> fieldConfs.add(CardType.FieldConf.builder().key(field.getKey()).enable(true).build()));
            cardType.setFields(fieldConfs);
        });

        //按系统schema排序
        List<String> typeOrders = getSysProjectCardSchema().getOrderOfTypes();
        schema.setTypes(schema.getTypes().stream().sorted(Comparator.comparingInt(o -> typeOrders.indexOf(o.getKey()))).collect(Collectors.toList()));
//        List<CardStatus> statuses = new ArrayList<>(getSysProjectCardSchema().getStatuses());
//        if (CollectionUtils.isNotEmpty(schema.getStatuses())) {
//            schema.getStatuses().forEach(status -> {
//                if (!getSysStatusKeys().contains(status.getKey())) {
//                    statuses.add(status);
//                }
//            });
//        }
//        schema.setStatuses(statuses);

        return schema;
    }

    public ProjectCardSchema generateCustomFieldKey(ProjectCardSchema schema) throws CodedException {
        if (null == schema) {
            return null;
        }

        List<CardField> fields = schema.getFields();
        if (CollectionUtils.isEmpty(fields)) {
            return schema;
        }
        int[] maxCustomFieldNum = new int[]{0};
        List<CardField> newFields = new ArrayList<>();
        fields.stream()
                .filter(field -> Source.CUSTOM.equals(field.getSource()))
                .forEach(field -> {
                    String key = field.getKey();
                    if (StringUtils.isEmpty(key)) {
                        newFields.add(field);
                    } else {
                        maxCustomFieldNum[0] = Math.max(maxCustomFieldNum[0], parseCustomFieldKeyIndex(key));
                    }
                });
        newFields.forEach(field -> {
            maxCustomFieldNum[0]++;
            if (maxCustomFieldNum[0] > MAX_CUSTOM_FIELDS) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "自定义字段个数过多!");
            }
            field.setKey(String.format("custom_%s_%s", maxCustomFieldNum[0], field.getType().getDefaultValueType().getEsDataType()));
            field.setValueType(field.getType().getDefaultValueType());
        });

        return schema;
    }

    public ProjectCardSchema reGenerateCustomFieldKey(ProjectCardSchema schema) throws CodedException {
        if (null == schema) {
            return null;
        }
        List<CardField> fields = schema.customFields();
        if (CollectionUtils.isEmpty(fields)) {
            return schema;
        }
        if (fields.size() > MAX_CUSTOM_FIELDS) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "卡片自定义字段个数过多!");
        }
        Map<String, String> keyMap = new HashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            CardField field = fields.get(i);
            String fromKey = field.getKey();
            field.setKey(String.format("custom_%s_%s", i + 1, field.getType().getDefaultValueType().getEsDataType()));
            field.setValueType(field.getType().getDefaultValueType());
            keyMap.put(fromKey, field.getKey());
        }
        Function<String, String> toKey = key -> StringUtils.defaultString(keyMap.get(key), key);
        forEach(schema.getTypes(), cardType -> {
            forEach(cardType.getFields(), fieldConf -> fieldConf.setKey(toKey.apply(fieldConf.getKey())));
            forEach(cardType.getStatuses(), statusConf -> {
                forEach(statusConf.getStatusFlows(), statusFlowConf -> {
                    statusFlowConf.setOpUserField(toKey.apply(statusFlowConf.getOpUserField()));
                });
            });
        });
        return schema;
    }

    private <T> void forEach(Collection<T> collection, Consumer<? super T> action) {
        if (collection != null && action != null) {
            for (Iterator<T> it = collection.iterator(); it.hasNext(); ) {
                action.accept(it.next());
            }
        }
    }

    public String newCustomStatusKey(ProjectCardSchema schema) throws CodedException {
        if (null == schema) {
            return null;
        }
        List<CardStatus> statuses = schema.customStatuses();
        if (CollectionUtils.isNotEmpty(statuses)) {
            if (statuses.size() >= MAX_CUSTOM_STATUSES) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "卡片自定义状态过多!");
            }
            List<Integer> indexes = statuses.stream()
                    .map(s -> parseCustomStatusKeyIndex(s.getKey()))
                    .sorted()
                    .collect(Collectors.toList());
            for (int i = 0; i < indexes.size(); i++) {
                Integer index = indexes.get(i);
                if (i + 1 < index) {
                    return String.format("custom_%s", i + 1);
                }
            }
            return String.format("custom_%s", statuses.size() + 1);
        }
        return String.format("custom_%s", 1);
    }

    public ProjectCardSchema generateCustomStatusKey(ProjectCardSchema schema) throws CodedException {
        if (null == schema) {
            return null;
        }

        List<CardStatus> statuses = schema.getStatuses();
        if (CollectionUtils.isEmpty(statuses)) {
            return schema;
        }
        int[] maxCustomStatusNum = new int[]{0};
        List<CardStatus> newStatuses = new ArrayList<>();
        statuses.stream()
                .filter(status -> Source.CUSTOM.equals(status.getSource()))
                .forEach(status -> {
                    String key = status.getKey();
                    if (StringUtils.isEmpty(key)) {
                        newStatuses.add(status);
                    } else {
                        maxCustomStatusNum[0] = Math.max(maxCustomStatusNum[0], parseCustomStatusKeyIndex(status.getKey()));
                    }
                });
        newStatuses.forEach(status -> {
            maxCustomStatusNum[0]++;
            if (maxCustomStatusNum[0] > MAX_CUSTOM_STATUSES) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "卡片自定义状态过多!");
            }
            status.setKey(String.format("custom_%s", maxCustomStatusNum[0]));
        });

        return schema;
    }

    public ProjectCardSchema reGenerateCustomStatusKey(ProjectCardSchema schema) throws CodedException {
        if (null == schema) {
            return null;
        }
        List<CardStatus> statuses = schema.customStatuses();
        if (CollectionUtils.isEmpty(statuses)) {
            return schema;
        }
        if (statuses.size() > MAX_CUSTOM_STATUSES) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "卡片状态过多!");
        }
        Map<String, String> keyMap = new HashMap<>();
        for (int i = 0; i < statuses.size(); i++) {
            CardStatus status = statuses.get(i);
            String fromKey = status.getKey();
            status.setKey(String.format("custom_%s", i + 1));
            keyMap.put(fromKey, status.getKey());
        }
        Function<String, String> toKey = key -> StringUtils.defaultString(keyMap.get(key), key);
        forEach(schema.getTypes(), cardType -> {
            forEach(cardType.getFields(), fieldConf -> {
                forEach(fieldConf.getStatusLimits(), statusLimit -> {
                    statusLimit.setStatus(toKey.apply(statusLimit.getStatus()));
                });
            });
            forEach(cardType.getStatuses(), statusConf -> {
                statusConf.setKey(toKey.apply(statusConf.getKey()));
                forEach(statusConf.getStatusFlows(), statusFlowConf -> {
                    statusFlowConf.setTargetStatus(toKey.apply(statusFlowConf.getTargetStatus()));
                });
            });
        });
        return schema;
    }

    public int parseCustomFieldKeyIndex(String key) {
        Matcher matcher = CUSTOM_FIELD_KEY_PATTERN.matcher(key);
        if (matcher.find()) {
            return NumberUtils.toInt(matcher.group("num"));
        } else {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("非法的自定义字段key:[%s] !", key));
        }
    }

    public int parseCustomStatusKeyIndex(String key) {
        Matcher matcher = CUSTOM_STATUS_KEY_PATTERN.matcher(key);
        if (matcher.find()) {
            return NumberUtils.toInt(matcher.group("num"));
        } else {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("非法的自定义状态key:[%s] !", key));
        }
    }

    /**
     * check points:
     * 1. Card's first status must be 'open' & enabled & not end status;
     * 2. FieldFlows, fields(ignore value) graph can only be trees
     *
     * @param schema
     * @throws CodedException
     */
    public void checkSchema(ProjectCardSchema schema) throws CodedException {
        List<CardType> cardTypes = schema.getTypes();
        if (CollectionUtils.isNotEmpty(cardTypes)) {
            cardTypes.forEach(cardType -> {
                checkSchemaCardStatusConf(cardType.getStatuses());
            });
        }
        checkFieldFlows(schema, schema.getFieldFlows());
    }

    private void checkSchemaCardStatusConf(List<CardType.StatusConf> statuses) {
        if (CollectionUtils.isEmpty(statuses)) {
            throw new CodedException(HttpStatus.BAD_REQUEST, "卡片的状态列表不能为空!");
        }
        CardType.StatusConf firstStatus = statuses.get(0);
        if (!"open".equals(firstStatus.getKey())) {
            throw new CodedException(HttpStatus.BAD_REQUEST, "系统内置的'新建'状态必须是第一个状态");
        } else if (firstStatus.isEnd()) {
            throw new CodedException(HttpStatus.BAD_REQUEST, "系统内置的'新建'状态不可设为结束状态");
        }
    }

    /**
     * 根据卡片schema定义
     * 1. 移除cardDetail的非法字段及其值；否则写入es，最大问题是可能占用将来扩展字段，其取值和schema定义可能冲突；
     * 2. 检查type, status字段有值合法；
     */
    public Map<String, Object> preProcessCardProps(ProjectCardSchema schema, Map<String, Object> cardDetail) {
        String type = FieldUtil.toString(cardDetail.get(CardField.TYPE));
        CardType cardType = schema.findCardType(type);
        if (null == cardType) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "非法的卡片类型!");
        }
        // remove invalid props
        List<String> fields = cardType.getFields().stream().map(f -> f.getKey()).collect(Collectors.toList());
        Iterator<Map.Entry<String, Object>> it = cardDetail.entrySet().iterator();
        while (it.hasNext()) {
            if (!fields.contains(it.next().getKey())) {
                it.remove();
            }
        }
        // check status
        String status = FieldUtil.toString(cardDetail.get(CardField.STATUS));
        if (null == status) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "状态不能为空!");
        } else {
            CardType.StatusConf statusConf = cardType.findStatusConf(status);
            if (null == statusConf) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "非法状态!");
            }
        }
        return cardDetail;
    }

    public void checkChangeCardStatus(ProjectCardSchema schema, Map<String, Object> detail, String user, String status, boolean bpmIsOpen) {
        checkChangeCardStatus(schema, detail, user, status, false, bpmIsOpen);
    }

    public void checkChangeCardStatus(ProjectCardSchema schema, Map<String, Object> detail, String user, String status, boolean approval, boolean bpmIsOpen) {
        Long currentFlowId = FieldUtil.getBpmFlowId(detail);
        if (currentFlowId != null && currentFlowId > 0) {
            throw new CodedException(ErrorCode.IN_BPM_FLOW, "卡片当前正处在审批状态！");
        }
        String type = String.valueOf(detail.get(CardField.TYPE));
        String fromStatus = String.valueOf(detail.get(CardField.STATUS));
        CardType cardType = schema.findCardType(type);
        CardType.StatusConf toStatusConf = cardType.findStatusConf(status);
        if (null == toStatusConf) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "非法状态!");
        }
        if (fromStatus.equals(status)) {
            return;
        }
        CardType.StatusConf fromStatusConf = cardType.findStatusConf(fromStatus);
        if (null == fromStatusConf) {
            return;
        }
        CardType.StatusFlowConf statusFlowConf = cardType.findStatusFlow(fromStatus, status);
        if (null == statusFlowConf) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "非法的状态流转!");
        }
        Long bpmFlowTemplateId = statusFlowConf.getBpmFlowTemplateId();
        if (approval) {
            if (bpmFlowTemplateId == null && bpmFlowTemplateId <= 0) {
                throw new CodedException(ErrorCode.NO_BPM_FLOW, "当前状态流转未绑定审批流!");
            }
        } else {
            if (bpmIsOpen && bpmFlowTemplateId != null && bpmFlowTemplateId > 0) {
                throw new CodedException(ErrorCode.REQUIRED_BPM_FLOW, "状态流转需要审批!");
            }
        }
        String opUserField = statusFlowConf.getOpUserField();
        if (StringUtils.isNotEmpty(opUserField)) {
            Object opUser = detail.get(opUserField);
            final String opUserFieldF = opUserField;
            CardField userField = schema.getFields().stream().filter(f -> f.getKey().equals(opUserFieldF)).findAny().orElse(null);
            if (userField != null) {
                // v1版 容错性：指定的可操作人为空，则视同无此限
                // v2版 修改成不能拖动， 具体见bug卡片（ezOne-4333 工作流设置了用户权限，如果该字段为空时，则所有人皆可拖动）
                if (FieldUtil.isEmptyValue(opUser)) {
                    CardStatus fromCardStatus = schema.findCardStatus(fromStatus);
                    CardStatus toCardStatus = schema.findCardStatus(status);
                    throw new CodedException(HttpStatus.NOT_ACCEPTABLE,
                            String.format("当前卡片状态由[%s]流转到[%s]时设置了[%s]才能操作", fromCardStatus.getName(), toCardStatus.getName(), userField.getName()));
                }
                switch (userField.getType()) {
                    case MEMBERS:
                    case USERS:
                        if (!(opUser instanceof Collection && ((Collection) opUser).contains(user))) {
                            throw new CodedException(HttpStatus.FORBIDDEN,
                                    String.format("根据状态流转设置，需要[%s]操作!", userField.getName()));
                        }
                        break;
                    default:
                        if (!String.valueOf(opUser).equals(user)) {
                            throw new CodedException(HttpStatus.FORBIDDEN,
                                    String.format("根据状态流转设置，需要[%s]操作!", userField.getName()));
                        }
                }
            }
        }
    }

    public CheckSchemaResult checkSchemaForCopy(ProjectCardSchema from, ProjectCardSchema to) {
        CheckSchemaResult result = checkSchemaCommon(from, to);
        result.setCheckCardResults(new HashMap<>());
        Map<String, CardType> toCardMap = to.getTypes().stream().collect(Collectors.toMap(CardType::getKey, Function.identity()));
        from.getTypes().forEach(fromCardType -> {
            CheckSchemaResult.CheckCardResult.CheckCardResultBuilder builder = CheckSchemaResult.CheckCardResult.builder();
            CardType toCardType = toCardMap.get(fromCardType.getKey());
            List<String> toFields = toCardType.getFields().stream()
                    .filter(f -> f.isEnable())
                    .map(f -> f.getKey()).collect(Collectors.toList());
            fromCardType.getFields().stream().filter(f -> f.isEnable()).forEach(fromField -> {
                if (result.getIncompatibleFields().contains(fromField.getKey())) {
                    builder.incompatibleField(fromField.getKey());
                    return;
                }
                String toField = result.getCompatibleFields().get(fromField.getKey());
                if (toField == null) {
                    toField = fromField.getKey();
                }
                if (!toFields.contains(toField)) {
                    builder.disabledField(fromField.getKey());
                }
            });
            List<String> toStatuses = toCardType.getStatuses().stream()
                    .map(CardType.StatusConf::getKey)
                    .collect(Collectors.toList());
            fromCardType.getStatuses().forEach(fromStatus -> {
                if (result.getIncompatibleStatuses().contains(fromStatus.getKey())) {
                    builder.incompatibleStatus(fromStatus.getKey());
                    return;
                }
                String toStatus = result.getCompatibleStatuses().get(fromStatus.getKey());
                if (toStatus == null) {
                    toStatus = fromStatus.getKey();
                }
                if (!toStatuses.contains(toStatus)) {
                    builder.disabledStatus(fromStatus.getKey());
                }
            });
            builder.defaultStatus(toStatuses.get(0));
            result.checkCardResult(fromCardType.getKey(), builder.build());
        });
        return result;
    }

    public List<ChangeTypeCheckResult> checkSchemaForChangeType(ProjectCardSchema schema, Map<String, String> typeMap) {
        Map<String, CardType> types = schema.getTypes().stream().collect(Collectors.toMap(CardType::getKey, Function.identity()));
        return typeMap.entrySet().stream().map(entry -> {
            CardType fromType = types.get(entry.getKey());
            CardType toType = types.get(entry.getValue());
            List<String> toTypeFields = toType.getFields().stream().filter(f -> f.isEnable()).map(f -> f.getKey()).collect(Collectors.toList());
            List<String> toTypeStatuses = toType.getStatuses().stream().map(s -> s.getKey()).collect(Collectors.toList());
            return ChangeTypeCheckResult.builder()
                    .fromType(entry.getKey())
                    .toType(entry.getValue())
                    .disabledFields(fromType.getFields().stream()
                            .filter(f -> f.isEnable() && !toTypeFields.contains(f.getKey()))
                            .map(f -> f.getKey())
                            .collect(Collectors.toList())
                    )
                    .disabledStatuses(fromType.getStatuses().stream()
                            .filter(s -> !toTypeStatuses.contains(s.getKey()))
                            .map(s -> s.getKey())
                            .collect(Collectors.toList())
                    )
                    .defaultStatus(toTypeStatuses.get(0))
                    .build();
        }).collect(Collectors.toList());
    }

    private CheckSchemaResult checkSchemaCommon(ProjectCardSchema from, ProjectCardSchema to) {
        CheckSchemaResult.CheckSchemaResultBuilder result = CheckSchemaResult.builder();
        result.disabledCardTypes(to.getTypes().stream().filter(t -> !t.isEnable()).map(CardType::getKey).collect(Collectors.toList()));
        List<String> incompatibleFields = new ArrayList<>();
        Map<String, String> compatibleFieldsMap = new HashMap<>();
        Map<String, CardField> nameFieldMap4ToSchema = to.getFields().stream()
                .filter(f -> Source.CUSTOM.equals(f.getSource()))
                .collect(Collectors.toMap(CardField::getName, Function.identity()));
        from.getFields().stream().filter(f -> Source.CUSTOM.equals(f.getSource())).forEach(fromField -> {
            CardField toField = nameFieldMap4ToSchema.get(fromField.getName());
            if (null == toField) {
                incompatibleFields.add(fromField.getKey());
                return;
            }
            FieldType.ValueType fromType = fromField.getValueType();
            FieldType.ValueType toType = toField.getValueType();
            switch (fromField.getType()) {
                case SELECT:
                case RADIO:
                case CHECK_BOX:
                    incompatibleFields.add(fromField.getKey());
                    break;
                default:
                    if (fromType == toType) {
                        if (!fromField.getKey().equals(toField.getKey())) {
                            compatibleFieldsMap.put(fromField.getKey(), toField.getKey());
                        }
                    } else {
                        incompatibleFields.add(fromField.getKey());
                    }
            }
        });
        result.incompatibleFields(incompatibleFields);
        result.compatibleFields(compatibleFieldsMap);
        List<String> incompatibleStatuses = new ArrayList<>();
        Map<String, String> compatibleStatusesMap = new HashMap<>();
        Map<String, CardStatus> nameStatusMap4ToSchema = to.getStatuses().stream()
                .collect(Collectors.toMap(CardStatus::getName, Function.identity()));
        from.getStatuses().stream().forEach(fromStatus -> {
            CardStatus toStatus = nameStatusMap4ToSchema.get(fromStatus.getName());
            if (null == toStatus) {
                incompatibleStatuses.add(fromStatus.getKey());
                return;
            }
            if (!fromStatus.getKey().equals(toStatus.getKey())) {
                compatibleStatusesMap.put(fromStatus.getKey(), toStatus.getKey());
            }
        });
        result.incompatibleStatuses(incompatibleStatuses);
        result.compatibleStatuses(compatibleStatusesMap);
        return result.build();
    }

    public void checkAutoStatusFlowConfConflict(List<CardType.AutoStatusFlowConf> autoStatusFlowConfs) {
        if (CollectionUtils.isEmpty(autoStatusFlowConfs)) {
            return;
        }
        Map<AutoStatusFlowEventType, List<CardType.AutoStatusFlowConf>> map = autoStatusFlowConfs.stream()
                .filter(f -> AutoStatusFlowEventType.CODE_BRANCH_FILTER_EVENTS.contains(f.getEventType()))
                .collect(Collectors.groupingBy(f -> f.getEventType()));
        if (MapUtils.isEmpty(map)) {
            return;
        }
        map.entrySet().stream().map(e -> e.getValue()).filter(CollectionUtils::isNotEmpty).forEach(autoStatusFlows -> {
            checkCodeEventFilterConflict(autoStatusFlows.stream()
                    .map(f -> f.getEventFilterConf())
                    .filter(c -> c instanceof CodeEventFilterConf || c == null)
                    .map(c -> (CodeEventFilterConf) c)
                    .collect(Collectors.toList()));
        });
    }

    private void checkCodeEventFilterConflict(List<CodeEventFilterConf> filters) {
        if (filters == null || filters.size() <= 1) {
            return;
        }
        StringMatchersConflictChecker checker = new StringMatchersConflictChecker();
        filters.forEach(filter -> {
            if (filter == null || CodeEventFilterConf.BranchesFilterType.all == filter.getBranchesFilterType()) {
                checker.addPrefix(StringUtils.EMPTY);
            } else if (CollectionUtils.isNotEmpty(filter.getBranchFilters())) {
                filter.getBranchFilters().forEach(f -> {
                    if (CodeEventFilterConf.BranchFilterType.prefix == f.getType()) {
                        checker.addPrefix(f.getBranch());
                    } else {
                        checker.addPrecise(f.getBranch());
                    }
                });
            }
        });
    }

    public Map<String, Long> extractCustomFieldId(ProjectCardSchema schema, List<Query> queries) {
        Map<String, Long> resUtil = new HashMap<>(8);
        if (CollectionUtils.isNotEmpty(queries)) {
            Map<String, CardField> configs = schema.getFields().stream().distinct().collect(Collectors.toMap(CardField::getKey, field -> field));
            queries.forEach(query -> {
                List<String> queryFields = query.fields();
                if (CollectionUtils.size(queryFields) > 0) {
                    for (String queryField : queryFields) {
                        CardField cardField = configs.get(queryField);
                        if (cardField != null && Source.CUSTOM.equals(cardField.getSource())) {
                            resUtil.put(queryField, cardField.getId() == null ? 0 : cardField.getId());
                        }
                    }
                }
            });
        }
        return resUtil;
    }

    private static final boolean FIELD_FLOW_NO_LIMIT = true;

    public void checkFieldFlows(ProjectCardSchema schema, List<CardFieldFlow> fieldFlows) {
        // 产品更新确定仅触发单层联动且仅前端联动，所以拓扑结构允许允许循环和交叉，不需要检查了
        if (FIELD_FLOW_NO_LIMIT) {
            return;
        }
        if (CollectionUtils.isEmpty(fieldFlows)) {
            return;
        }
        Map<String, CardField> fieldMap = schema.getFields().stream().collect(CollectorsV2.toMap(CardField::getKey, Function.identity()));
        FieldValueCheckHelper valueChecker = FieldValueCheckHelper.builder().build();
        Map<String, Set<String>> parentsMap = new HashMap<>();
        Map<String, Set<String>> childrenMap = new HashMap<>();
        for (CardFieldFlow fieldFlow : fieldFlows) {
            String parent = fieldFlow.getFieldKey();
            CardField parentField = fieldMap.get(parent);
            if (parentField == null) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("未找到字段key[%s]", parent));
            }
            for (CardFieldValueFlow flow : fieldFlow.getFlows()) {
                Object parentValue = flow.getFieldValue();
                valueChecker.check(fieldMap.get(parent), parentValue);
                Set<String> children = flow.getTargetFieldValues().stream()
                        .map(target -> {
                            String child = target.getFieldKey();
                            CardField childField = fieldMap.get(child);
                            if (childField == null) {
                                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("未找到字段key[%s]", child));
                            }
                            valueChecker.check(fieldMap.get(child), target.getFieldValue());
                            return target.getFieldKey();
                        })
                        .collect(Collectors.toSet());
                childrenMap.put(parent, children);
                flow.getTargetFieldValues().stream().map(CardFieldValue::getFieldKey).forEach(child -> {
                    Set<String> parents = parentsMap.get(child);
                    if (parents == null) {
                        parents = new HashSet<>();
                        parentsMap.put(child, parents);
                    }
                    parents.add(parent);
                    if (parents.size() > 1) {
                        throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "存在交叉");
                    }
                });
            }
        }
        Map<String, Integer> loopCount = new HashMap<>();

        Set<String> roots = new HashSet<>(childrenMap.keySet());
        roots.removeAll(parentsMap.keySet());

        fieldFlowLoop(roots, childrenMap, loopCount);

        if (!loopCount.keySet().containsAll(childrenMap.keySet())) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "存在循环");
        }
    }

    private void fieldFlowLoop(Set<String> fields, Map<String, Set<String>> childrenMap, Map<String, Integer> loopCount) {
        if (CollectionUtils.isEmpty(fields)) {
            return;
        }
        for (String field : fields) {
            Integer count = loopCount.get(field);
            if (count == null) {
                count = 0;
            }
            count++;
            if (count > 1) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "存在循环");
            }
            loopCount.put(field, count);
            fieldFlowLoop(childrenMap.get(field), childrenMap, loopCount);
        }
    }

}
