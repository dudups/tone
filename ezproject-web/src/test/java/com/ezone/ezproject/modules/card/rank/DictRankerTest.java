package com.ezone.ezproject.modules.card.rank;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class DictRankerTest {
    private DictRanker rank = DictRanker.builder()
            .chars("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")
            .build();

    @Test
    public void testDict2Long() {
        Assert.assertEquals(0L, rank.dict2Long("0000", 4));
        Assert.assertEquals(1L, rank.dict2Long("0001", 4));
        Assert.assertEquals(10L, rank.dict2Long("000A", 4));
        Assert.assertEquals(62L, rank.dict2Long("0001", 5));
    }

    @Test
    public void testLong2Dict() {
        Assert.assertEquals("0000", rank.long2Dict(0L, 4, false));
        Assert.assertEquals("0001", rank.long2Dict(1L, 4, false));
        Assert.assertEquals("000A", rank.long2Dict(10L, 4, false));
        Assert.assertEquals("0001", rank.long2Dict(62L, 5, false));
    }

    @Test
    public void testRank() {
        // 正好平均分配的情况必须保证位置平均
        List<String> ranks = rank.ranks("a", "c", 1);
        Assert.assertEquals("b", ranks.get(0));
        ranks = rank.ranks("2", "3", 61);
        Assert.assertEquals("21", ranks.get(0));
        Assert.assertEquals("2z", ranks.get(60));
        ranks = rank.ranks("1", "9", 1);
        Assert.assertEquals("5", ranks.get(0));
        ranks = rank.ranks("1", "7", 2);
        Assert.assertEquals("3", ranks.get(0));
        // 无法均匀分配是的策略，前面n-1个小step，最后距离end空余的step略大>>所以"1"和"4"之间填充1个"2"，而不是"3"
        ranks = rank.ranks("1", "4", 1);
        Assert.assertEquals("2", ranks.get(0));
    }

    @Test
    public void testNext() {
        Assert.assertEquals("0001", rank.next("0000", 4));
        Assert.assertEquals("0010", rank.next("000z", 4));
        Assert.assertEquals("20", rank.next("1z", 2));
        Assert.assertEquals("12", rank.next("111", 2));
        Assert.assertEquals("02", rank.next("01", 2));
        Assert.assertEquals("02", rank.next("1", 2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNextTooLarge() {
        rank.next("zzzz", 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNextWithInvalidChars() {
        rank.next("!@#", 4);
    }
}
