package com.ezone.ezproject.modules.project.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yinchengfeng
 * @date 2022/3/26
 * @Description:
 */
class ProjectSummaryServiceTest {

    @Test
    void mergerTypes() {
        List<String> oldTypes = Arrays.asList("milestone", "bugTrend", "activeCards", "endCards",
                "codeData", "buildData", "deployData", "cardsScatter", "cardTrend");
        List<String> newTypes=Arrays.asList("milestone", "bugTrend", "activeCards", "endCards",
                "codeData", "buildData", "deployData", "cardsScatter");
        List<String> result = ProjectSummaryService.mergerTypes(oldTypes, newTypes);
        assertEquals(result.size(), oldTypes.size(), "大小异常");

        result = ProjectSummaryService.mergerTypes(newTypes, oldTypes);
        assertEquals(result.size(), newTypes.size(), "大小异常");
    }
}