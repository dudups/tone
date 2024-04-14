package com.ezone.ezproject.modules.chart.config.range;

import com.ezone.ezproject.modules.chart.config.enums.DateInterval;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "dateBeforeRange", value = DateBeforeRange.class),
        @JsonSubTypes.Type(name = "dateBetweenRange", value = DateBetweenRange.class),
        @JsonSubTypes.Type(name = "dateCurrentRange", value = DateCurrentRange.class)
})
public interface DateRange {
    public static final Class[] RANGE_CLASSES = {
            DateBeforeRange.class,
            DateBetweenRange.class,
            DateCurrentRange.class
    };

    public static final int DEFAULT_SPANS_LIMIT = 100;

    public static List<TimeSpan> timeSpans(DateRange range, DateInterval interval, int preAdd, int endAdd) {
        return timeSpans(range, DEFAULT_SPANS_LIMIT, interval, preAdd, endAdd);
    }

    public static int step(Date start, Date end, int limit, int internalDays) {
        if (limit <= 0) {
            return 1;
        }
        int days = (int) ((end.getTime() - start.getTime()) / (24 * 3600 * 1000));
        int spans = days / internalDays + 1;
        if (spans <= limit) {
            return 1;
        }
        return spans / limit + 1;
    }

    public static List<TimeSpan> timeSpans(DateRange range, int limit, DateInterval interval, int preAdd, int endAdd) {
        if (null == range) {
            return ListUtils.EMPTY_LIST;
        }
        Date rangeStart = range.start();
        Date rangeEnd = range.end();
        Function<Date, Date> startFunc;
        Function<Date, Date> nextFunc;
        final int step;
        switch (interval) {
            case YEAR:
                rangeStart = DateUtils.addYears(rangeStart, preAdd);
                rangeEnd = DateUtils.addYears(rangeEnd, endAdd);
                startFunc = date -> new DateTime(date).dayOfYear().withMinimumValue().toDate();
                step = step(rangeStart, rangeEnd, limit, 365);
                nextFunc = date -> DateUtils.addYears(date, step);
                break;
            case QUARTER:
                rangeStart = DateUtils.addMonths(rangeStart, preAdd * 3);
                rangeEnd = DateUtils.addMonths(rangeEnd, endAdd * 3);
                startFunc = date -> new DateTime(date).minusMonths((new DateTime(date).getMonthOfYear() - 1) % 3).dayOfMonth().withMinimumValue().toDate();
                step = step(rangeStart, rangeEnd, limit, 91);
                nextFunc = date -> DateUtils.addMonths(date, step);
                break;
            case MONTH:
                rangeStart = DateUtils.addMonths(rangeStart, preAdd);
                rangeEnd = DateUtils.addMonths(rangeEnd, endAdd);
                startFunc = date -> new DateTime(date).dayOfMonth().withMinimumValue().toDate();
                step = step(rangeStart, rangeEnd, limit, 30);
                nextFunc = date -> DateUtils.addMonths(date, step);
                break;
            case WEEK:
                rangeStart = DateUtils.addWeeks(rangeStart, preAdd);
                rangeEnd = DateUtils.addWeeks(rangeEnd, endAdd);
                startFunc = date -> new DateTime(date).dayOfWeek().withMinimumValue().toDate();
                step = step(rangeStart, rangeEnd, limit, 7);
                nextFunc = date -> DateUtils.addWeeks(date, step);
                break;
            case DAY:
            default:
                rangeStart = DateUtils.addDays(rangeStart, preAdd);
                rangeEnd = DateUtils.addDays(rangeEnd, endAdd);
                startFunc = date -> date;
                step = step(rangeStart, rangeEnd, limit, 1);
                nextFunc = date -> DateUtils.addDays(date, step);
        }

        List<TimeSpan> timeSpans = new ArrayList<>();
        Date spanStart = startFunc.apply(rangeStart);
        Date spanEnd = nextFunc.apply(spanStart);
        if (spanEnd.after(rangeEnd)) {
            spanEnd = rangeEnd;
        }
        do {
            timeSpans.add(TimeSpan.builder().start(spanStart).end(spanEnd).build());
            spanStart = spanEnd;
            spanEnd = nextFunc.apply(spanEnd);
        } while (spanStart.before(rangeEnd));
        return timeSpans;
    }

    public static List<TimeSpan> timeSpans(DateRange range, DateInterval interval) {
        return timeSpans(range, interval, 0, 0);
    }

    public static List<TimeSpan> timeSpans(DateRange range, int limit, DateInterval interval) {
        return timeSpans(range, limit, interval, 0, 0);
    }

    Date start();

    /**
     * 不包含
     *
     * @return
     */
    Date end();

    default List<TimeSpan> timeSpans(DateInterval interval) {
        return timeSpans(this, interval);
    }

    default List<TimeSpan> timeSpans(int limit, DateInterval interval) {
        return timeSpans(this, limit, interval);
    }

    /**
     *
     * @param interval
     * @param preAdd 起点增减，正数表示往后的日期，负数表示之前的日期
     * @param endAdd 终点增减，正数表示往后的日期，负数表示之前的日期
     * @return
     */
    default List<TimeSpan> timeSpans(DateInterval interval, int preAdd, int endAdd) {
        return timeSpans(this, interval, preAdd, endAdd);
    }

    default List<TimeSpan> timeSpans(int limit, DateInterval interval, int preAdd, int endAdd) {
        return timeSpans(this, limit, interval, preAdd, endAdd);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class TimeSpan {
        private String name;
        private Date start;
        private Date end;

        public String getStartAsString() {
            if (null == start) {
                return null;
            }
            return DateFormatUtils.format(start, "yyyy-MM-dd");
        }
    }

}
