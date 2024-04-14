package com.ezone.ezproject.modules.portfolio.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePortfolioRequest {
    @NotNull
    Long parentId;
    @NotNull
    PortfolioBean portfolio;

    @Data
    public static class PortfolioBean {
        @NotNull
        private String name;
        private Date startDate;
        private Date endDate;
        List<PortfolioBean> subPortfolios;
    }
}
