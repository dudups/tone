package com.ezone.ezproject.modules.card.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author cf
 */
class CardMemberChangedNoticeServiceTest {

    @Test
    void calcDiff() {
        List<String> removes = new ArrayList<>();
        List<String> adds = new ArrayList<>();
        String from = "afei apollo ezone-test";
        String to = "afei apollo 关甜甜\n";
        CardMemberChangedNoticeService.calcDiff(from, to, adds, removes);
        assertTrue(adds.contains("关甜甜"), "计算add异常");
        assertTrue(removes.contains("ezone-test"),"计算remove异常");
    }
}