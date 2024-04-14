package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.stream.CollectorsV2;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardFieldFlow;
import com.ezone.ezproject.es.entity.CardFieldValue;
import com.ezone.ezproject.es.entity.CardFieldValueFlow;
import com.ezone.ezproject.es.entity.CardStatus;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.enums.Source;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.common.OperationContext;
import com.ezone.ezproject.modules.log.service.OperationLogCmdService;
import com.ezone.ezproject.modules.project.bean.CardStatusesConf;
import com.ezone.ezproject.modules.project.bean.CardTypeConf;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@AllArgsConstructor
@Slf4j
public class ProjectCardSchemaSettingHelper {
    private ProjectCardSchemaHelper schemaHelper;
    private OperationLogCmdService operationLogCmdService;
    private UserService userService;
    private CompanyService companyService;

    public ProjectCardSchema setTypes(Long projectId, ProjectCardSchema schema, List<CardTypeConf> cardTypeConfs) {
        Map<String, CardTypeConf> cardTypeConfMap = cardTypeConfs.stream().collect(
                CollectorsV2.toMap(CardTypeConf::getKey, Function.identity()));
        Map<String, Integer> indexMap = getStatusKeyIndexMap(schema);
        forEach(schema.getTypes(), cardType -> {
            CardTypeConf cardTypeConf = cardTypeConfMap.get(cardType.getKey());
            if (cardTypeConf != null) {
                if (cardType.isEnable() != cardTypeConf.isEnable()) {
                    operationLogCmdService.updateCardTypeEnable(OperationContext.instance(userService.currentUserName()), companyService.currentCompany(), projectId, cardType, cardTypeConf.isEnable());
                }
                cardType.setEnable(cardTypeConf.isEnable());
                cardType.setDescription(cardTypeConf.getDescription());
                cardType.setName(cardTypeConf.getName());
                cardType.setColor(cardTypeConf.getColor());
            }
        });
        return schema;
    }

    public ProjectCardSchema setFieldFlows(ProjectCardSchema schema, List<CardFieldFlow> fieldFlows) {
        if (CollectionUtils.isEmpty(fieldFlows)) {
            schema.setFieldFlows(ListUtils.EMPTY_LIST);
            return schema;
        }
        Set<String> upFields = schema.getFields().stream()
                .map(CardField::getKey)
                .filter(key -> !CardField.FIELD_FLOW_INVALID_UPSTREAM_FIELD_KEYS.contains(key))
                .collect(Collectors.toSet());
        Set<String> downFields = schema.getFields().stream()
                .map(CardField::getKey)
                .filter(key -> !CardField.FIELD_FLOW_INVALID_DOWNSTREAM_FIELD_KEYS.contains(key))
                .collect(Collectors.toSet());
        List<CardFieldFlow> finalFieldFlows = fieldFlows.stream()
                .filter(fieldFlow -> {
                    String fieldKey = fieldFlow.getFieldKey();
                    if (!upFields.contains(fieldKey)) {
                        // 当前策略：自动忽略掉本条联动触发规则，而非报错
                        return false;
                    }
                    List<CardFieldValueFlow> flows = fieldFlow.getFlows();
                    if (CollectionUtils.isEmpty(flows)) {
                        // 当前策略：自动忽略掉本条联动触发规则，而非报错
                        return false;
                    }
                    flows = flows.stream()
                            .filter(flow -> {
                                List<CardFieldValue> targetFieldValues = flow.getTargetFieldValues();
                                if (CollectionUtils.isEmpty(targetFieldValues)) {
                                    return false;
                                }
                                targetFieldValues = targetFieldValues.stream()
                                        // 当前策略：自动忽略掉本条联动触发规则，而非报错
                                        .filter(target -> downFields.contains(target.getFieldKey()))
                                        .collect(Collectors.toList());
                                if (CollectionUtils.isEmpty(targetFieldValues)) {
                                    return false;
                                }
                                flow.setTargetFieldValues(targetFieldValues);
                                return true;
                            })
                            .collect(Collectors.toList());
                    if (CollectionUtils.isEmpty(flows)) {
                        return false;
                    }
                    fieldFlow.setFlows(flows);
                    return true;
                })
                .collect(Collectors.toList());
        schemaHelper.checkFieldFlows(schema, finalFieldFlows);
        schema.setFieldFlows(finalFieldFlows);
        return schema;
    }

