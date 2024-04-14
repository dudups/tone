package com.ezone.ezproject.modules.chart.bean;

import com.ezone.ezproject.modules.chart.enums.ChartGroupType;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChartGroupRequest {
    @ApiModelProperty(value = "标题")
    @NotNull
    @Size(min = 1, max = 32)
    private String title;

    @NotNull
    private ChartGroupType type;
}
