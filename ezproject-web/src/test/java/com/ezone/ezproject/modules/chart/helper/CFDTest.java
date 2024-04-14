package com.ezone.ezproject.modules.chart.helper;

import org.junit.Assert;
import org.junit.Test;

public class CFDTest {
    @Test
    public void testRun() {
        CFD.CFDResult result = new CFD(new int[][]{
                {3, 0, 0},
                {1, 2, 0},
                {0, 2, 1},
                {0, 0, 3}
        }, 4, 3).run();
        Assert.assertEquals(2.0, result.getCycles()[0][0], 0);
        Assert.assertEquals(1.0, result.getCycles()[1][0], 0);
        Assert.assertEquals(1.5, result.getCycles()[1][1], 0);
        Assert.assertEquals(1.0, result.getCycles()[2][1], 0);
        Assert.assertEquals(3.0, result.getLeads()[0], 0);
        Assert.assertEquals(2.0, result.getLeads()[1], 0);
        Assert.assertEquals(1.0, result.getLeads()[2], 0);
    }
}
