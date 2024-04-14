package com.ezone.ezproject.modules.card.update;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectWorkloadSetting;
import com.ezone.ezproject.modules.card.bean.UpdateCardsFieldsRequest;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.field.check.ActualWorkloadChecker;
import com.ezone.ezproject.modules.card.field.limit.SysFieldOpLimit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldOperationsChecker {
    private CompanyCardSchema companyCardSchema;
    private ProjectCardSchema projectCardSchema;
    private ProjectWorkloadSetting workloadSetting;
    private Map<String, List<UpdateCardsFieldsRequest.FieldOperation>> typeFieldOperations;

    public void check() throws CodedException {
        if (MapUtils.isEmpty(typeFieldOperations)) {
            return;
        }
        typeFieldOperations.entrySet().forEach(e -> {
            String type = e.getKey();
            List<UpdateCardsFieldsRequest.FieldOperation> fieldOps = e.getValue();
            if (CollectionUtils.isEmpty(fieldOps)) {
                return;
            }
            CardType cardType = projectCardSchema.findCardType(type);
            if (cardType == null) {
                throw new CodedException(HttpStatus.NOT_FOUND, String.format("卡片类型[%s]不存在！", type));
            }
            String cardTypeName = companyCardSchema.findCardTypeName(type);
            boolean isEnableIncrWorkload = workloadSetting!= null && workloadSetting.isEnableIncrWorkload();
            fieldOps.forEach(fieldOp -> {
                String fieldKey = fieldOp.getField();
                if (isEnableIncrWorkload && CardField.ACTUAL_WORKLOAD.equals(fieldKey)) {
                    throw ActualWorkloadChecker.REQUIRE_INCR_WORKLOAD;
                }
                CardField cardField = projectCardSchema.findCardField(fieldKey);
                if (cardField == null) {
                    throw new CodedException(HttpStatus.NOT_FOUND, String.format("卡片字段[%s]不存在！", fieldKey));
                }
                if (!SysFieldOpLimit.canOp(cardField, SysFieldOpLimit.Op.BATCH_UPDATE)) {
                    throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("不支持批量修改卡片字段[%s]！", cardField.getName()));
                }
                CardType.FieldConf fieldConf = cardType.findFieldConf(fieldKey);
                if (fieldConf == null || !fieldConf.isEnable()) {
                    throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("卡片类型[%s]下未开启卡片字段[%s]！", cardTypeName, cardField.getName()));
                }
                FieldUtil.parse(cardField.getValueType(), fieldOp.getValue());
            });
        });
    }
}
