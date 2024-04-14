package com.ezone.ezproject.modules.card.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class StatusBatchUpdateRequest {
    @NotNull
    @ApiModelProperty(value = "状态修改请求")
    private List<TypeCards> typeCards;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class TypeCards {
        @ApiModelProperty(value = "卡片类型")
        private String cardType;
        @ApiModelProperty(value = "要修改状态的卡片Id")
        @NotNull
        private List<Long> cardIds;
        @ApiModelProperty(value = "目标状态")
        private String status;
    }

}

