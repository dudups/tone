package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.es.entity.enums.RoleType;
import org.junit.Test;
import org.testng.Assert;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class UserProjectPermissionsTest {
    @Test
    public void testBuildAdmin() {
        UserProjectPermissions permissions;

        permissions = UserProjectPermissions.from("u1", null);
        Assert.assertEquals(permissions.getUser(), "u1");
        Assert.assertFalse(permissions.isAdmin());

        permissions = UserProjectPermissions.fromAdmin("u1");
        Assert.assertTrue(permissions.isAdmin());

        permissions = UserProjectPermissions.from("u1", Arrays.asList(ProjectRole.builder().type(RoleType.MEMBER).build()));
        Assert.assertFalse(permissions.isAdmin());

        permissions = UserProjectPermissions.from("u1", Arrays.asList(ProjectRole.builder().type(RoleType.ADMIN).build()));
        Assert.assertTrue(permissions.isAdmin());
    }

    @Test
    public void testBuildMember() {
        UserProjectPermissions permissions;

        permissions = UserProjectPermissions.from("u1", Arrays.asList(ProjectRole.builder().type(RoleType.GUEST).build()));
        Assert.assertFalse(permissions.isMember());

        permissions = UserProjectPermissions.from("u1", Arrays.asList(ProjectRole.builder().type(RoleType.MEMBER).build()));
        Assert.assertTrue(permissions.isMember());
    }

    @Test
    public void testMergeEnable() {
        UserProjectPermissions permissions;

        permissions = UserProjectPermissions.from("u1", Arrays.asList(
                ProjectRole.builder()
                        .type(RoleType.MEMBER)
                        .operation(OperationType.PLAN_CREATE, OperationConfig.builder().enable(false).build())
                        .build(),
                ProjectRole.builder()
                        .type(RoleType.GUEST)
                        .operation(OperationType.PLAN_CREATE, OperationConfig.builder().enable(true).build())
                        .build()
        ));
        Assert.assertFalse(permissions.hasPermission(OperationType.PLAN_CREATE));
    }

    @Test
    public void testMergeList() {
        UserProjectPermissions permissions;

        permissions = UserProjectPermissions.from("u1", Arrays.asList(
                ProjectRole.builder()
                        .type(RoleType.MEMBER)
                        .operation(OperationType.CARD_CREATE, OperationConfig.builder().cardTypes(new HashSet<>(Arrays.asList("story"))).build())
                        .build(),
                ProjectRole.builder()
                        .type(RoleType.GUEST)
                        .operation(OperationType.CARD_CREATE, OperationConfig.builder().enable(true).build())
                        .build()
        ));

        Map<String, Object> cardProps = new HashMap<>();

        cardProps.put(CardField.TYPE, "story");
        Assert.assertTrue(permissions.hasLimitPermission(OperationType.CARD_CREATE, cardProps));

        cardProps.put(CardField.TYPE, "bug");
        cardProps.put(CardField.STATUS, CardStatus.FIRST);
        Assert.assertTrue(permissions.hasLimitPermission(OperationType.CARD_CREATE, cardProps));
    }

    @Test
    public void testMergeLimitList() {
        UserProjectPermissions permissions;

        permissions = UserProjectPermissions.from("u1", Arrays.asList(
                ProjectRole.builder()
                        .type(RoleType.MEMBER)
                        .operation(OperationType.CARD_CREATE, OperationConfig.builder().cardTypes(new HashSet<>(Arrays.asList("story"))).build())
                        .build(),
                ProjectRole.builder()
                        .type(RoleType.GUEST)
                        .operation(OperationType.CARD_CREATE, OperationConfig.builder().enable(true).build())
                        .build()
        ));

        Map<String, Object> cardProps = new HashMap<>();
        cardProps.put(CardField.STATUS, CardStatus.FIRST);

        cardProps.put(CardField.TYPE, "story");
        Assert.assertTrue(permissions.hasLimitPermission(OperationType.CARD_CREATE, cardProps));

        cardProps.put(CardField.TYPE, "bug");
        Assert.assertTrue(permissions.hasLimitPermission(OperationType.CARD_CREATE, cardProps));

        cardProps.put(CardField.TYPE, "story");
        cardProps.put(CardField.PLAN_ID, 1L);
        Assert.assertTrue(permissions.hasLimitPermission(OperationType.CARD_CREATE, cardProps));

        cardProps.put(CardField.TYPE, "bug");
        cardProps.put(CardField.PLAN_ID, 1L);
        Assert.assertFalse(permissions.hasLimitPermission(OperationType.CARD_CREATE, cardProps));
    }
}