    /**
     * 获取schema中中总的状态key及顺序
     *
     * @param schema
     * @return
     */
    public static Map<String, Integer> getStatusKeyIndexMap(ProjectCardSchema schema) {
        Map<String, Integer> indexMap = new HashMap<>();
        List<CardStatus> statuses = schema.getStatuses();
        for (int i = 0; i < statuses.size(); i++) {
            CardStatus status = statuses.get(i);
            indexMap.put(status.getKey(), i);
        }
        return indexMap;
    }

    /**
     * 重新设置cardType
     *
     * @param statusMap schema中总的状态定义中的key及顺序。
     * @param cardType  需要验证的及重设的cardType
     */
    public static void resetTypeStatus(Map<String, Integer> statusMap, CardType cardType) {
        //重设状态
        cardType.setStatuses(cardType.getStatuses().stream()
                .filter(statusConf -> statusMap.containsKey(statusConf.getKey()))
                .sorted(Comparator.comparingInt(statusConf -> statusMap.get(statusConf.getKey())))
                .collect(Collectors.toList()));

        //重设状态下的流程
        Set<String> cardTypeStatus = cardType.getStatuses().stream().map(CardType.StatusConf::getKey).collect(Collectors.toSet());
        cardType.getStatuses().forEach(statusConf -> statusConf.setStatusFlows(statusConf.getStatusFlows().stream()
                .filter(statusFlowConf -> statusMap.containsKey(statusFlowConf.getTargetStatus()) && cardTypeStatus.contains(statusFlowConf.getTargetStatus()))
                .collect(Collectors.toList())));
    }


    public ProjectCardSchema setFields(ProjectCardSchema schema, List<CardField> fields) {
        generateFieldIdForNewField(schema, fields);
        schema.setFields(fields);
        schemaHelper.generateCustomFieldKey(schema);
        Map<String, CardField> sysFieldMap = schemaHelper.getSysFieldMap();
        List<String> fieldKeys = new ArrayList<>();
        forEach(schema.getFields(), f -> {
            fieldKeys.add(f.getKey());
            CardField sysField = sysFieldMap.get(f.getKey());
            if (null == sysField) {
                f.setValueType(f.getType().getDefaultValueType());
                if (CollectionUtils.isNotEmpty(f.getOptions())) {
                    f.getOptions().stream()
                            .filter(option -> StringUtils.isEmpty(option.getKey()))
                            .forEach(option -> option.setKey("option_" + IdUtil.generateId()));
                }
            } else {
                f.setType(sysField.getType());
                f.setValueType(sysField.getValueType());
                f.setOptions(sysField.getOptions());
            }
        });
        List<CardFieldFlow> fieldFlows = schema.getFieldFlows();
        if (CollectionUtils.isNotEmpty(fieldFlows)) {
            fieldFlows = fieldFlows.stream()
                    .filter(f -> fieldKeys.contains(f.getFieldKey()) && CollectionUtils.isNotEmpty(f.getFlows()))
                    .map(f -> {
                        f.setFlows(f.getFlows().stream()
                                .filter(flow -> CollectionUtils.isNotEmpty(flow.getTargetFieldValues()))
                                .map(flow -> {
                                    flow.setTargetFieldValues(flow.getTargetFieldValues().stream()
                                            .filter(t -> fieldKeys.contains(t.getFieldKey()))
                                            .collect(Collectors.toList()));
                                    return flow;
                                })
                                .filter(flow -> CollectionUtils.isNotEmpty(flow.getTargetFieldValues()))
                                .collect(Collectors.toList()));
                        return f;
                    })
                    .collect(Collectors.toList());
            schema.setFieldFlows(fieldFlows);
        }
        forEach(schema.getTypes(), cardType -> {
            filter(cardType.getFields(), f -> fieldKeys.contains(f.getKey()));
            forEach(cardType.getStatuses(), statusConf -> forEach(statusConf.getStatusFlows(), statusFlowConf -> {
                String opUserField = statusFlowConf.getOpUserField();
                if (StringUtils.isNotEmpty(opUserField) && !fieldKeys.contains(opUserField)) {
                    statusFlowConf.setOpUserField(null);
                }
            }));
        });
        return schema;
    }

    private void generateFieldIdForNewField(ProjectCardSchema schema, List<CardField> fields) {
        Map<String, CardField> oldFieldMap = schema.getFields().stream().distinct().collect(CollectorsV2.toMap(field -> field.getKey(), field -> field));
        for (CardField field : fields) {
            CardField cardField = oldFieldMap.get(field.getKey());

            //新增的字段
            if (cardField == null && Source.CUSTOM.equals(field.getSource())) {
                field.setId(IdUtil.generateId());
            }
        }
    }

