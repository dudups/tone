package com.ezone.ezproject.modules.card.bean;

import com.ezone.ezproject.modules.card.bean.query.BindType;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BatchBindResponse {
    @ApiModelProperty(value = "绑定的资源类型", example = "")
    @NotNull
    private BindType bindType;

    @ApiModelProperty(value = "关连成功的集合")
    private Object successAdd;

    @ApiModelProperty(value = "关连失败的信息，可能部份失败")
    private String errorMsg;
}
