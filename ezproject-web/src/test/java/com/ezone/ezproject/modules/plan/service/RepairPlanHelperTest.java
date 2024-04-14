package com.ezone.ezproject.modules.plan.service;

import com.ezone.ezproject.dal.entity.Plan;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author cf
 */
class RepairPlanHelperTest {

    @Test
    public void testFindAncestorsDeleteOrInactive1() {
        RepairPlanHelper helper = RepairPlanHelper.builder().build();
        RepairPlanHelper.PlanNode planNode = new RepairPlanHelper.PlanNode();
        Plan root = Plan.builder().id(1L).ancestorId(0L).parentId(0L).isActive(true).build();
        planNode.setRoot(root);
        Plan node2 = Plan.builder().id(2L).ancestorId(1L).parentId(1L).isActive(true).build();
        Plan node3 = Plan.builder().id(3L).ancestorId(2L).parentId(2L).isActive(true).build();
        Plan node4 = Plan.builder().id(4L).ancestorId(3L).parentId(3L).isActive(true).build();
        List<Plan> plans = Arrays.asList(node2, node3, node4);
        planNode.setSubPlans(plans);
        List<Plan> errorPlans = helper.findAncestorsDeleteOrInactive(planNode);
        List<Long> errIds = errorPlans.stream().map(Plan::getId).collect(Collectors.toList());
        assertTrue(CollectionUtils.isEqualCollection(errIds, Arrays.asList()), "超预期");
        Plan lastActive = helper.findFirstInactiveOrDeletePrePlan(planNode);
        assertEquals(4L, lastActive.getId());
    }

    @Test
    public void testFindAncestorsDeleteOrInactive2() {
        RepairPlanHelper helper = RepairPlanHelper.builder().build();
        RepairPlanHelper.PlanNode planNode = new RepairPlanHelper.PlanNode();
        Plan root = Plan.builder().id(1L).ancestorId(0L).parentId(0L).isActive(true).build();
        planNode.setRoot(root);
        Plan node2 = Plan.builder().id(2L).ancestorId(1L).parentId(1L).isActive(true).build();
        Plan node3 = Plan.builder().id(3L).ancestorId(2L).parentId(2L).isActive(false).build();
        Plan node4 = Plan.builder().id(4L).ancestorId(3L).parentId(3L).isActive(true).build();
        List<Plan> plans = Arrays.asList(node2, node3, node4);
        planNode.setSubPlans(plans);
        List<Plan> errorPlans = helper.findAncestorsDeleteOrInactive(planNode);
        List<Long> errIds = errorPlans.stream().map(Plan::getId).collect(Collectors.toList());
        assertTrue(CollectionUtils.isEqualCollection(errIds, Arrays.asList(4L)), "超预期");
        Plan lastActive = helper.findFirstInactiveOrDeletePrePlan(planNode);
        assertEquals(3L, lastActive.getId());
    }

    @Test
    public void testFindAncestorsDeleteOrInactive3() {
        RepairPlanHelper helper = RepairPlanHelper.builder().build();
        RepairPlanHelper.PlanNode planNode = new RepairPlanHelper.PlanNode();
        Plan root = Plan.builder().id(1L).ancestorId(0L).parentId(0L).isActive(true).build();
        planNode.setRoot(root);
        Plan node2 = Plan.builder().id(2L).ancestorId(1L).parentId(1L).isActive(true).build();
//        Plan node3 = Plan.builder().id(3L).ancestorId(2L).parentId(2L).isActive(false).build();
        Plan node4 = Plan.builder().id(4L).ancestorId(3L).parentId(3L).isActive(true).build();
        List<Plan> plans = Arrays.asList(node2, node4);
        planNode.setSubPlans(plans);
        List<Plan> errorPlans = helper.findAncestorsDeleteOrInactive(planNode);
        List<Long> errIds = errorPlans.stream().map(Plan::getId).collect(Collectors.toList());
        assertTrue(CollectionUtils.isEqualCollection(errIds, Arrays.asList(4L)), "超预期");
        Plan lastActive = helper.findFirstInactiveOrDeletePrePlan(planNode);
        assertEquals(root.getId(), lastActive.getId());
    }

