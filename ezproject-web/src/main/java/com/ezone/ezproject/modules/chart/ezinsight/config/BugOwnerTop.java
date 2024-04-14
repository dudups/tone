package com.ezone.ezproject.modules.chart.ezinsight.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BugOwnerTop extends CardOwnerTop {
    private List<String> bugCardTypes;
    private boolean excludeNoPlan;
}
