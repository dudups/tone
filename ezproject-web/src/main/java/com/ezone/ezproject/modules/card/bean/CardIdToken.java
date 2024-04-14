package com.ezone.ezproject.modules.card.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardIdToken {
    @ApiModelProperty(value = "卡片id")
    private Long id;
    @ApiModelProperty(value = "卡片accessToken")
    private String token;
}