    @Test
    public void testFindAncestorsDeleteOrInactive4() {
        RepairPlanHelper helper = RepairPlanHelper.builder().build();
        RepairPlanHelper.PlanNode planNode = new RepairPlanHelper.PlanNode();
        Plan root = Plan.builder().id(1L).ancestorId(0L).parentId(0L).isActive(true).build();
        planNode.setRoot(root);
        Plan node2 = Plan.builder().id(2L).ancestorId(1L).parentId(1L).isActive(true).build();
        Plan node3 = Plan.builder().id(3L).ancestorId(2L).parentId(2L).isActive(true).build();
        Plan node4 = Plan.builder().id(4L).ancestorId(3L).parentId(3L).isActive(false).build();
        List<Plan> plans = Arrays.asList(node2, node3, node4);
        planNode.setSubPlans(plans);
        List<Plan> errorPlans = helper.findAncestorsDeleteOrInactive(planNode);
        List<Long> errIds = errorPlans.stream().map(Plan::getId).collect(Collectors.toList());
        assertTrue(CollectionUtils.isEqualCollection(errIds, Arrays.asList()), "超预期");
        Plan lastActive = helper.findFirstInactiveOrDeletePrePlan(planNode);
        assertEquals(4L, lastActive.getId());
    }

    @Test
    public void testFindAncestorsDeleteOrInactive5() {
        RepairPlanHelper helper = RepairPlanHelper.builder().build();
        RepairPlanHelper.PlanNode planNode = new RepairPlanHelper.PlanNode();
        Plan root = Plan.builder().id(1L).ancestorId(0L).parentId(0L).isActive(true).build();
        planNode.setRoot(root);
        Plan node2 = Plan.builder().id(2L).ancestorId(1L).parentId(1L).isActive(true).build();
        Plan node3 = Plan.builder().id(3L).ancestorId(2L).parentId(2L).isActive(false).build();
//        Plan node4 = Plan.builder().id(4L).ancestorId(3L).parentId(3L).isActive(false).build();
        Plan node5 = Plan.builder().id(5L).ancestorId(4L).parentId(4L).isActive(false).build();
        Plan node6 = Plan.builder().id(6L).ancestorId(5L).parentId(5L).isActive(true).build();
        List<Plan> plans = Arrays.asList(node5, node6, node2, node3);
        planNode.setSubPlans(plans);
        List<Plan> errorPlans = helper.findAncestorsDeleteOrInactive(planNode);
        List<Long> errIds = errorPlans.stream().map(Plan::getId).collect(Collectors.toList());
        assertTrue(CollectionUtils.isEqualCollection(errIds, Arrays.asList(5L, 6L)), "超预期");
        Plan lastActive = helper.findFirstInactiveOrDeletePrePlan(planNode);
        assertEquals(3L, lastActive.getId());
    }

    @Test
    public void testFindAncestorsDeleteOrInactive6() {
        RepairPlanHelper helper = RepairPlanHelper.builder().build();
        RepairPlanHelper.PlanNode planNode = new RepairPlanHelper.PlanNode();
        Plan root = Plan.builder().id(1L).ancestorId(0L).parentId(0L).isActive(true).build();
        planNode.setRoot(root);
        Plan node2 = Plan.builder().id(2L).ancestorId(1L).parentId(1L).isActive(true).build();
        Plan node3 = Plan.builder().id(3L).ancestorId(2L).parentId(2L).isActive(true).build();
//        Plan node4 = Plan.builder().id(4L).ancestorId(3L).parentId(3L).isActive(false).build();
        Plan node5 = Plan.builder().id(5L).ancestorId(4L).parentId(4L).isActive(false).build();
        Plan node6 = Plan.builder().id(6L).ancestorId(5L).parentId(5L).isActive(true).build();
        List<Plan> plans = Arrays.asList(node5, node6, node2, node3);
        planNode.setSubPlans(plans);
        List<Plan> errorPlans = helper.findAncestorsDeleteOrInactive(planNode);
        List<Long> errIds = errorPlans.stream().map(Plan::getId).collect(Collectors.toList());
        assertTrue(CollectionUtils.isEqualCollection(errIds, Arrays.asList(5L, 6L)), "超预期");
        Plan lastActive = helper.findFirstInactiveOrDeletePrePlan(planNode);
        assertEquals(root.getId(), lastActive.getId());
    }

