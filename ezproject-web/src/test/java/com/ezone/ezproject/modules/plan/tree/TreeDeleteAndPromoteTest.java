package com.ezone.ezproject.modules.plan.tree;

import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.mapper.PlanMapper;
import org.junit.Test;
import org.mockito.Mockito;
import org.testng.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TreeDeleteAndPromoteTest {

    private List<Plan> plan1to5() {
        return Arrays.asList(
                null,
                Plan.builder().id(1L).parentId(0L).ancestorId(0L).build(),
                Plan.builder().id(2L).parentId(1L).ancestorId(1L).build(),
                Plan.builder().id(3L).parentId(2L).ancestorId(1L).build(),
                Plan.builder().id(4L).parentId(3L).ancestorId(1L).build(),
                Plan.builder().id(5L).parentId(4L).ancestorId(1L).build()
        );
    }

    private void run(List<Plan> plans, List<Long> deletePlanIds) {
        List<Long> planIds = TreeDeleteAndPromote.builder()
                .planMapper(Mockito.mock(PlanMapper.class))
                .plans(plans.stream().filter(Objects::nonNull).collect(Collectors.toList()))
                .isExpired(plan -> deletePlanIds.contains(plan.getId()))
                .build()
                .run();
        Assert.assertEquals(deletePlanIds.size(), planIds.size());
        deletePlanIds.forEach(planIds::contains);
    }



    @Test
    public void testDeleteTop() {
        List<Plan> plans = plan1to5();
        List<Long> deletePlanIds = Arrays.asList(1L);
        run(plans, deletePlanIds);

        Assert.assertEquals((long)plans.get(2).getParentId(), 0L);
        Assert.assertEquals((long)plans.get(3).getParentId(), 2L);
        Assert.assertEquals((long)plans.get(4).getParentId(), 3L);
        Assert.assertEquals((long)plans.get(5).getParentId(), 4L);
    }

    @Test
    public void testDeleteBottom() {
        List<Plan> plans = plan1to5();
        List<Long> deletePlanIds = Arrays.asList(5L);
        run(plans, deletePlanIds);

        Assert.assertEquals((long)plans.get(1).getParentId(), 0L);
        Assert.assertEquals((long)plans.get(2).getParentId(), 1L);
        Assert.assertEquals((long)plans.get(3).getParentId(), 2L);
        Assert.assertEquals((long)plans.get(4).getParentId(), 3L);
    }

    @Test
    public void testDeleteParentChild() {
        List<Plan> plans = plan1to5();
        List<Long> deletePlanIds = Arrays.asList(2L, 3L);
        run(plans, deletePlanIds);

        Assert.assertEquals((long)plans.get(1).getParentId(), 0L);
        Assert.assertEquals((long)plans.get(4).getParentId(), 1L);
        Assert.assertEquals((long)plans.get(5).getParentId(), 4L);
    }

    @Test
    public void testDeleteAncestorDescendant() {
        List<Plan> plans = plan1to5();
        List<Long> deletePlanIds = Arrays.asList(2L, 4L);
        run(plans, deletePlanIds);

        Assert.assertEquals((long)plans.get(1).getParentId(), 0L);
        Assert.assertEquals((long)plans.get(3).getParentId(), 1L);
        Assert.assertEquals((long)plans.get(5).getParentId(), 3L);
    }
}
