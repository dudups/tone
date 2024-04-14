package com.ezone.ezproject.modules.card.field.check;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.exception.ErrorCode;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class FieldStatusLimitHelp {
    private ProjectCardSchema schema;
    private String fieldKey;


    public void check(@Nullable Map<String, Object> fromCardDetail, @NotNull Map<String, Object> toCardDetail) {
        CardType cardType = schema.findCardType(FieldUtil.getType(toCardDetail));
        if (cardType == null) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("卡片类型[%s]不存在！", FieldUtil.getType(toCardDetail)));
        }
        Optional<CardType.FieldConf> configOptional = cardType.getFields().stream()
                .filter(field -> field.getKey().equals(this.fieldKey))
                .findFirst();
        if (!configOptional.isPresent()) {
            return;
        }
        CardType.FieldConf fieldConf = configOptional.get();
        List<CardType.FieldStatusLimit> statusLimits = fieldConf.getStatusLimits();
        if (CollectionUtils.isEmpty(statusLimits)) {
            return;
        }
        String toStatus = FieldUtil.getStatus(toCardDetail);
        Optional<CardType.FieldStatusLimit> toStatusLimit =
                statusLimits.stream().filter(limit -> toStatus.equals(limit.getStatus())).findFirst();
        if (toStatusLimit.isPresent()) {
            CardType.FieldStatusLimit fieldStatusLimit = toStatusLimit.get();
            CardType.FieldLimit limit = fieldStatusLimit.getLimit();
            switch (limit) {
                case READ_ONLY:
                    readOnlyCheck(fromCardDetail, toCardDetail);
                    break;
                case REQUIRED:
                    requiredCheck(fromCardDetail, toCardDetail, cardType);
                    break;
                default:
            }
        }
    }

    public void readOnlyCheck(@Nullable Map<String, Object> fromCardDetail, Map<String, Object> cardDetail) throws CodedException {
        if (MapUtils.isEmpty(fromCardDetail)) {
            return;
        }
        String fromStatus= FieldUtil.getStatus(fromCardDetail);
        String toStatus  = FieldUtil.getStatus(cardDetail);
        Object fromValue = fromCardDetail.get(this.fieldKey);
        Object toValue = cardDetail.get(this.fieldKey);
        if (Objects.equals(fromValue, toValue)) {
            return;
        }
        //只有到达指定状态后，才校验只读
        if (!StringUtils.equals(fromStatus, toStatus)) {
            return;
        }
        throw new CodedException(ErrorCode.REQUIRED_FIELDS, String.format("只读字段:[%s]", schema.findCardField(this.fieldKey).getName()));
    }

    private void requiredCheck(@Nullable Map<String, Object> fromCardDetail, @NotNull Map<String, Object> cardDetail,
                               CardType cardType) throws CodedException {
        String toStatus = FieldUtil.getStatus(cardDetail);
        boolean check;
        if (MapUtils.isEmpty(fromCardDetail)) {
            check = true;
        } else {
            String fromStatus = FieldUtil.getStatus(fromCardDetail);
            //状态转换同时前后状态都配置为只读时，忽略必填
            CardType.StatusConf fromStatusConfig = cardType.findStatusConf(fromStatus);
            CardType.StatusConf toStatusConfig = cardType.findStatusConf(toStatus);
            boolean readOnlyToReadOnly = fromStatusConfig.isReadOnly() && toStatusConfig.isReadOnly();
            boolean jumpCheck = !fromStatus.equals(toStatus) && readOnlyToReadOnly;
            check = !jumpCheck;
        }
        if (check && FieldUtil.isEmptyValue(cardDetail.get(fieldKey))) {
            List<String> lackFields = cardType.findRequiredFields(toStatus).stream()
                    .filter(f -> FieldUtil.isEmptyValue(cardDetail.get(f)))
                    .collect(Collectors.toList());
            throw new CodedException(ErrorCode.REQUIRED_FIELDS,
                    String.format("必填字段:[%s]", StringUtils.join(schema.fieldNames(lackFields), ",")), lackFields);
        }
    }
}
