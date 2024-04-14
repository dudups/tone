package com.ezone.ezproject.modules.portfolio.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioChartRequest {

    @ApiModelProperty(value = "标题")
    @NotNull
    @Size(min = 1, max = 32)
    private String title;

    @NotNull
    private Map<String, Object> config;

    @NotNull
    public String getConfigType() {
        return String.valueOf(config.get("type"));
    }
}
