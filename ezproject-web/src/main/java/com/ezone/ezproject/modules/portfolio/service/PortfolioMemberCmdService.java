package com.ezone.ezproject.modules.portfolio.service;

import com.ezone.ezbase.iam.bean.enums.GroupUserType;
import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Portfolio;
import com.ezone.ezproject.dal.entity.PortfolioMember;
import com.ezone.ezproject.dal.entity.PortfolioMemberExample;
import com.ezone.ezproject.dal.mapper.PortfolioMemberMapper;
import com.ezone.ezproject.es.entity.PortfolioRole;
import com.ezone.ezproject.es.entity.PortfolioRoleSchema;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.es.entity.enums.RoleType;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.project.bean.MemberBean;
import com.ezone.ezproject.modules.project.bean.RoleKeySource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
@AllArgsConstructor
public class PortfolioMemberCmdService {
    private PortfolioMemberMapper portfolioMemberMapper;
    private PortfolioMemberQueryService portfolioMemberQueryService;
    private UserPortfolioPermissionsService userPortfolioPermissionsService;
    private PortfolioSchemaQueryService portfolioSchemaQueryService;
    private PortfolioQueryService portfolioQueryService;
    private UserService userService;

    public void initMembers(Portfolio portfolio, String adminUser) {
        if (StringUtils.isNotEmpty(adminUser)) {
            portfolioMemberMapper.insert(PortfolioMember.builder()
                    .id(IdUtil.generateId())
                    .portfolioId(portfolio.getId())
                    .userType(GroupUserType.USER.name())
                    .user(adminUser)
                    .companyId(portfolio.getCompanyId())
                    .role(PortfolioRole.ADMIN)
                    .roleSource(RoleSource.SYS.name())
                    .roleType(RoleType.ADMIN.name())
                    .build());
        }
    }

    /**
     * 将数据库中sourceRole中在targetRole中不存在的角色成员迁移过去。
     *
     * @param portfolioId
     * @param sourceRole
     * @param targetRole
     */
    public void migrateRoleUsers(Long portfolioId, RoleKeySource sourceRole, RoleKeySource targetRole) {
        if (targetRole == null || sourceRole == null) {
            return;
        }
        List<PortfolioMember> members = portfolioMemberQueryService.select(portfolioId);
        List<PortfolioMember> sourceMembers = new ArrayList<>();
        List<PortfolioMember> targetMembers = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(members)) {
            members.forEach(projectMember -> {
                if (projectMember.getRole().equals(sourceRole.getRole()) && projectMember.getRoleSource().equals(sourceRole.getRoleSource().name())) {
                    sourceMembers.add(projectMember);
                }
                if (projectMember.getRole().equals(targetRole.getRole()) && projectMember.getRoleSource().equals(targetRole.getRoleSource().name())) {
                    targetMembers.add(projectMember);
                }
            });
            sourceMembers.forEach(sourceMember -> {
                if (!targetMembers.stream().anyMatch(targetMember ->
                        sourceMember.getUserType().equals(targetMember.getUserType())
                                && sourceMember.getUser().equals(targetMember.getUser())
                                && sourceMember.getRoleSource().equals(targetMember.getRoleSource())
                                && sourceMember.getRole().equals(targetMember.getRole())
                )) {
                    sourceMember.setRole(targetRole.getRole());
                    sourceMember.setRoleSource(targetRole.getRoleSource().name());
                    portfolioMemberMapper.updateByPrimaryKeySelective(sourceMember);
                }
            });
            userPortfolioPermissionsService.cacheEvict(portfolioId, userService.currentUserName());
        }
    }

    public void deleteByPortfolioId(Long portfolioId) {
        PortfolioMemberExample example = new PortfolioMemberExample();
        example.createCriteria().andPortfolioIdEqualTo(portfolioId);
        portfolioMemberMapper.deleteByExample(example);
    }

    public void setPortfolioMembers(Long portfolioId, List<MemberBean> members) throws IOException {
        PortfolioRoleSchema schema = portfolioSchemaQueryService.getPortfolioRoleSchema(portfolioId);
        if (CollectionUtils.isEmpty(members) || members.stream().noneMatch(memberBean -> memberBean.getRole().equals(RoleType.ADMIN.name()))) {
            throw new CodedException(HttpStatus.BAD_REQUEST, "至少保留一名管理员！");
        }
        if (CollectionUtils.isNotEmpty(members)) {
            members = members.stream().filter(member -> schema.findRole(member.getSource(), member.getRole()) != null).collect(Collectors.toList());
        }
        List<MemberBean> finalMembers = members;
        List<PortfolioMember> membersInDb = portfolioMemberQueryService.select(portfolioId);
        List<PortfolioMember> deletedMembers = new ArrayList<>();
        membersInDb.forEach(memberInDb -> {
            if (!finalMembers.stream().anyMatch(member -> member.equals(memberInDb))) {
                portfolioMemberMapper.deleteByPrimaryKey(memberInDb.getId());
                deletedMembers.add(memberInDb);
            }
        });
        Long company = portfolioQueryService.getPortfolioCompany(portfolioId);
        for (MemberBean member : finalMembers) {
            if (!membersInDb.stream().anyMatch(memberInDb -> member.equals(memberInDb))) {
                PortfolioMember addMember = PortfolioMember.builder()
                        .id(IdUtil.generateId())
                        .portfolioId(portfolioId)
                        .userType(member.getUserType().name())
                        .user(member.getUser())
                        .companyId(company)
                        .role(member.getRole())
                        .roleSource(member.getSource().name())
                        .roleType(schema.findRole(member.getSource(), member.getRole()).getType().name())
                        .build();
                portfolioMemberMapper.insert(addMember);
            }
        }

        String user = userService.currentUserName();
        userPortfolioPermissionsService.cacheEvict(portfolioId, user);
    }

    public void deleteByPortfolioRole(Long portfolioId, RoleSource source, String roleKey) {
        PortfolioMemberExample example = new PortfolioMemberExample();
        example.createCriteria().andPortfolioIdEqualTo(portfolioId).andRoleSourceEqualTo(source.name()).andRoleEqualTo(roleKey);
        portfolioMemberMapper.deleteByExample(example);
    }
}
