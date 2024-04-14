package com.ezone.ezproject.modules.plan.service;

import com.ezone.ezbase.iam.bean.GroupUser;
import com.ezone.ezbase.iam.bean.LoginUser;
import com.ezone.ezbase.iam.bean.enums.GroupUserType;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.es.entity.enums.RoleType;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.common.EndpointHelper;
import com.ezone.ezproject.modules.notice.NoticeService;
import com.ezone.ezproject.modules.notice.message.PlanMessageModel;
import com.ezone.ezproject.modules.notice.message.PlansDeletedMessageModel;
import com.ezone.ezproject.modules.project.service.ProjectMemberQueryService;
import com.ezone.ezproject.modules.project.service.ProjectNoticeConfigService;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class PlanNoticeService {

    private UserService userService;

    private EndpointHelper endpointHelper;

    private ProjectQueryService projectQueryService;
    private ProjectMemberQueryService projectMemberQueryService;
    private ProjectNoticeConfigService projectNoticeConfigService;

    private NoticeService noticeService;

    public void saveOrUpdate(Plan plan, boolean isNew) throws IOException {
        ProjectNoticeConfig projectNoticeConfig = projectNoticeConfigService.getProjectNoticeConfig(plan.getProjectId());
        ProjectNoticeConfig.Config planConfig = projectNoticeConfig.getPlanNoticeConfig();
        boolean canNoticeUpdate = !isNew && planConfig.getNotifyTypes().contains(ProjectNoticeConfig.Type.UPDATE);
        boolean canNoticeCreate = isNew && planConfig.getNotifyTypes().contains(ProjectNoticeConfig.Type.CREATE);
        boolean needNotice = planConfig.isOpen() && (canNoticeUpdate || canNoticeCreate);
        if (needNotice) {
            notice(projectQueryService.select(plan.getProjectId()), plan, userService.currentUser(), planConfig, isNew ? ProjectNoticeConfig.Type.CREATE : ProjectNoticeConfig.Type.UPDATE);
        }
    }

    public void delete(Long projectId, List<Plan> plans) throws IOException {
        ProjectNoticeConfig projectNoticeConfig = projectNoticeConfigService.getProjectNoticeConfig(projectId);
        ProjectNoticeConfig.Config planConfig = projectNoticeConfig.getPlanNoticeConfig();
        if (planConfig.isOpen() && planConfig.getNotifyTypes().contains(ProjectNoticeConfig.Type.DELETE)) {
            List<ProjectNoticeConfig.User> receivers = planConfig.getUsers();
            if (ProjectNoticeConfig.UsersType.PROJECT_MEMBERS.equals(planConfig.getUsersType())) {
                receivers = projectMemberQueryService.select(projectId).stream()
                        .filter(member -> !member.getRole().equals(RoleType.GUEST.name()))
                        .map(member -> ProjectNoticeConfig.User.builder()
                                .userType(GroupUserType.valueOf(member.getUserType()))
                                .user(member.getUser())
                                .build())
                        .collect(Collectors.toList());
            }
            if (CollectionUtils.isEmpty(receivers)) {
                return;
            }
            noticeDeleted(projectQueryService.select(projectId), plans, userService.currentUser(), receivers);
        }
    }

    private void notice(Project project, Plan plan, LoginUser sender, ProjectNoticeConfig.Config noticeConfig, ProjectNoticeConfig.Type type) {
        List<ProjectNoticeConfig.User> receivers = noticeConfig.getUsers();
        if (ProjectNoticeConfig.UsersType.PROJECT_MEMBERS.equals(noticeConfig.getUsersType())) {
            receivers = projectMemberQueryService.select(plan.getProjectId()).stream()
                    .filter(member -> !member.getRole().equals(RoleType.GUEST.name()))
                    .map(member -> ProjectNoticeConfig.User.builder()
                            .userType(GroupUserType.valueOf(member.getUserType()))
                            .user(member.getUser())
                            .build())
                    .collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(receivers)) {
            return;
        }
        Set<GroupUser> msgReceivers = receivers.stream().map(ProjectNoticeConfig.User::toGroupUser).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(msgReceivers)) {
            return;
        }
        PlanMessageModel messageModel = PlanMessageModel.builder()
                .nickName(userService.userNickOrName(project.getCompanyId(),sender))
                .sender(sender.getUsername())
                .endpointHelper(endpointHelper)
                .project(project)
                .operationType(type)
                .plan(plan)
                .build();
        noticeService.sendMessageModelToGroupUsers(project.getCompanyId(), sender.getUsername(), msgReceivers, messageModel);
    }

    private void noticeDeleted(Project project, @NotNull List<Plan> plans, LoginUser sender, @NotNull Collection<ProjectNoticeConfig.User> receivers) {
        List<String> planNames = plans.stream().map(Plan::getName).collect(Collectors.toList());
        Set<GroupUser> msgReceivers = receivers.stream().map(ProjectNoticeConfig.User::toGroupUser).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(msgReceivers)){
            return;
        }
        PlansDeletedMessageModel messageModel = PlansDeletedMessageModel.builder()
                .nickName(userService.userNickOrName(project.getCompanyId(),sender))
                .sender(sender.getUsername())
                .endpointHelper(endpointHelper)
                .project(project)
                .deletedPlanNames(planNames)
                .build();
        noticeService.sendMessageModelToGroupUsers(project.getCompanyId(), sender.getUsername(), msgReceivers, messageModel);
    }
}
