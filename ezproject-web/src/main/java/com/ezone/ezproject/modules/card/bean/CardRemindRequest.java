package com.ezone.ezproject.modules.card.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardRemindRequest {
    @ApiModelProperty(value = "催办说明")
    private String content;
    @ApiModelProperty(value = "用户类型字段列表")
    @NotNull
    @Size(min = 1)
    private List<String> userFields;
}
