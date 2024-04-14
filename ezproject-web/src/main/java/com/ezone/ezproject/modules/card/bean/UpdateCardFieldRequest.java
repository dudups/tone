package com.ezone.ezproject.modules.card.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UpdateCardFieldRequest {
    @ApiModelProperty(value = "field string值，如果是list则转为json字符串")
    private String value;
    ;
    private AtUsersChange atUsersChange;
}
