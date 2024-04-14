package com.ezone.ezproject.modules.portfolio.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditPortfolioRequest {
    @NotNull
    Long parentId;
    @NotNull
    private String name;
    private Date startDate;
    private Date endDate;

}