    @Deprecated
    public ProjectCardSchema setStatuses(ProjectCardSchema schema, CardStatusesConf reqCardStatuses) {
        List<CardStatus> reqStatuses = reqCardStatuses.getStatuses();
        schema.setStatuses(reqStatuses);
        Set<String> reqStatusKeys = reqStatuses.stream().map(CardStatus::getKey).collect(Collectors.toSet());
        schemaHelper.generateCustomStatusKey(schema);
        //cardStatusIndexesMap中，前端只回传已开启卡片的设置，不传关闭的卡片的设置
        Map<String, List<Integer>> cardStatusIndexesMap = reqCardStatuses.getCardStatusIndexesMap();
        forEach(schema.getTypes(), cardType -> {
            Map<String, CardType.StatusConf> statusConfMap = cardType.getStatuses().stream().collect(
                    CollectorsV2.toMap(CardType.StatusConf::getKey, Function.identity()));
            List<Integer> cardStatusIndexes = cardStatusIndexesMap.get(cardType.getKey());
            if (cardType.isEnable()) {
                if (CollectionUtils.isEmpty(cardStatusIndexes)) {
                    throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("卡片类型:[%s]的状态列表为空!", cardType.getKey()));
                }
                cardStatusIndexes = cardStatusIndexes.stream().distinct().collect(Collectors.toList());
                List<String> statusKeys = cardStatusIndexes.stream()
                        .map(index -> reqStatuses.get(index).getKey())
                        .collect(Collectors.toList());
                ListDiff<String> statusDiff = diff(statusConfMap.keySet(), statusKeys);
                cardType.setStatuses(cardStatusIndexes.stream().map(index -> {
                    CardStatus status = reqStatuses.get(index);
                    CardType.StatusConf statusConf = statusConfMap.get(status.getKey());
                    if (null == statusConf) {
                        // 对卡片新加入的合法状态的初始化设置
                        statusConf = CardType.StatusConf.builder()
                                .key(status.getKey())
                                .isEnd(false)
                                .statusFlows(statusKeys.stream()
                                        // 默认可以流转到其它任何状态
                                        .filter(k -> !k.equals(status.getKey()))
                                        .map(k -> CardType.StatusFlowConf.builder().targetStatus(k).build())
                                        .collect(Collectors.toList())
                                )
                                .build();
                    } else {
                        // 对卡片原有状态，检查状态流转设置，确保目标状态都合法
                        List<CardType.StatusFlowConf> statusFlows = new ArrayList<>();
                        forEach(statusConf.getStatusFlows(), flow -> {
                            if (!statusDiff.isRemove(flow.getTargetStatus())) {
                                statusFlows.add(flow);
                            }
                        });
                        forEach(statusDiff.getAdds(), key ->
                                statusFlows.add(CardType.StatusFlowConf.builder().targetStatus(key).build())
                        );
                        statusConf.setStatusFlows(statusFlows);
                    }
                    return statusConf;
                }).collect(Collectors.toList()));

                checkSchemaCardStatusConf(cardType.getStatuses());
                filter(cardType.getAutoStatusFlows(), f -> statusKeys.contains(f.getTargetStatus()));
                // 更新字段在不同状态下的必填/只读等限制性设置
                forEach(cardType.getFields(), f ->
                        filter(f.getStatusLimits(), limit -> statusKeys.contains(limit.getStatus()))
                );
            } else {
                cardType.setStatuses(cardType.getStatuses().stream().filter(statusConf -> {
                    if (!reqStatusKeys.contains(statusConf.getKey())) {
                        return false;
                    } else {
                        statusConf.setStatusFlows(statusConf.getStatusFlows().stream().filter(
                                statusFlowConf -> reqStatusKeys.contains(statusFlowConf.getTargetStatus())
                        ).collect(Collectors.toList()));
                        return true;
                    }
                }).collect(Collectors.toList()));
            }
        });
        return schema;
    }

    public ProjectCardSchema addStatus(ProjectCardSchema schema, CardStatus cardStatus) {
        if (schema.existStatusName(cardStatus.getName())) {
            throw new CodedException(HttpStatus.CONFLICT, "状态命名冲突！");
        }
        cardStatus.setSource(Source.CUSTOM);
        cardStatus.setKey(schemaHelper.newCustomStatusKey(schema));
        schema.getStatuses().add(cardStatus);
        return schema;
    }

    public ProjectCardSchema updateStatus(ProjectCardSchema schema, CardStatus cardStatus) {
        CardStatus status = schema.findCardStatus(cardStatus.getKey());
        if (null == status) {
            throw new CodedException(HttpStatus.NOT_FOUND, "状态不存在！");
        }
        if (schema.existStatusName(cardStatus.getName(), cardStatus.getKey())) {
            throw new CodedException(HttpStatus.CONFLICT, "状态命名冲突！");
        }
        status.setName(cardStatus.getName());
        status.setDescription(cardStatus.getDescription());
        return schema;
    }

    public ProjectCardSchema sortStatuses(ProjectCardSchema schema, List<String> statusKeys) {
        Map<String, Integer> indexMap = new HashMap<>();
        List<CardStatus> statuses = schema.getStatuses();
        for (int i = 0; i < statuses.size(); i++) {
            CardStatus status = statuses.get(i);
            int index = statusKeys.indexOf(status.getKey());
            if (index < 0) {
                index = i;
            }
            indexMap.put(status.getKey(), index);
        }
        statuses = statuses.stream()
                .sorted(Comparator.comparingInt(status -> indexMap.get(status.getKey())))
                .collect(Collectors.toList());
        schema.setStatuses(statuses);
        forEach(schema.getTypes(), cardType -> {
            cardType.setStatuses(cardType.getStatuses().stream()
                    .filter(statusConf -> indexMap.containsKey(statusConf.getKey()))
                    .sorted(Comparator.comparingInt(statusConf -> indexMap.get(statusConf.getKey())))
                    .collect(Collectors.toList()));
        });
        return schema;
    }

    public ProjectCardSchema enableStatus(ProjectCardSchema schema, String cardTypeKey, String cardStatusKey) {
        CardType cardType = schema.findCardType(cardTypeKey);
        if (null == cardType) {
            throw new CodedException(HttpStatus.NOT_FOUND, "卡片类型不存在！");
        }
        if (null == schema.findCardStatus(cardStatusKey)) {
            throw new CodedException(HttpStatus.NOT_FOUND, "状态不存在！");
        }

        if (null != cardType.findStatusConf(cardStatusKey)) {
            return schema;
        }

        List<CardType.StatusConf> statusConfs = cardType.getStatuses();
        Map<String, CardType.StatusConf> statusConfMap = statusConfs.stream().collect(CollectorsV2.toMap(conf -> conf.getKey(), conf -> conf));
        List<CardStatus> statuses = schema.getStatuses();
        Map<String, Integer> indexMap = IntStream.range(0, statuses.size()).mapToObj(i -> i).collect(CollectorsV2.toMap(i -> statuses.get(i).getKey(), i -> i));
        //默认单向流动，且不能跨状态流转
        String preStatusConfKey = null;
        String nextStatusConfKey = null;
        boolean findCurrent = false;
        for (CardStatus cardStatus : statuses) {
            if (statusConfMap.containsKey(cardStatus.getKey())) {
                if (!findCurrent) {
                    preStatusConfKey = cardStatus.getKey();
                }
                if (findCurrent) {
                    nextStatusConfKey = cardStatus.getKey();
                    break;
                }
            }
            if (cardStatus.getKey().equals(cardStatusKey)) {
                findCurrent = true;
            }
        }

        //设置前一个流向当前开启的状态
        if (preStatusConfKey != null) {
            CardType.StatusConf preStatusConf = statusConfMap.get(preStatusConfKey);
            Set<String> targetStatusSet = preStatusConf.getStatusFlows().stream().map(CardType.StatusFlowConf::getTargetStatus).collect(Collectors.toSet());
            if (!targetStatusSet.contains(cardStatusKey)) {
                preStatusConf.getStatusFlows()
                        .add(CardType.StatusFlowConf.builder().targetStatus(cardStatusKey).build());
            }
        }

        //设置当前开启的状态，并设置流转到下一个状态
        CardType.StatusConf.StatusConfBuilder statusConfBuilder = CardType.StatusConf.builder()
                .key(cardStatusKey);
        if (nextStatusConfKey != null) {
            statusConfBuilder.isEnd(false);
            statusConfBuilder.statusFlows(
                    Arrays.asList(CardType.StatusFlowConf.builder().targetStatus(nextStatusConfKey).build()));
        } else {
            statusConfBuilder.isEnd(true);
        }
        statusConfs.add(statusConfBuilder.build());

        cardType.setStatuses(statusConfs.stream().sorted(Comparator.comparingInt(s -> indexMap.get(s.getKey()))).collect(Collectors.toList()));
        return schema;
    }

    public ProjectCardSchema disableStatus(ProjectCardSchema schema, String cardTypeKey, String cardStatusKey) {
        CardType cardType = schema.findCardType(cardTypeKey);
        if (null == cardType) {
            throw new CodedException(HttpStatus.NOT_FOUND, "卡片类型不存在！");
        }
        if (null == cardType.findStatusConf(cardStatusKey)) {
            return schema;
        }
        List<CardType.StatusConf> statusConfs = cardType.getStatuses();
        filter(statusConfs, s -> !cardStatusKey.equals(s.getKey()));
        forEach(statusConfs, s -> filter(s.getStatusFlows(), f -> !cardStatusKey.equals(f.getTargetStatus())));
        return schema;
    }

    public ProjectCardSchema deleteStatus(ProjectCardSchema schema, String statusKey) {
        forEach(schema.getTypes(), cardType -> {
            List<CardType.StatusConf> statusConfs = cardType.getStatuses();
            filter(statusConfs, s -> !statusKey.equals(s.getKey()));
            forEach(statusConfs, s -> filter(s.getStatusFlows(), f -> !statusKey.equals(f.getTargetStatus())));
        });
        filter(schema.getStatuses(), s -> !statusKey.equals(s.getKey()));
        return schema;
    }

    private void checkSchemaCardStatusConf(List<CardType.StatusConf> statuses) {
        if (CollectionUtils.isEmpty(statuses)) {
            throw new CodedException(HttpStatus.BAD_REQUEST, "卡片状态列表不能为空!");
        }
        CardType.StatusConf firstStatus = statuses.get(0);
        if (!"open".equals(firstStatus.getKey())) {
            throw new CodedException(HttpStatus.BAD_REQUEST, "系统内置新建状态必须是第一个状态!");
        } else if (firstStatus.isEnd()) {
            throw new CodedException(HttpStatus.BAD_REQUEST, "系统内置新建状态不能设未结束状态!");
        }
    }

    public ProjectCardSchema setFields4Card(ProjectCardSchema schema, String card, List<CardType.FieldConf> fields) {
        List<String> fieldKeys = schema.getFields().stream().map(CardField::getKey).collect(Collectors.toList());
        filter(fields, f -> fieldKeys.contains(f.getKey()));
        List<String> enabledKeys = fields.stream().filter(f -> f.isEnable()).map(f -> f.getKey()).collect(Collectors.toList());
        return setCard(schema, card, cardType -> {
            cardType.setFields(fields);
            forEach(cardType.getStatuses(), statusConf ->
                    forEach(statusConf.getStatusFlows(), flow -> {
                        if (!enabledKeys.contains(flow.getOpUserField())) {
                            flow.setOpUserField(null);
                        }
                    })
            );
            return schema;
        });
    }

    public ProjectCardSchema setStatuses4Card(Long projectId, ProjectCardSchema schema, String card, List<CardType.StatusConf> statuses) {
        List<String> statusKeys = schema.getStatuses().stream().map(CardStatus::getKey).collect(Collectors.toList());
        filter(statuses, s -> statusKeys.contains(s.getKey()));
        forEach(statuses, s -> filter(s.getStatusFlows(), flow -> statusKeys.contains(flow.getTargetStatus())));
        checkSchemaCardStatusConf(statuses);
        return setCard(schema, card, cardType -> {

            Map<String, CardType.StatusConf> oldStatusConf = cardType.getStatuses().stream().collect(CollectorsV2.toMap(CardType.StatusConf::getKey, statusConf -> statusConf));

            //状态流转变更审计 对StatusConf的statusFlows进行判断
            statuses.forEach(statusConf -> {
                List<CardType.StatusFlowConf> oldStatusFlows = oldStatusConf.get(statusConf.getKey()).getStatusFlows();
                Map<String, CardType.StatusFlowConf> oldsFlowConfigMap = oldStatusFlows.stream().collect(CollectorsV2.toMap(flow -> flow.getTargetStatus(), flow -> flow));
                Map<String, CardType.StatusFlowConf> flowConfigMap = statusConf.getStatusFlows().stream().collect(CollectorsV2.toMap(flow -> flow.getTargetStatus(), flow -> flow));
                List<CardType.StatusFlowConf> addFlowConfigs = statusConf.getStatusFlows().stream().filter(flowConfig -> !oldsFlowConfigMap.containsKey(flowConfig.getTargetStatus())).collect(Collectors.toList());
                List<CardType.StatusFlowConf> deleteFlowConfigs = oldStatusFlows.stream().filter(flowConfig -> !flowConfigMap.containsKey(flowConfig.getTargetStatus())).collect(Collectors.toList());
                //添加与删除流程
                OperationContext opContext = OperationContext.instance(userService.currentUserName());
                operationLogCmdService.updateCardTypeWorkFlowNum(opContext, companyService.currentCompany(), projectId, cardType, schema.findCardStatus(statusConf.getKey()), addFlowConfigs, deleteFlowConfigs);
                operationLogCmdService.updateCardTypeStatusIsReadOnly(opContext, companyService.currentCompany(), projectId, cardType, schema.findCardStatus(statusConf.getKey()), statusConf, oldStatusConf.get(statusConf.getKey()));
                statusConf.getStatusFlows().stream().forEach(flowConfig -> {
                    if (oldsFlowConfigMap.containsKey(flowConfig.getTargetStatus())) {
                        CardType.StatusFlowConf oldFlowConf = oldsFlowConfigMap.get(flowConfig.getTargetStatus());
                        if (!StringUtils.equals(flowConfig.getOpUserField(), oldFlowConf.getOpUserField())) {
                            operationLogCmdService.updateCardTypeWorkFlowPermission(opContext, companyService.currentCompany(), projectId, cardType, schema.findCardStatus(statusConf.getKey()).getName(), flowConfig);
                        }
                    }
                });
            });

            cardType.setStatuses(statuses);
            forEach(cardType.getFields(), f -> filter(f.getStatusLimits(), limit -> statusKeys.contains(limit.getStatus())));
            filter(cardType.getAutoStatusFlows(), f -> statusKeys.contains(f.getTargetStatus()));
            return schema;
        });
    }

    public ProjectCardSchema setAutoStatusFlows4Card(Long projectId, Long companyId, ProjectCardSchema schema, String card, List<CardType.AutoStatusFlowConf> autoStatusFlowConfs) {
        List<String> statusKeys = schema.getStatuses().stream().map(CardStatus::getKey).collect(Collectors.toList());
        filter(autoStatusFlowConfs, f -> statusKeys.contains(f.getTargetStatus()));
        return setCard(schema, card, cardType -> {
            schemaHelper.checkAutoStatusFlowConfConflict(autoStatusFlowConfs);
            cardType.setAutoStatusFlows(autoStatusFlowConfs);
            operationLogCmdService.updateCardTypeAutoWorkFlow(OperationContext.instance(userService.currentUserName()), projectId, companyId, cardType);
            return schema;
        });
    }

    private ProjectCardSchema setCard(ProjectCardSchema schema, String card, Function<CardType, ProjectCardSchema> set) {
        Optional<CardType> optional = schema.getTypes().stream().filter(t -> t.getKey().equals(card)).findAny();
        if (optional.isPresent()) {
            set.apply(optional.get());
        }
        return schema;
    }

    private <T> void filter(Collection<T> collection, Function<T, Boolean> filter) {
        if (collection != null && filter != null) {
            for (Iterator<T> it = collection.iterator(); it.hasNext(); ) {
                if (BooleanUtils.isNotTrue(filter.apply(it.next()))) {
                    it.remove();
                }
            }
        }
    }

    private <T> void forEach(Collection<T> collection, Consumer<? super T> action) {
        if (collection != null && action != null) {
            for (Iterator<T> it = collection.iterator(); it.hasNext(); ) {
                action.accept(it.next());
            }
        }
    }

    private <T> ListDiff<T> diff(Collection<T> from, Collection<T> to) {
        List<T> adds = new ArrayList<>();
        List<T> removes = new ArrayList<>();
        forEach(from, f -> {
            if (null == to || !to.contains(f)) {
                removes.add(f);
            }
        });
        forEach(to, t -> {
            if (null == from || !from.contains(t)) {
                adds.add(t);
            }
        });
        return ListDiff.<T>builder().adds(adds).removes(removes).build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    private static class ListDiff<T> {
        private List<T> adds;
        private List<T> removes;

        public boolean isAdd(T t) {
            return null != adds && adds.contains(t);
        }

        public boolean isRemove(T t) {
            return null != removes && removes.contains(t);
        }
    }

}
