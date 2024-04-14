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
public class GetCardIdsByKeysRequest {
    @NotNull
    @ApiModelProperty(value = "公司ID", example = "1")
    Long companyId;

    @NotNull
    @ApiModelProperty(value = "卡片ids", example = "1")
    List<String> cardKeys;
}
