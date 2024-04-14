package com.ezone.ezproject.modules.card.bean;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
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
public class FilterCardRequest {
    @NotNull
    @ApiModelProperty(value = "公司ID", example = "1")
    Long companyId;

    @NotNull
    @ApiParam(value = "用户名", example = "u1")
    String user;

    @NotNull
    @ApiParam(value = "卡片keys", example = "demo-1")
    List<String> cardKeys;
}
