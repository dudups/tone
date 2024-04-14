package com.ezone.ezproject.modules.card.field.check;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.exception.ErrorCode;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.ProjectWorkloadSetting;
import com.ezone.ezproject.modules.card.field.bean.FieldChange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Map;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class ActualWorkloadChecker implements IFieldChecker {
    public static final CodedException REQUIRE_INCR_WORKLOAD = new CodedException(ErrorCode.REQUIRED_BPM_FLOW, "当前已开启登记工时，禁止直接设置实际工时!");

    private ProjectWorkloadSetting workloadSetting;
    @Override
    public void check(Map<String, Object> cardProps) throws CodedException {
        if (isEnableIncrWorkload() && cardProps.containsKey(CardField.ACTUAL_WORKLOAD)) {
            throw REQUIRE_INCR_WORKLOAD;
        }
    }

    public void check(List<FieldChange> fieldChanges) throws CodedException {
        if (isEnableIncrWorkload() && CollectionUtils.isNotEmpty(fieldChanges)
                && fieldChanges.stream().anyMatch(change -> CardField.ACTUAL_WORKLOAD.equals(change.getField().getKey()))) {
            throw REQUIRE_INCR_WORKLOAD;
        }
    }

    private boolean isEnableIncrWorkload() {
        return workloadSetting != null && workloadSetting.isEnableIncrWorkload();
    }
}
