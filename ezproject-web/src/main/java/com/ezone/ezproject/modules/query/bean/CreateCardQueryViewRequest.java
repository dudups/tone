package com.ezone.ezproject.modules.query.bean;

import com.ezone.ezproject.dal.entity.enums.CardQueryViewType;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
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
public class CreateCardQueryViewRequest {
    @NotNull
    @Min(1)
    @ApiModelProperty(value = "所属项目ID")
    private Long projectId;

    @NotNull
    @Size(min = 1, max = 32)
    @ApiModelProperty(value = "视图名")
    private String name;

    @NotNull
    @ApiModelProperty(value = "视图类型")
    private CardQueryViewType type;

    @NotNull
    @ApiModelProperty(value = "视图请求详情")
    private SearchEsRequest request;
}
