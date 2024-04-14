package com.ezone.ezproject.modules.project.service;

import com.ezone.ezbase.iam.bean.enums.GroupUserType;
import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.ProjectMember;
import com.ezone.ezproject.dal.entity.ProjectMemberExample;
import com.ezone.ezproject.dal.mapper.ProjectMemberMapper;
import com.ezone.ezproject.es.entity.ProjectRole;
import com.ezone.ezproject.es.entity.ProjectRoleSchema;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.es.entity.enums.RoleType;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.common.OperationContext;
import com.ezone.ezproject.modules.log.service.OperationLogCmdService;
import com.ezone.ezproject.modules.permission.UserProjectPermissionsService;
import com.ezone.ezproject.modules.project.bean.MemberBean;
import com.ezone.ezproject.modules.project.bean.RoleKeySource;
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory;
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
public class ProjectMemberCmdService {
    private ProjectMemberMapper projectMemberMapper;

    private ProjectMemberQueryService projectMemberQueryService;
    private ProjectQueryService projectQueryService;

    private ProjectSchemaQueryService projectSchemaQueryService;

    private UserService userService;

    private UserProjectPermissionsService userProjectPermissionsService;

    private OperationLogCmdService operationLogCmdService;

    public void initMembers(Long projectId, String adminUser, String guestUser) {
        Long company = projectQueryService.getProjectCompany(projectId);
        if (StringUtils.isNotEmpty(adminUser)) {
            projectMemberMapper.insert(ProjectMember.builder()
                    .id(IdUtil.generateId())
                    .projectId(projectId)
                    .userType(GroupUserType.USER.name())
                    .user(adminUser)
                    .companyId(company)
                    .role(ProjectRole.ADMIN)
                    .roleSource(RoleSource.SYS.name())
                    .roleType(RoleType.ADMIN.name())
                    .build());
        }
        if (StringUtils.isNotEmpty(guestUser)) {
            projectMemberMapper.insert(ProjectMember.builder()
                    .id(IdUtil.generateId())
                    .projectId(projectId)
                    .userType(GroupUserType.GROUP.name())
                    .user(guestUser)
                    .companyId(company)
                    .role(ProjectRole.GUEST)
                    .roleSource(RoleSource.SYS.name())
                    .roleType(RoleType.ADMIN.name())
                    .build());
        }
    }

    public void setProjectMembers(Long projectId, List<MemberBean> members) throws IOException {
        ProjectRoleSchema schema = projectSchemaQueryService.getProjectRoleSchema(projectId);
        if (CollectionUtils.isEmpty(members) || members.stream().noneMatch(memberBean -> memberBean.getRole().equals(RoleType.ADMIN.name()))) {
            throw new CodedException(HttpStatus.BAD_REQUEST, "至少保留一名管理员！");
        }
        if (CollectionUtils.isNotEmpty(members)) {
            members = members.stream().filter(member -> schema.findRole(member.getSource(), member.getRole()) != null).collect(Collectors.toList());
        }
        List<MemberBean> finalMembers = members;
        List<ProjectMember> membersInDb = projectMemberQueryService.select(projectId);
        List<ProjectMember> deletedMembers = new ArrayList<>();
        membersInDb.forEach(memberInDb -> {
            if (!finalMembers.stream().anyMatch(member -> member.equals(memberInDb))) {
                projectMemberMapper.deleteByPrimaryKey(memberInDb.getId());
                deletedMembers.add(memberInDb);
            }
        });
        List<ProjectMember> addMembers = new ArrayList<>();
        Long company = projectQueryService.getProjectCompany(projectId);
        for (MemberBean member : finalMembers) {
            if (!membersInDb.stream().anyMatch(memberInDb -> member.equals(memberInDb))) {
                ProjectMember addMember = ProjectMember.builder()
                        .id(IdUtil.generateId())
                        .projectId(projectId)
                        .userType(member.getUserType().name())
                        .user(member.getUser())
                        .companyId(company)
                        .role(member.getRole())
                        .roleSource(member.getSource().name())
                        .roleType(schema.findRole(member.getSource(), member.getRole()).getType().name())
                        .build();
                projectMemberMapper.insert(addMember);
                addMembers.add(addMember);
            }
        }
        SpringBeanFactory.getBean(ProjectSchemaCmdService.class).forkCompanyRole(projectId, addMembers.stream()
                .filter(m -> RoleSource.COMPANY.name().equals(m.getRoleSource())).map(m -> m.getRole())
                .distinct()
                .collect(Collectors.toList()));
        String user = userService.currentUserName();
        OperationContext opContext = OperationContext.instance(user);
        userProjectPermissionsService.cacheEvict(projectId, user);
        operationLogCmdService.updateProjectMember(opContext, projectId, deletedMembers, addMembers);
    }

    public void deleteByProjectRole(Long projectId, RoleSource source, String roleKey) {
        ProjectMemberExample example = new ProjectMemberExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andRoleSourceEqualTo(source.name()).andRoleEqualTo(roleKey);
        projectMemberMapper.deleteByExample(example);
    }

    public void deleteByCompanyRole(Long companyId, RoleSource source, String roleKey) {
        ProjectMemberExample example = new ProjectMemberExample();
        example.createCriteria().andCompanyIdEqualTo(companyId).andRoleSourceEqualTo(source.name()).andRoleEqualTo(roleKey);
        projectMemberMapper.deleteByExample(example);
    }

    public void deleteByProjectId(Long projectId) {
        ProjectMemberExample example = new ProjectMemberExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        projectMemberMapper.deleteByExample(example);
    }

    /**
     * 将数据库中sourceRole中在targetRole中不存在的角色成员迁移过去。
     * @param projectId
     * @param sourceRole
     * @param targetRole
     */
    public void migrateRoleUsers(Long projectId, RoleKeySource sourceRole, RoleKeySource targetRole) {
        if (targetRole == null || sourceRole == null) {
            return;
        }
        List<ProjectMember> members = projectMemberQueryService.select(projectId);
        List<ProjectMember> sourceMembers = new ArrayList<>();
        List<ProjectMember> targetMembers = new ArrayList<>();
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
                )){
                    sourceMember.setRole(targetRole.getRole());
                    sourceMember.setRoleSource(targetRole.getRoleSource().name());
                    projectMemberMapper.updateByPrimaryKeySelective(sourceMember);
                }
            });
        }
    }
}