    @Test
    public void testFindAncestorsDeleteOrInactive7() {
        RepairPlanHelper helper = RepairPlanHelper.builder().build();
        RepairPlanHelper.PlanNode planNode = new RepairPlanHelper.PlanNode();
        Plan root = Plan.builder().id(1L).ancestorId(0L).parentId(0L).isActive(true).build();
        planNode.setRoot(root);
        Plan node2 = Plan.builder().id(2L).ancestorId(1L).parentId(1L).isActive(true).build();
        Plan node3 = Plan.builder().id(3L).ancestorId(2L).parentId(2L).isActive(true).build();
//        Plan node4 = Plan.builder().id(4L).ancestorId(3L).parentId(3L).isActive(false).build();
        Plan node5 = Plan.builder().id(5L).ancestorId(4L).parentId(4L).isActive(false).build();
        Plan node6 = Plan.builder().id(6L).ancestorId(5L).parentId(5L).isActive(true).build();
        List<Plan> plans = Arrays.asList(node5, node6, node2, node3);
        planNode.setSubPlans(plans);
        Map<RepairPlanHelper.PlanAncestorState, List<Plan>> ancestorsDeleteOrInactiveMap = helper.findAncestorsDeleteOrInactiveMap(planNode);
        List<Plan> deletePlans = ancestorsDeleteOrInactiveMap.get(RepairPlanHelper.PlanAncestorState.delete);
        assertTrue(deletePlans.isEmpty(), "超预期");
        List<Plan> inactivePlans = ancestorsDeleteOrInactiveMap.get(RepairPlanHelper.PlanAncestorState.inactive);
        List<Long> inactiveIds = inactivePlans.stream().map(Plan::getId).collect(Collectors.toList());
        assertTrue(CollectionUtils.isEqualCollection(inactiveIds, Arrays.asList(6L)), "超预期");
        Plan lastActive = helper.findFirstInactiveOrDeletePrePlan(planNode);
        assertEquals(root.getId(), lastActive.getId());
    }


    @Test
    public void testFindAncestorsDeleteOrInactive8() {
        RepairPlanHelper helper = RepairPlanHelper.builder().build();
        RepairPlanHelper.PlanNode planNode = new RepairPlanHelper.PlanNode();
        Plan root = Plan.builder().id(1L).ancestorId(0L).parentId(0L).isActive(true).build();
        planNode.setRoot(root);
        Plan node2 = Plan.builder().id(2L).ancestorId(1L).parentId(1L).isActive(true).build();
        Plan node3 = Plan.builder().id(3L).ancestorId(2L).parentId(2L).isActive(true).build();
//        Plan node4 = Plan.builder().id(4L).ancestorId(3L).parentId(3L).isActive(false).build();
        Plan node5 = Plan.builder().id(5L).ancestorId(4L).parentId(4L).isActive(true).build();
        Plan node6 = Plan.builder().id(6L).ancestorId(5L).parentId(5L).isActive(true).build();
        List<Plan> plans = Arrays.asList(node5, node6, node2, node3);
        planNode.setSubPlans(plans);
        Map<RepairPlanHelper.PlanAncestorState, List<Plan>> ancestorsDeleteOrInactiveMap = helper.findAncestorsDeleteOrInactiveMap(planNode);
        List<Plan> deletePlans = ancestorsDeleteOrInactiveMap.get(RepairPlanHelper.PlanAncestorState.delete);
        assertEquals(2, deletePlans.size(), "超预期");
    }

