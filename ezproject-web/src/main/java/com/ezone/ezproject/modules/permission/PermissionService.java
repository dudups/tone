package com.ezone.ezproject.modules.permission;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.UserProjectPermissions;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.project.service.ProjectMemberQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class PermissionService {
    private UserProjectPermissionsService userProjectPermissionsService;

    public UserProjectPermissions permissions(String user, Long projectId) throws CodedException {
        return userProjectPermissionsService.userProjectPermissions(user, projectId);
    }

    public boolean isAdmin(String user, Long projectId) {
        UserProjectPermissions permissions = permissions(user, projectId);
        if (permissions == null) {
            return false;
        }
        return permissions.isAdmin();
    }

    public boolean isMember(String user, Long projectId) {
        UserProjectPermissions permissions = permissions(user, projectId);
        if (permissions == null) {
            return false;
        }
        return permissions.isAdmin() || permissions.isMember();
    }

    public boolean isRole(String user, Long projectId) {
        UserProjectPermissions permissions = permissions(user, projectId);
        return permissions != null;
    }

    public boolean isRole(String user, List<Long> projectIds) {
        if (CollectionUtils.isEmpty(projectIds)) {
            return false;
        }
        for (Long projectId : projectIds) {
            UserProjectPermissions permissions = permissions(user, projectId);
            if (permissions == null) {
                return false;
            }
        }
        return true;
    }
}
