package com.ezone.ezproject.modules.portfolio.service;

import com.ezone.ezbase.iam.bean.enums.GroupUserType;
import com.ezone.ezproject.dal.entity.PortfolioMember;
import com.ezone.ezproject.dal.entity.PortfolioMemberExample;
import com.ezone.ezproject.dal.mapper.PortfolioMemberMapper;
import com.ezone.ezproject.es.entity.PortfolioRole;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.project.bean.RoleKeySource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class PortfolioMemberQueryService {
    private PortfolioMemberMapper portfolioMemberMapper;
    private UserService userService;
    private CompanyService companyService;

    public List<PortfolioMember> select(Long portfolioId) {
        PortfolioMemberExample example = new PortfolioMemberExample();
        example.createCriteria().andPortfolioIdEqualTo(portfolioId);
        return portfolioMemberMapper.selectByExample(example);
    }

    public List<PortfolioMember> select(Long portfolioId, RoleSource source, String role) {
        PortfolioMemberExample example = new PortfolioMemberExample();
        example.createCriteria().andPortfolioIdEqualTo(portfolioId).andRoleSourceEqualTo(source.name()).andRoleEqualTo(role);
        return portfolioMemberMapper.selectByExample(example);
    }

    public boolean hasMember(Long portfolioId, RoleSource source, String role) {
        PortfolioMemberExample example = new PortfolioMemberExample();
        example.createCriteria().andPortfolioIdEqualTo(portfolioId).andRoleSourceEqualTo(source.name()).andRoleEqualTo(role);
        return portfolioMemberMapper.countByExample(example) > 0;
    }

    @NotNull
    public List<Long> selectUserRolePortfolioIds(Long company, String user) {
        List<String> groups = userService.queryGroupNamesByCompanyUsername(company, user);
        PortfolioMemberExample example = new PortfolioMemberExample();
        example.or().andCompanyIdEqualTo(company).andUserTypeEqualTo(GroupUserType.USER.name()).andUserEqualTo(user);
        if (CollectionUtils.isNotEmpty(groups)) {
            example.or().andCompanyIdEqualTo(company).andUserTypeEqualTo(GroupUserType.GROUP.name()).andUserIn(groups);
        }
        List<PortfolioMember> members = portfolioMemberMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(members)) {
            return ListUtils.EMPTY_LIST;
        }
        return members.stream().map(PortfolioMember::getPortfolioId).distinct().collect(Collectors.toList());
    }

    /**
     * 查询用户为项目管理员角色时的所有项目。
     *
     * @param company
     * @param user
     * @return
     */
    @NotNull
    public List<Long> selectAdminPortfolioIds(Long company, String user) {
        List<String> groups = userService.queryGroupNamesByCompanyUsername(company, user);
        PortfolioMemberExample example = new PortfolioMemberExample();
        example.or().andCompanyIdEqualTo(company).andUserTypeEqualTo(GroupUserType.USER.name()).andUserEqualTo(user).andRoleEqualTo(PortfolioRole.ADMIN);
        if (CollectionUtils.isNotEmpty(groups)) {
            example.or().andCompanyIdEqualTo(company).andUserTypeEqualTo(GroupUserType.GROUP.name()).andUserIn(groups).andRoleEqualTo(PortfolioRole.ADMIN);
        }
        List<PortfolioMember> members = portfolioMemberMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(members)) {
            return ListUtils.EMPTY_LIST;
        }
        return members.stream().map(PortfolioMember::getPortfolioId).distinct().collect(Collectors.toList());
    }

    @NotNull
    public List<String> selectUserPortfolioRoles(Long company, String user, Long portfolioId) {
        List<String> groups = userService.queryGroupNamesByCompanyUsername(company, user);
        PortfolioMemberExample example = new PortfolioMemberExample();
        example.or().andCompanyIdEqualTo(company).andUserTypeEqualTo(GroupUserType.USER.name()).andUserEqualTo(user).andPortfolioIdEqualTo(portfolioId);
        if (CollectionUtils.isNotEmpty(groups)) {
            example.or().andCompanyIdEqualTo(company).andUserTypeEqualTo(GroupUserType.GROUP.name()).andUserIn(groups).andPortfolioIdEqualTo(portfolioId);
        }
        List<PortfolioMember> members = portfolioMemberMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(members)) {
            return ListUtils.EMPTY_LIST;
        }
        return members.stream().map(PortfolioMember::getRole).distinct().collect(Collectors.toList());
    }

    /**
     * 查询成员下的用户（将用户组转换成用户）
     *
     * @param projectId           项目ID
     * @param queryProjectMembers 待查询的项目成员
     * @return
     */
    @NotNull
    public Map<RoleKeySource, Set<String>> selectPortfolioMemberUsers(Long projectId, List<RoleKeySource> queryProjectMembers) {
        Map<RoleKeySource, Set<String>> roleUsersMap = new HashMap<>();
        if (CollectionUtils.isEmpty(queryProjectMembers)) {
            return roleUsersMap;
        }
        PortfolioMemberExample example = new PortfolioMemberExample();
        for (RoleKeySource roleKeySource : queryProjectMembers) {
            PortfolioMemberExample.Criteria criteria = example.createCriteria();
            criteria.andPortfolioIdEqualTo(projectId).andRoleEqualTo(roleKeySource.getRole()).andRoleSourceEqualTo(roleKeySource.getRoleSource().name());
            example.or(criteria);
        }
        List<PortfolioMember> projectMembers = portfolioMemberMapper.selectByExample(example);
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
}
