package com.ezone.ezproject.modules.chart.config;

import com.ezone.ezproject.modules.chart.config.enums.DateInterval;
import com.ezone.ezproject.modules.chart.config.range.DateCurrentRange;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DateTime.class)
public class DateCurrentRangeTest {

    private String format = "yyyy-MM-dd HH:mm:ss.SSS";

    @Before
    public void before() throws Exception {
        PowerMockito.mockStatic(DateTime.class);
        Mockito.when(DateTime.now()).thenReturn(new DateTime(DateUtils.parseDate("2020-08-09 10:10:10.000", format)));
    }

    @Test
    public void testCurrentWeek() throws Exception {
        DateCurrentRange range = new DateCurrentRange();
        range.setInterval(DateInterval.WEEK);
        Assert.assertEquals(DateUtils.parseDate("2020-08-03 00:00:00.000", format), range.start());
    }

    @Test
    public void testCurrentWeekFuture() throws Exception {
        DateCurrentRange range = new DateCurrentRange();
        range.setInterval(DateInterval.WEEK);
        range.setWithFuture(true);
        Assert.assertEquals(DateUtils.parseDate("2020-08-09 23:59:59.999", format), range.end());
    }

    @Test
    public void testCurrentQuarter() throws Exception {
        DateCurrentRange range = new DateCurrentRange();
        range.setInterval(DateInterval.QUARTER);
        Assert.assertEquals(DateUtils.parseDate("2020-07-01 00:00:00.000", format), range.start());
    }

    @Test
    public void testCurrentQuarterFuture() throws Exception {
        DateCurrentRange range = new DateCurrentRange();
        range.setInterval(DateInterval.QUARTER);
        range.setWithFuture(true);
        Assert.assertEquals(DateUtils.parseDate("2020-09-30 23:59:59.999", format), range.end());
    }
}
