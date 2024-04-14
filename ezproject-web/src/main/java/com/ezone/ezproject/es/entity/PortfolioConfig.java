package com.ezone.ezproject.es.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PortfolioConfig {
    /**
     * 报表是否包含子孙项目集
     */
    private Boolean chartContainDescendant;
}
