package com.ezone.ezproject.modules.chart.helper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

/**
 * 状态累积流图，计算横向的cycleTime(当前到下一状态)和leadTime(开始到结束状态)
 */
public class CFD {
    // 保留一位小数
    private static final Function<Double, Double> ROUND_DOUBLE_DECIMAL =
            d -> new BigDecimal(d).setScale(1, RoundingMode.HALF_EVEN).doubleValue();

    // values: count(时间，状态)
    private int[][] values;
    private int xLen;
    private int yLen;
    private int leadFromYIndex;
    private int leadToYIndex;
    private int step;

    public CFD(int[][] values, int xLen, int yLen) {
        this(values, xLen, yLen, 0, yLen - 1);
    }

    public CFD(@NonNull int[][] values, int xLen, int yLen, int leadFromYIndex, int leadToYIndex) {
        this(values, xLen, yLen, leadFromYIndex, leadToYIndex, 1);
    }

    public CFD(@NonNull int[][] values, int xLen, int yLen, int leadFromYIndex, int leadToYIndex, int step) {
        Assert.isTrue(leadToYIndex >= leadFromYIndex, "leadToYIndex must >= leadFromYIndex");
        this.values = values;
        this.xLen = xLen;
        this.yLen = yLen;
        this.leadFromYIndex = leadFromYIndex;
        this.leadToYIndex = leadToYIndex;
        this.step = step;
    }

    public CFDResult run() {
        // 累积值
        int[][] cumulativeValues = new int[xLen][yLen];
        for (int x = 0; x < xLen; x++) {
            int cumulativeValue = 0;
            for (int y = yLen - 1; y >= 0; y--) {
                cumulativeValue += values[x][y];
                cumulativeValues[x][y] = cumulativeValue;
            }
        }

        Double[][] cycles = new Double[xLen][yLen];
        Double[] leads = new Double[xLen];
        // 根据累积图随x时间递增&随y状态累计递增的特性，可以缓存并从上次x开始计算，以此优化时间复杂度
        int[] cycleAtXIndexes = new int[yLen];
        int leadAtXIndex = 0;
        for (int x = 0; x < xLen; x++) {
            for (int y = yLen - 1; y >= 0; y--) {
                // cycle
                if (y >= yLen - 1 || values[x][y] == 0) {
                    cycles[x][y] = null;
                } else {
                    int value = cumulativeValues[x][y];
                    int cx = Math.max(cycleAtXIndexes[y], 1);
                    // find cx(cycleAtXIndex) where [cx][y+1] >= [x][y]
                    boolean find = false;
                    for (; cx >= 1 && cx < xLen; cx ++) {
                        if (cumulativeValues[cx][y + 1] >= value) {
                            find = true;
                            cycleAtXIndexes[y] = cx;
                            cycles[x][y] = ROUND_DOUBLE_DECIMAL.apply(step * (cx - 1 - x
                                    + (double) (value - cumulativeValues[cx - 1][y + 1]) / (cumulativeValues[cx][y + 1] - cumulativeValues[cx - 1][y + 1])));
                            break;
                        }
                    }
                    if (!find) {
                        cycleAtXIndexes[y] = cx;
                        cycles[x][y] = null;
                    }
                }
            }
            // lead
            int value = cumulativeValues[x][leadFromYIndex];
            boolean find = false;
            if (value > cumulativeValues[x][leadToYIndex]) {
                int lx = Math.max(leadAtXIndex, cycleAtXIndexes[leadFromYIndex]);
                // find lx(leadAtXIndex) where [lx][to] >= [x][from]
                for (; lx >= 1 && lx < xLen; lx ++) {
                    if (cumulativeValues[lx][leadToYIndex] >= value) {
                        if (lx != x) {
                            find = true;
                            leadAtXIndex = lx;
                            leads[x] = ROUND_DOUBLE_DECIMAL.apply(step * (lx - 1 - x
                                    + (double) (value - cumulativeValues[lx - 1][leadToYIndex]) / (cumulativeValues[lx][leadToYIndex] - cumulativeValues[lx - 1][leadToYIndex])));
                            break;
                        }
                    }
                }
            }
            if (!find) {
                leads[x] = null;
            }
        }
        return CFDResult.builder().cycles(cycles).leads(leads).build();
    }

    @Builder
    @AllArgsConstructor
    @Getter
    public static class CFDResult {
        private Double[][] cycles;
        private Double[] leads;
    }
}
