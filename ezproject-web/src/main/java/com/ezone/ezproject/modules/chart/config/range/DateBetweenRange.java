package com.ezone.ezproject.modules.chart.config.range;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DateBetweenRange implements DateRange {
    @NotNull
    private Date start;
    @NotNull
    private Date end;

    @Override
    public Date start() {
        return start;
    }

    @Override
    public Date end() {
        return end;
    }
}
