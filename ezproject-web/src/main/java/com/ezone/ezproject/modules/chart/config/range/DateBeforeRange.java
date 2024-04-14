package com.ezone.ezproject.modules.chart.config.range;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.joda.time.DateTime;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DateBeforeRange implements DateRange {

    @NotNull
    @Min(1)
    private int days = 1;

    @Override
    public Date start() {
        return DateTime.now().minusDays(days).withTimeAtStartOfDay().toDate();
    }

    @Override
    public Date end() {
        return new Date();
    }
}
