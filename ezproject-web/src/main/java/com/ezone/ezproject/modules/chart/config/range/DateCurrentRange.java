package com.ezone.ezproject.modules.chart.config.range;

import com.ezone.ezproject.modules.chart.config.enums.DateInterval;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.joda.time.DateTime;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DateCurrentRange implements DateRange {
    @NotNull
    private DateInterval interval = DateInterval.WEEK;

    private boolean withFuture;

    @Override
    public Date start() {
        switch (interval) {
            case WEEK:
                return DateTime.now().withTimeAtStartOfDay().dayOfWeek().withMinimumValue().toDate();
            case MONTH:
                return DateTime.now().withTimeAtStartOfDay().dayOfMonth().withMinimumValue().toDate();
            case QUARTER:
                DateTime today = DateTime.now().withTimeAtStartOfDay();
                return today.minusMonths((today.getMonthOfYear() - 1) % 3).dayOfMonth().withMinimumValue().toDate();
            case YEAR:
                return DateTime.now().withTimeAtStartOfDay().dayOfYear().withMinimumValue().toDate();
            case DAY:
            default:
                return DateTime.now().withTimeAtStartOfDay().toDate();
        }
    }

    @Override
    public Date end() {
        if (!withFuture) {
            return DateTime.now().millisOfDay().withMaximumValue().toDate();
        }
        switch (interval) {
            case WEEK:
                return DateTime.now().dayOfWeek().withMaximumValue().millisOfDay().withMaximumValue().toDate();
            case MONTH:
                return DateTime.now().dayOfMonth().withMaximumValue().millisOfDay().withMaximumValue().toDate();
            case QUARTER:
                DateTime today = DateTime.now();
                return today.minusMonths((today.getMonthOfYear() - 1) % 3).plusMonths(2).dayOfMonth().withMaximumValue().millisOfDay().withMaximumValue().toDate();
            case YEAR:
                return DateTime.now().dayOfYear().withMaximumValue().millisOfDay().withMaximumValue().toDate();
            case DAY:
            default:
                return DateTime.now().millisOfDay().withMaximumValue().toDate();
        }
    }
}