    @Test
    public void testFindNearestInactivePlan() {
        RepairPlanHelper helper = RepairPlanHelper.builder().build();
        RepairPlanHelper.PlanNode planNode = new RepairPlanHelper.PlanNode();
        Plan root = Plan.builder().id(1L).ancestorId(0L).parentId(0L).isActive(true).build();
        planNode.setRoot(root);
        Plan node2 = Plan.builder().id(2L).ancestorId(1L).parentId(1L).isActive(true).build();
        Plan node3 = Plan.builder().id(3L).ancestorId(2L).parentId(2L).isActive(true).build();
//        Plan node4 = Plan.builder().id(4L).ancestorId(3L).parentId(3L).isActive(false).build();
        Plan node5 = Plan.builder().id(5L).ancestorId(4L).parentId(4L).isActive(false).build();
        Plan node6 = Plan.builder().id(6L).ancestorId(5L).parentId(5L).isActive(true).build();
        List<Plan> plans = Arrays.asList(node5, node6, node2, node3);
        planNode.setSubPlans(plans);
        Plan nearestInactivePlan = helper.findNearestInactivePlan(planNode, node6);
        assertEquals(5L, nearestInactivePlan.getId());
        nearestInactivePlan = helper.findNearestInactivePlan(planNode, node5);
        assertEquals(root.getId(), nearestInactivePlan.getId());
    }

    @Test
    public void testFindNearestInactivePlan2() {
        RepairPlanHelper helper = RepairPlanHelper.builder().build();
        RepairPlanHelper.PlanNode planNode = new RepairPlanHelper.PlanNode();
        Plan root = Plan.builder().id(1L).ancestorId(0L).parentId(0L).isActive(true).build();
        planNode.setRoot(root);
        Plan node2 = Plan.builder().id(2L).ancestorId(1L).parentId(1L).isActive(true).build();
        Plan node3 = Plan.builder().id(3L).ancestorId(2L).parentId(2L).isActive(false).build();
//        Plan node4 = Plan.builder().id(4L).ancestorId(3L).parentId(3L).isActive(false).build();
        Plan node5 = Plan.builder().id(5L).ancestorId(4L).parentId(4L).isActive(true).build();
        Plan node6 = Plan.builder().id(6L).ancestorId(5L).parentId(5L).isActive(true).build();
        List<Plan> plans = Arrays.asList(node5, node6, node2, node3);
        planNode.setSubPlans(plans);
        Plan nearestInactivePlan = helper.findNearestInactivePlan(planNode, node6);
        assertEquals(root.getId(), nearestInactivePlan.getId());
        nearestInactivePlan = helper.findNearestInactivePlan(planNode, node5);
        assertEquals(root.getId(), nearestInactivePlan.getId());
    }

    @Test
    public void testFindNearestInactivePlan3() {
        RepairPlanHelper helper = RepairPlanHelper.builder().build();
        RepairPlanHelper.PlanNode planNode = new RepairPlanHelper.PlanNode();
        Plan root = Plan.builder().id(1L).ancestorId(0L).parentId(0L).isActive(true).build();
        planNode.setRoot(root);
        Plan node2 = Plan.builder().id(2L).ancestorId(1L).parentId(1L).isActive(true).build();
        Plan node3 = Plan.builder().id(3L).ancestorId(2L).parentId(2L).isActive(false).build();
        Plan node4 = Plan.builder().id(4L).ancestorId(3L).parentId(3L).isActive(false).build();
        Plan node5 = Plan.builder().id(5L).ancestorId(4L).parentId(4L).isActive(true).build();
        Plan node6 = Plan.builder().id(6L).ancestorId(5L).parentId(5L).isActive(true).build();
        List<Plan> plans = Arrays.asList(node5, node6, node2, node3, node4);
        planNode.setSubPlans(plans);
        Plan nearestInactivePlan = helper.findNearestInactivePlan(planNode, node6);
        assertEquals(4L, nearestInactivePlan.getId());
        nearestInactivePlan = helper.findNearestInactivePlan(planNode, node5);
        assertEquals(4L, nearestInactivePlan.getId());
        nearestInactivePlan = helper.findNearestInactivePlan(planNode, node3);
        assertEquals(root.getId(), nearestInactivePlan.getId());
        nearestInactivePlan = helper.findNearestInactivePlan(planNode, node2);
        assertEquals(root.getId(), nearestInactivePlan.getId());
        nearestInactivePlan = helper.findNearestInactivePlan(planNode, root);
        assertEquals(root.getId(), nearestInactivePlan.getId());
    }
}