package com.ezone.ezproject.modules.project.service;

import com.ezone.ezbase.iam.bean.enums.GroupUserType;
import com.ezone.ezproject.configuration.CacheManagers;
import com.ezone.ezproject.dal.entity.ProjectMember;
import com.ezone.ezproject.dal.entity.ProjectMemberExample;
import com.ezone.ezproject.dal.entity.enums.ProjectMemberRole;
import com.ezone.ezproject.dal.mapper.ProjectMemberMapper;
import com.ezone.ezproject.es.entity.ProjectRole;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.project.bean.RoleKeySource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectMemberQueryService {
    private ProjectMemberMapper projectMemberMapper;

    private UserService userService;

    private CompanyService companyService;

    public List<ProjectMember> select(Long projectId) {
        ProjectMemberExample example = new ProjectMemberExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        return projectMemberMapper.selectByExample(example);
    }

    public List<ProjectMember> select(Long projectId, ProjectMemberRole role) {
        ProjectMemberExample example = new ProjectMemberExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andRoleEqualTo(role.name());
        return projectMemberMapper.selectByExample(example);
    }

    public List<ProjectMember> select(Long projectId, RoleSource source, String role) {
        ProjectMemberExample example = new ProjectMemberExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andRoleSourceEqualTo(source.name()).andRoleEqualTo(role);
        return projectMemberMapper.selectByExample(example);
    }

    public boolean hasMember(Long projectId, RoleSource source, String role) {
        ProjectMemberExample example = new ProjectMemberExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andRoleSourceEqualTo(source.name()).andRoleEqualTo(role);
        return projectMemberMapper.countByExample(example) > 0;
    }

    /**
     * 查询成员下的用户（将用户组转换成用户）
     *
     * @param projectId 项目ID
     * @param queryProjectMembers 待查询的项目成员
     * @return
     */
    @NotNull
    public Map<RoleKeySource, Set<String>> selectProjectRoleUsers(Long projectId, List<RoleKeySource> queryProjectMembers) {
        Map<RoleKeySource, Set<String>> roleUsersMap = new HashMap<>();
        if (CollectionUtils.isEmpty(queryProjectMembers)) {
            return roleUsersMap;
        }
         ProjectMemberExample example = new ProjectMemberExample();
        for (RoleKeySource roleKeySource : queryProjectMembers) {
            ProjectMemberExample.Criteria criteria = example.createCriteria();
            criteria.andProjectIdEqualTo(projectId).andRoleEqualTo(roleKeySource.getRole()).andRoleSourceEqualTo(roleKeySource.getRoleSource().name());
            example.or(criteria);
        }
        List<ProjectMember> projectMembers = projectMemberMapper.selectByExample(example);
        if (CollectionUtils.isNotEmpty(projectMembers)) {
            Set<String> groupUser = new HashSet<>();
            Map<String, List<RoleKeySource>> groupNameProjectMemberMap = new HashMap<>();
            projectMembers.forEach(projectMember -> {
                RoleKeySource roleKeySource = RoleKeySource.builder().role(projectMember.getRole()).roleSource(RoleSource.valueOf(projectMember.getRoleSource())).build();
                Set<String> users = roleUsersMap.getOrDefault(roleKeySource, new HashSet<>());
                roleUsersMap.put(roleKeySource, users);
                if (GroupUserType.GROUP.name().equals(projectMember.getUserType())) {
                    groupUser.add(projectMember.getUser());
                    List<RoleKeySource> groupNames = groupNameProjectMemberMap.getOrDefault(projectMember.getUser(), new ArrayList<>());
                    groupNames.add(roleKeySource);
                    groupNameProjectMemberMap.put(projectMember.getUser(), groupNames);
                } else {
                    users.add(projectMember.getUser());
                }
            });
            if (groupUser.size() > 0) {
                Long companyId = companyService.currentCompany();
                Map<String, Set<String>> groupMembers = userService.getGroupMember(companyId, groupUser);

                groupMembers.forEach((groupUserName, users) -> {
                    List<RoleKeySource> roleKeySources = groupNameProjectMemberMap.get(groupUserName);
                    if (CollectionUtils.isNotEmpty(roleKeySources)) {
                        for (RoleKeySource roleKeySource : roleKeySources) {
                            Set<String> roleUsers = roleUsersMap.get(roleKeySource);
                            roleUsers.addAll(users);
                        }
                    }
                });
            }
        }
        return roleUsersMap;
    }

    public Set<String> selectProjectMemberFinalUsers(Long companyId, List<Long> projectIds, int limit) {
        if (CollectionUtils.isEmpty(projectIds)) {
            return SetUtils.EMPTY_SET;
        }
        ProjectMemberExample example = new ProjectMemberExample();
        example.createCriteria().andProjectIdIn(projectIds).andRoleNotEqualTo(ProjectRole.GUEST);
        List<ProjectMember> members = projectMemberMapper.selectByExample(example);
        Set<String> finalMembers = new LinkedHashSet<>();
        Set<String> groups = new HashSet<>();
        members.forEach(member -> {
            if (GroupUserType.USER.name().equals(member.getUserType())) {
                finalMembers.add(member.getUser());
            } else {
                groups.add(member.getUser());
            }
        });
        if (finalMembers.size() >= limit) {
            return finalMembers;
        }
        if (CollectionUtils.isNotEmpty(groups)) {
            Map<String, Set<String>> groupsMembers = userService.getGroupMember(companyId, groups);
            for (Set<String> groupMembers : groupsMembers.values()) {
                for (String member : groupMembers) {
                    finalMembers.add(member);
                    if (finalMembers.size() >= limit) {
                        return finalMembers;
                    }
                }
                if (finalMembers.size() >= limit) {
                    return finalMembers;
                }
            }
        }
        return finalMembers;
    }

    public Map<Long, Integer> selectProjectMemberSize(Long companyId, List<Long> projectIds) {
        if (CollectionUtils.isEmpty(projectIds)) {
            return MapUtils.EMPTY_MAP;
        }
        ProjectMemberExample example = new ProjectMemberExample();
        example.createCriteria().andProjectIdIn(projectIds).andRoleNotEqualTo(ProjectRole.GUEST).andUserTypeEqualTo(GroupUserType.USER.name());
        List<ProjectMember> members = projectMemberMapper.selectByExample(example);
        return members.stream().collect(Collectors.groupingBy(
                m -> m.getProjectId(),
                Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> list.stream()
                                .map(m -> m.getUser())
                                .collect(Collectors.toSet()).size())));
    }

    @NotNull
    public List<Long> selectAdminProjectIds(Long company, String user) {
        List<String> groups = userService.queryGroupNamesByCompanyUsername(company, user);
        ProjectMemberExample example = new ProjectMemberExample();
        example.or().andCompanyIdEqualTo(company).andUserTypeEqualTo(GroupUserType.USER.name()).andUserEqualTo(user).andRoleEqualTo(ProjectMemberRole.ADMIN.name());
        if (CollectionUtils.isNotEmpty(groups)) {
            example.or().andCompanyIdEqualTo(company).andUserTypeEqualTo(GroupUserType.GROUP.name()).andUserIn(groups).andRoleEqualTo(ProjectMemberRole.ADMIN.name());
        }
        List<ProjectMember> members = projectMemberMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(members)) {
            return ListUtils.EMPTY_LIST;
        }
        return members.stream().map(ProjectMember::getProjectId).distinct().collect(Collectors.toList());
    }

    @NotNull
    public List<Long> selectUserRoleProjectIds(Long company, String user) {
        List<String> groups = userService.queryGroupNamesByCompanyUsername(company, user);
        ProjectMemberExample example = new ProjectMemberExample();
        example.or().andCompanyIdEqualTo(company).andUserTypeEqualTo(GroupUserType.USER.name()).andUserEqualTo(user);
        if (CollectionUtils.isNotEmpty(groups)) {
            example.or().andCompanyIdEqualTo(company).andUserTypeEqualTo(GroupUserType.GROUP.name()).andUserIn(groups);
        }
        List<ProjectMember> members = projectMemberMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(members)) {
            return ListUtils.EMPTY_LIST;
        }
        return members.stream().map(ProjectMember::getProjectId).distinct().collect(Collectors.toList());
    }

    @NotNull
    public List<Long> selectUserMemberProjectIds(Long company, String user) {
        List<String> groups = userService.queryGroupNamesByCompanyUsername(company, user);
        ProjectMemberExample example = new ProjectMemberExample();
        example.or().andCompanyIdEqualTo(company).andUserTypeEqualTo(GroupUserType.USER.name()).andUserEqualTo(user).andRoleNotEqualTo(ProjectRole.GUEST);
        if (CollectionUtils.isNotEmpty(groups)) {
            example.or().andCompanyIdEqualTo(company).andUserTypeEqualTo(GroupUserType.GROUP.name()).andUserIn(groups).andRoleNotEqualTo(ProjectRole.GUEST);
        }
        List<ProjectMember> members = projectMemberMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(members)) {
            return ListUtils.EMPTY_LIST;
        }
        return members.stream().map(ProjectMember::getProjectId).distinct().collect(Collectors.toList());
    }

    @NotNull
    public List<String> selectUserProjectRoles(Long company, String user, Long projectId) {
        List<String> groups = userService.queryGroupNamesByCompanyUsername(company, user);
        ProjectMemberExample example = new ProjectMemberExample();
        example.or().andCompanyIdEqualTo(company).andUserTypeEqualTo(GroupUserType.USER.name()).andUserEqualTo(user).andProjectIdEqualTo(projectId);
        if (CollectionUtils.isNotEmpty(groups)) {
            example.or().andCompanyIdEqualTo(company).andUserTypeEqualTo(GroupUserType.GROUP.name()).andUserIn(groups).andProjectIdEqualTo(projectId);
        }
        List<ProjectMember> members = projectMemberMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(members)) {
            return ListUtils.EMPTY_LIST;
        }
        return members.stream().map(ProjectMember::getRole).distinct().collect(Collectors.toList());
    }

    public boolean isUserRoleInProject(Long company, Long projectId, String user, ProjectMemberRole role) {
        List<ProjectMember> members = select(projectId, role);
        return isUserInCompanyProjectMembers(user, company, members);
    }

    public boolean isUserInProject(Long company, Long projectId, String user) {
        if (userService.isCompanyAdmin(user, company)) {
            return true;
        }
        List<ProjectMember> members = select(projectId);
        return isUserInCompanyProjectMembers(user, company, members);
    }

    @Cacheable(
            cacheManager = CacheManagers.TRANSIENT_CACHE_MANAGER,
            cacheNames = "ProjectMemberQueryService.maxRoleInCompanyProjectMembers",
            key = "#user.concat('@').concat(#company).concat(':').concat(#projectId)"
    )
    public ProjectMemberRole maxRoleInCompanyProjectMembers(Long company, Long projectId, String user) {
        List<ProjectMember> members = select(projectId);
        return maxRoleInCompanyProjectMembers(user, company, members);
    }

    private boolean isUserInCompanyProjectMembers(String user, Long company, List<ProjectMember> members) {
        if (CollectionUtils.isEmpty(members)) {
            return false;
        }
        List<String> groupTypeUsers = new ArrayList<>();
        for (ProjectMember member : members) {
            if (StringUtils.equals(GroupUserType.USER.name(), member.getUserType())) {
                if (StringUtils.equals(user, member.getUser())) {
                    return true;
                }
            } else {
                groupTypeUsers.add(member.getUser());
            }
        }
        if (CollectionUtils.isEmpty(groupTypeUsers)) {
            return false;
        }
        List<String> userInGroups = userService.queryGroupNamesByCompanyUsername(company, user);
        if (CollectionUtils.isEmpty(userInGroups)) {
            return false;
        }
        return groupTypeUsers.stream().anyMatch(groupTypeUser -> userInGroups.contains(groupTypeUser));
    }

    private ProjectMemberRole maxRoleInCompanyProjectMembers(String user, Long company, List<ProjectMember> members) {
        if (CollectionUtils.isEmpty(members)) {
            return null;
        }
        ProjectMemberRole role = null;
        List<ProjectMember> groupTypeMembers = new ArrayList<>();
        for (ProjectMember member : members) {
            if (StringUtils.equals(GroupUserType.USER.name(), member.getUserType())) {
                if (StringUtils.equals(user, member.getUser())) {
                    role = ProjectMemberRole.max(role, member.getRole());
                    if (ProjectMemberRole.isMax(role)) {
                        return role;
                    }
                }
            } else {
                groupTypeMembers.add(member);
            }
        }
        if (CollectionUtils.isEmpty(groupTypeMembers)) {
            return role;
        }
        List<String> userInGroups = userService.queryGroupNamesByCompanyUsername(company, user);
        if (CollectionUtils.isEmpty(userInGroups)) {
            return role;
        }
        for (ProjectMember member : members) {
            if (userInGroups.contains(member.getUser())) {
                role = ProjectMemberRole.max(role, member.getRole());
                if (ProjectMemberRole.isMax(role)) {
                    return role;
                }
            }
        }
        return role;
    }

    @Nullable
    public List<ProjectMember> listCompanyRoleMember(Long companyId, String roleKey) {
        ProjectMemberExample example = new ProjectMemberExample();
        example.createCriteria().andCompanyIdEqualTo(companyId).andRoleEqualTo(roleKey).andRoleSourceEqualTo(RoleSource.COMPANY.name());
        return projectMemberMapper.selectByExample(example);

    }
}
