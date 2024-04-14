package com.ezone.ezproject.dal.entity.enums;

import lombok.AllArgsConstructor;

@Deprecated
@AllArgsConstructor
public enum ProjectMemberRole {
    ADMIN(10), MEMBER(9);

    private int permissionLevel;

    public static boolean isMax(ProjectMemberRole role) {
        return role == ADMIN;
    }

    public static boolean isMax(String role) {
        return isMax(parse(role));
    }

    public static ProjectMemberRole max(ProjectMemberRole r1, ProjectMemberRole r2) {
        if (null == r1) {
            return r2;
        }
        if (null == r2) {
            return r1;
        }
        if (r2.permissionLevel > r1.permissionLevel) {
            return r2;
        }
        return r1;
    }

    public static ProjectMemberRole max(String r1, String r2) {
        return max(parse(r1), parse(r2));
    }

    public static ProjectMemberRole max(ProjectMemberRole r1, String r2) {
        return max(r1, parse(r2));
    }

    private static ProjectMemberRole parse(String role) {
        try {
            return ProjectMemberRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
