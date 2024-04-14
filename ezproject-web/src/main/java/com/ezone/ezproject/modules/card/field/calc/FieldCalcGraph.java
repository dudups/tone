package com.ezone.ezproject.modules.card.field.calc;

import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.modules.common.OperationContext;
import org.apache.commons.collections.CollectionUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FieldCalcGraph {
    public static final FieldCalcGraph INSTANCE = new FieldCalcGraph(FieldCalc.graph);

    private List<FieldCalc> graph;

    private Map<String, FieldCalc> calcMap;
    private Map<String, Set<String>> calcDownMap;
    private Function<String, Integer> calcDeep;
    private List<FieldCalc> calcDeepSorted;

    private FieldCalcGraph(List<FieldCalc> graph) {
        this.graph = graph;
        init();
    }

    public void calcUpdate(Map<String, Object> cardDetail, Set<String> changedFields, OperationContext context, ProjectCardSchema schema, FieldRefValueLoader refLoader) {
        if (CollectionUtils.isEmpty(changedFields)) {
            return;
        }
        Set<String> downFields = changedFields.stream()
                .map(calcDownMap::get)
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        Set<String> toChangeFields = new HashSet<>(downFields);
        calcInternal(cardDetail, toChangeFields, context, schema, refLoader);
    }

    public void calcCreate(Map<String, Object> cardDetail, OperationContext context, ProjectCardSchema schema, FieldRefValueLoader refLoader) {
        calcDeepSorted.forEach(fieldCalc -> fieldCalc.getCalc().calc(cardDetail, context, schema, refLoader));
    }

    /**
     * 算法：
     *      从待计算字段中，不断挑最上游字段(deep深度最小)出列;
     *      如果计算后值更新，则把其下游字段入列到待计算字段;
     *      递归直到待计算字段集合为空;
     * @param cardDetail
     * @param toChangeFields
     */
    private void calcInternal(Map<String, Object> cardDetail, Set<String> toChangeFields, OperationContext context, ProjectCardSchema schema, FieldRefValueLoader refLoader) {
        if (CollectionUtils.isEmpty(toChangeFields)) {
            return;
        }
        String field = toChangeFields.stream().min(Comparator.comparing(calcDeep)).get();
        toChangeFields.remove(field);
        FieldCalc calc = calcMap.get(field);
        boolean change = calc.getCalc().calc(cardDetail, context, schema, refLoader);
        if (change) {
            Set<String> downFields = calcDownMap.get(field);
            if (CollectionUtils.isNotEmpty(downFields)) {
                toChangeFields.addAll(downFields);
            }
        }
        calcInternal(cardDetail, toChangeFields, context, schema, refLoader);
    }

    private void init() {
        this.calcMap = graph.stream().collect(Collectors.toMap(
                FieldCalc::getField, Function.identity()));
        this.calcDownMap = calcDownMap();
        this.calcDeep = calcDeep();
        this.calcDeepSorted = graph.stream()
                .sorted((f1, f2) -> this.calcDeep.apply(f1.getField()) - this.calcDeep.apply(f2.getField()))
                .collect(Collectors.toList());
    }

    private Map<String, Set<String>> calcDownMap() {
        Map<String, Set<String>> calcDownMap = new HashMap<>();
        for (FieldCalc calc : graph) {
            Set<String> upFields = calc.getUpFields();
            if (CollectionUtils.isEmpty(upFields)) {
                continue;
            }
            for (String upField : upFields) {
                Set<String> downFields = calcDownMap.get(upField);
                if (downFields == null) {
                    downFields = new HashSet<>();
                    calcDownMap.put(upField, downFields);
                }
                downFields.add(calc.getField());
            }
        }
        return calcDownMap;
    }

    /**
     * 字段拓扑关系中的深度计算；
     * 算法只有一个原则：两个字段如果有上下游关系，下游的深度值必须比上游大；
     * 算法：
     *      从calc的up字段挑出root字段，按bfs思路不断向下遍历；
     *      如果到叶子节点或已经设置了比父更高的深度则转头向上回溯，否则设置为父深度+1并继续下探；
     * @return
     */
    private Function<String, Integer> calcDeep() {
        Map<String, Integer> calcDeepMap = new HashMap<>();
        Set<String> upFields = calcDownMap.keySet();
        Set<String> calcFields = graph.stream().map(FieldCalc::getField).collect(Collectors.toSet());
        Collection<String> roots = CollectionUtils.subtract(upFields, calcFields);
        Stack<String> dfs = new Stack<>();
        dfs.addAll(roots);
        while (!dfs.isEmpty()) {
            String field = dfs.pop();
            Integer deep = calcDeepMap.get(field);
            deep = deep == null ? 0 : deep;
            Set<String> children = calcDownMap.get(field);
            if (CollectionUtils.isEmpty(children)) {
                continue;
            }
            for (String child : children) {
                Integer childDeep = calcDeepMap.get(child);
                if (childDeep != null && childDeep > deep) {
                    continue;
                }
                childDeep = deep + 1;
                calcDeepMap.put(child, childDeep);
                dfs.push(child);
            }
        }
        return calcField -> {
            Integer deep = calcDeepMap.get(calcField);
            return deep == null ? 0 : deep;
        };
    }

}
