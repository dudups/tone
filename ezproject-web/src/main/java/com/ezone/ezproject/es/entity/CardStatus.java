package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.es.entity.enums.Source;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardStatus {
    @ApiModelProperty(value = "状态标识key", example = "open")
    private String key;
    @ApiModelProperty(value = "状态名", example = "新建")
    @NotNull
    @Size(min = 1, max = 32)
    private String name;
    @ApiModelProperty(value = "状态具体描述")
    private String description;
    @NotNull
    @ApiModelProperty(value = "状态来源")
    private Source source;

    public static final String FIRST = "open";
}
