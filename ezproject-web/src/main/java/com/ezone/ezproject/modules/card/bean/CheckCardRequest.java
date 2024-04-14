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
public class CheckCardRequest {
    @NotNull
    @ApiModelProperty(value = "项目ID", example = "1")
    Long projectId;

    @NotNull
    @ApiModelProperty(value = "用户名", example = "u1")
    String user;

    @NotNull
    @ApiModelProperty(value = "卡片ids", example = "1")
    List<Long> cardIds;
}
