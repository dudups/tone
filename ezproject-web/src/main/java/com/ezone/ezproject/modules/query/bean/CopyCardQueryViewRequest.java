package com.ezone.ezproject.modules.query.bean;

import com.ezone.ezproject.dal.entity.enums.CardQueryViewType;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CopyCardQueryViewRequest {
    @ApiModelProperty(value = "视图名")
    @NotNull
    @Size(min = 1, max = 32)
    private String name;

    @ApiModelProperty(value = "视图类型")
    @NotNull
    @Size(min = 1)
    private CardQueryViewType type;

    @NotNull
    @Min(1)
    @ApiModelProperty(value = "复制视图")
    private Long copyFromId;
}
