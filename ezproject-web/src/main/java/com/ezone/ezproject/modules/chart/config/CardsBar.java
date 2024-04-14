package com.ezone.ezproject.modules.chart.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;

@Data
@SuperBuilder
@NoArgsConstructor
public class CardsBar implements Chart.Config {
    @NotNull
    private String classifyField;
}
