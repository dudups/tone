package com.ezone.ezproject.es.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.collections.CollectionUtils;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OperationConfig {
    @ApiModelProperty(value = "是否允许操作，enable为true时对于限制卡片类型的操作(CARD_CREATE和CARD_DELETE)等于允许所有卡片类型")
    private boolean enable;
    @ApiModelProperty(value = "对于限制卡片类型的操作(CARD_CREATE和CARD_DELETE)，enable为false时允许操作的卡片类型")
    private Set<String> cardTypes;

    public boolean hasPermission(String cardType) {
        return enable || (CollectionUtils.isNotEmpty(cardTypes) && cardTypes.contains(cardType));
    }

    public void merge(OperationConfig config) {
        if (enable) {
            return;
        }
        if (config.enable) {
            enable = true;
            return;
        }
        if (CollectionUtils.isEmpty(config.cardTypes)) {
            return;
        }
        if (cardTypes == null) {
            cardTypes = new HashSet<>();
        }
        cardTypes.addAll(config.cardTypes);
    }
}
