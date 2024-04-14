package com.ezone.ezproject.modules.card.service;

import com.ezone.ezproject.common.SortUtil;
import com.ezone.ezproject.dal.entity.CardDocRel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author cf
 */
class SortUtilTest {

    @Test
    public void testSortByIds() {
        List<CardDocRel> list = new ArrayList<>();
        list.add(CardDocRel.builder().docId(3L).build());
        list.add(CardDocRel.builder().docId(1L).build());
        list.add(CardDocRel.builder().docId(6L).build());
        list.add(CardDocRel.builder().docId(5L).build());
        List<Long> ids = Arrays.asList(1L, 3L, 5L, 6L);

        List<CardDocRel> sort = SortUtil.sortByIds(ids, list, cardDocRel -> cardDocRel.getDocId());
        for (int i = 0; i < ids.size(); i++) {
            assertEquals(sort.get(i).getDocId(), ids.get(i));
        }
    }
}