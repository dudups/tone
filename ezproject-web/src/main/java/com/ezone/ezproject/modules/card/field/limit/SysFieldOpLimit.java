package com.ezone.ezproject.modules.card.field.limit;

import com.ezone.ezproject.common.resource.BeanLoader;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.enums.Source;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysFieldOpLimit {
    private static final SysFieldOpLimit INSTANCE = BeanLoader.YAML.fromResource("/sys-field-op-limit.yaml", SysFieldOpLimit.class);

    private static final Map<Op, Set<String>> OP_FIELDS = Arrays.stream(Op.values()).collect(Collectors.toMap(
            Function.identity(),
            op -> INSTANCE.limits.stream()
                    .filter(limit -> limit.getOps().contains(op))
                    .map(OpLimit::getField)
                    .collect(Collectors.toSet()))
    );

    private List<OpLimit> limits;

    public static boolean canOp(String fieldKey, Op op) {
        return StringUtils.startsWith(fieldKey, CardField.CUSTOM_PREFIX) || OP_FIELDS.get(op).contains(fieldKey);
    }

    public static boolean canOp(CardField field, Op op) {
        return field.getSource() == Source.CUSTOM || OP_FIELDS.get(op).contains(field.getKey());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpLimit {
        private String field;
        private List<Op> ops;
    }

    public enum Op {
        CREATE, IMPORT, UPDATE, BATCH_UPDATE
    }
}
