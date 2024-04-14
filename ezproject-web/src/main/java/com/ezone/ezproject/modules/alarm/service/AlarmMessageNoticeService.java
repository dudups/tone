package com.ezone.ezproject.modules.alarm.service;

import com.ezone.ezbase.iam.bean.GroupUser;
import com.ezone.ezbase.iam.bean.enums.GroupUserType;
import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.CardAlarmNoticePlan;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.ProjectMember;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.ProjectAlarmExt;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.enums.FieldType;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import com.ezone.ezproject.es.entity.enums.RoleType;
import com.ezone.ezproject.modules.alarm.bean.AlarmItem;
import com.ezone.ezproject.modules.alarm.bean.CardAlarmItem;
import com.ezone.ezproject.modules.alarm.bean.NoticeFieldUsersConfig;
import com.ezone.ezproject.modules.alarm.bean.NoticeRoleUserConfig;
import com.ezone.ezproject.modules.alarm.bean.NoticeSpecUsersConfig;
import com.ezone.ezproject.modules.alarm.bean.NoticeUserConfig;
import com.ezone.ezproject.modules.alarm.bean.PlanAlarmItem;
import com.ezone.ezproject.modules.alarm.bean.ProjectAlarmItem;
import com.ezone.ezproject.modules.card.bean.CardBean;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.service.CardSearchService;
import com.ezone.ezproject.modules.common.EndpointHelper;
import com.ezone.ezproject.modules.notice.NoticeService;
import com.ezone.ezproject.modules.notice.message.CardAlarmMessageModel;
import com.ezone.ezproject.modules.notice.message.PlanAlarmMessageModel;
import com.ezone.ezproject.modules.notice.message.ProjectAlarmMessageModel;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import com.ezone.ezproject.modules.project.bean.ProjectExt;
import com.ezone.ezproject.modules.project.service.ProjectMemberQueryService;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Transactional
@Service
@Slf4j
public class AlarmMessageNoticeService {

    private AlarmConfigQueryService alarmQueryService;
    private ProjectSchemaQueryService schemaQueryService;
    private CardAlarmNoticePlanService noticePlanService;
    private CardSearchService cardSearchService;
    private AlarmNoticeService alarmNoticeService;
    private ProjectQueryService projectQueryService;
    private CardAlarmNoticePlanService cardAlarmNoticePlanService;
    private NoticeService noticeService;
    private PlanQueryService planQueryService;
    private EndpointHelper endpointHelper;
    private ProjectMemberQueryService projectMemberQueryService;

    public static final String SYS_ALARM_SEND_NAME = "system-alarm";

    public void sendCardAlarmNotice(Long projectId) {
        Date now = new Date();
        int minute = (int) (now.getTime() / 1000 / 60);
        List<CardAlarmNoticePlan> cardAlarmNoticePlans = cardAlarmNoticePlanService.searchBySendTime(projectId, minute);
        if (CollectionUtils.isNotEmpty(cardAlarmNoticePlans)) {
            List<ProjectAlarmExt> projectAlarms = alarmQueryService.getProjectAlarms(projectId, true);
            if (CollectionUtils.isEmpty(projectAlarms) || projectAlarms.stream().noneMatch(alarm -> alarm.getAlarmItem() instanceof CardAlarmItem)) {
                return;
            }
            Set<Long> projectNoticeCardIds = cardAlarmNoticePlans.stream().map(CardAlarmNoticePlan::getCardId).collect(Collectors.toSet());
            ProjectCardSchema projectCardSchema = schemaQueryService.getProjectCardSchema(projectId);
            String[] fields = getQueryFieldKeys(projectCardSchema);
            try {
                TotalBean<CardBean> cardBeanTotalBean = cardSearchService.search(projectId, new ArrayList<>(projectNoticeCardIds), fields);
                Map<Long, Map<String, Object>> cardDetailMap = cardBeanTotalBean.getList().stream().collect(Collectors.toMap(CardBean::getId, CardBean::getCard, (first, second) -> first));
                Map<Long, ProjectAlarmExt> projectAlarmMap = projectAlarms.stream().collect(Collectors.toMap(ProjectAlarmExt::getId, Function.identity(), (first, second) -> first));
                cardAlarmNoticePlans.forEach(noticePlan -> doSendCardAlarmNotice(projectId, projectCardSchema, cardDetailMap, projectAlarmMap, noticePlan, noticePlanService::deleteFlag));
            } catch (IOException e) {
                log.error("[sendAlarmNotice][error][" + e.getMessage() + "]", e);
            }
        }
    }


    private void doSendCardAlarmNotice(Long projectId, ProjectCardSchema projectCardSchema, Map<Long, Map<String, Object>> cardDetailMap,
                                       Map<Long, ProjectAlarmExt> projectAlarmMap, CardAlarmNoticePlan noticePlan, Consumer<CardAlarmNoticePlan> afterSendFunction) {
        Long cardId = noticePlan.getCardId();
        Long alarmId = noticePlan.getAlarmId();
        Map<String, Object> cardDetail = cardDetailMap.get(cardId);
        ProjectAlarmExt projectAlarm = projectAlarmMap.get(alarmId);
        //卡片类型字段启用等等验证。
        if ( !alarmNoticeService.validNoticePlan(projectAlarm, cardId, cardDetail, projectCardSchema, noticePlan)) {
            return;
        }
        List<GroupUser> noticeUsers = getNoticeUsers(cardDetail, projectAlarm);
        sendMessage(cardDetail, projectId, noticeUsers, projectAlarm);
        afterSendFunction.accept(noticePlan);
    }


    private void sendMessage(Map<String, Object> cardDetail, Long projectId, List<GroupUser> noticeUsers, ProjectAlarmExt projectAlarm) {
        if (CollectionUtils.isEmpty(noticeUsers)) {
            return;
        }
        noticeUsers = noticeUsers.stream().distinct().collect(Collectors.toList());
        Project project = projectQueryService.select(projectId);
        ProjectCardSchema projectCardSchema = schemaQueryService.getProjectCardSchema(projectId);
        CardAlarmMessageModel messageModel = CardAlarmMessageModel.builder()
                .cardDetail(cardDetail)
                .project(project)
                .alarmItem(projectAlarm.getAlarmItem())
                .endpointHelper(endpointHelper)
                .projectCardSchema(projectCardSchema)
                .build();
        noticeService.sendMessageModelForGroupUser(project.getCompanyId(), SYS_ALARM_SEND_NAME, noticeUsers, messageModel);
    }


    private void sendMessage(Long projectId, Plan plan, List<GroupUser> noticeUsers, ProjectAlarmExt projectAlarm) {
        if (CollectionUtils.isEmpty(noticeUsers)) {
            return;
        }
        noticeUsers = noticeUsers.stream().distinct().collect(Collectors.toList());
        Project project = projectQueryService.select(projectId);
        PlanAlarmMessageModel messageModel = PlanAlarmMessageModel.builder()
                .project(project)
                .plan(plan)
                .alarmItem(projectAlarm.getAlarmItem())
                .endpointHelper(endpointHelper)
                .build();
        noticeService.sendMessageModelForGroupUser(project.getCompanyId(), SYS_ALARM_SEND_NAME, noticeUsers, messageModel);
    }

    private void sendMessage(Project project, List<GroupUser> noticeUsers, ProjectAlarmExt projectAlarm) {
        if (CollectionUtils.isEmpty(noticeUsers)) {
            return;
        }
        noticeUsers = noticeUsers.stream().distinct().collect(Collectors.toList());
        ProjectAlarmMessageModel messageModel = ProjectAlarmMessageModel.builder()
                .project(project)
                .alarmItem(projectAlarm.getAlarmItem())
                .endpointHelper(endpointHelper)
                .build();
        noticeService.sendMessageModelForGroupUser(project.getCompanyId(), SYS_ALARM_SEND_NAME, noticeUsers, messageModel);
    }

    private String[] getQueryFieldKeys(ProjectCardSchema projectCardSchema) {
        List<String> dateFieldKeys = projectCardSchema.getFields().stream()
                .filter(cardField -> cardField.getType().equals(FieldType.DATE) || cardField.getType().equals(FieldType.DATE_TIME)
                        || cardField.getType().equals(FieldType.USER) || cardField.getType().equals(FieldType.USERS)
                        || cardField.getType().equals(FieldType.MEMBERS) || cardField.getType().equals(FieldType.MEMBER))
                .map(CardField::getKey).collect(Collectors.toList());
        dateFieldKeys.add(CardField.TYPE);
        dateFieldKeys.add(CardField.TITLE);
        dateFieldKeys.add(CardField.SEQ_NUM);
        return dateFieldKeys.toArray(new String[0]);
    }

    private List<GroupUser> getNoticeUsers(Map<String, Object> cardDetail, ProjectAlarmExt projectAlarm) {
        List<GroupUser> noticeUsers = new ArrayList<>();
        List<NoticeUserConfig> warningUsers = projectAlarm.getAlarmItem().getWarningUsers();
        addRoleAndSpecificUser(noticeUsers, projectAlarm.getProjectId(), FieldUtil.getCreateUser(cardDetail), warningUsers);
        warningUsers.forEach(noticeUserConfig -> {
            if (noticeUserConfig instanceof NoticeFieldUsersConfig) {
                NoticeFieldUsersConfig fieldUsersConfig = (NoticeFieldUsersConfig) noticeUserConfig;
                List<String> userFields = fieldUsersConfig.getUserFields();
                if (CollectionUtils.isNotEmpty(userFields)) {
                    userFields.forEach(userField -> {
                        List<String> users = FieldUtil.getUserTypeFieldValues(cardDetail, userField);
                        if (CollectionUtils.isNotEmpty(users)) {
                            noticeUsers.addAll(users.stream().map(user -> new GroupUser(user, GroupUserType.USER)).collect(Collectors.toList()));
                        }
                    });
                }
            }
        });
        return noticeUsers;
    }

    private List<GroupUser> getNoticeUsers(Plan plan, ProjectAlarmExt projectAlarm) {
        List<GroupUser> noticeUsers = new ArrayList<>();
        Long projectId = plan.getProjectId();
        String creator = plan.getCreateUser();
        List<NoticeUserConfig> warningUsers = projectAlarm.getAlarmItem().getWarningUsers();
        addRoleAndSpecificUser(noticeUsers, projectId, creator, warningUsers);
        return noticeUsers;
    }

    private List<GroupUser> getNoticeUsers(ProjectExt projectExt, ProjectAlarmExt projectAlarm) {
        List<GroupUser> noticeUsers = new ArrayList<>();
        List<NoticeUserConfig> warningUsers = projectAlarm.getAlarmItem().getWarningUsers();
        addRoleAndSpecificUser(noticeUsers, projectExt.getId(), projectExt.getCreateUser(), warningUsers);
        return noticeUsers;
    }

    private void addRoleAndSpecificUser(List<GroupUser> noticeUsers, Long projectId, String creator, List<NoticeUserConfig> warningUsersConfigs) {
        warningUsersConfigs.forEach(noticeUserConfig -> {
            if (noticeUserConfig instanceof NoticeSpecUsersConfig) {
                NoticeSpecUsersConfig specUsersConfig = (NoticeSpecUsersConfig) noticeUserConfig;
                noticeUsers.addAll(specUsersConfig.getUsers());
            } else if (noticeUserConfig instanceof NoticeRoleUserConfig) {
                ((NoticeRoleUserConfig) noticeUserConfig).getRoles().forEach(role -> {
                    switch (role) {
                        case admin:
                            List<ProjectMember> projectMembers = projectMemberQueryService.select(projectId, RoleSource.SYS, RoleType.ADMIN.name());
                            if (CollectionUtils.isNotEmpty(projectMembers)) {
                                projectMembers.forEach(projectMember -> noticeUsers.add(new GroupUser(projectMember.getUser(), GroupUserType.valueOf(projectMember.getUserType()))));
                            }
                            break;
                        case creator:
                            noticeUsers.add(new GroupUser(creator, GroupUserType.USER));
                            break;
                        case noGuest:
                            List<ProjectMember> allProjectMembers = projectMemberQueryService.select(projectId);
                            if (CollectionUtils.isNotEmpty(allProjectMembers)) {
                                allProjectMembers.forEach(projectMember -> noticeUsers.add(new GroupUser(projectMember.getUser(), GroupUserType.valueOf(projectMember.getUserType()))));
                            }
                            break;
                        default:
                            throw new CodedException(HttpStatus.BAD_REQUEST, "不支持的角色类型");
                    }
                });
            }
        });
    }

    public void sendProjectAndPlanAlarmNotice(int minute) {
        int pageSize = 1000;
        int pageNumber = 1;
        do {
            List<ProjectAlarmExt> projectAlarms = alarmQueryService.getProjectAlarms(true, Arrays.asList(AlarmItem.Type.planAlarm, AlarmItem.Type.projectAlarm), pageNumber, pageSize);
            if (CollectionUtils.isEmpty(projectAlarms)) {
                break;
            }
            List<ProjectAlarmExt> projectOrPlanAlarms = projectAlarms.stream().filter(alarm -> !(alarm.getAlarmItem() instanceof CardAlarmItem)).collect(Collectors.toList());
            Map<Long, List<ProjectAlarmExt>> projectOrPlanAlarmMap = projectOrPlanAlarms.stream().collect(Collectors.groupingBy(ProjectAlarmExt::getProjectId));
            projectOrPlanAlarmMap.forEach((projectId, alarmConfigs) -> {
                Project project = projectQueryService.select(projectId);
                try {
                    ProjectExt projectExt = new ProjectExt(project, projectQueryService.selectExtend(projectId));
                    alarmConfigs.forEach(alarmConfig -> {
                        AlarmItem alarmItem = alarmConfig.getAlarmItem();
                        if (alarmItem instanceof PlanAlarmItem) {
                            sendPlanAlarmNotice(minute, projectId, alarmConfig);
                        } else if (alarmItem instanceof ProjectAlarmItem) {
                            sendProjectAlarmNotice(minute, projectExt, alarmConfig, (ProjectAlarmItem) alarmItem);
                        }
                    });
                } catch (IOException e) {
                    log.error("[sendProjectAndPlanAlarmNotice][" + " minute :" + minute + "][error][" + e.getMessage() + "]", e);
                }
            });
            pageNumber++;
        } while (true);
    }

    private void sendProjectAlarmNotice(int minute, ProjectExt projectExt, ProjectAlarmExt alarmConfig, ProjectAlarmItem alarmItem) {
        Integer planMinute = alarmNoticeService.getAlarmNoticeTimestampMinute(projectExt, alarmItem);
        if (planMinute != null && planMinute == minute) {
            log.debug("将发送通知：" + projectExt.getName());
            List<GroupUser> noticeUsers = getNoticeUsers(projectExt, alarmConfig);
            sendMessage(projectExt, noticeUsers, alarmConfig);
        }
    }

    private void sendPlanAlarmNotice(int minute, Long projectId, ProjectAlarmExt alarmConfig) {
        List<Plan> activePlans = planQueryService.selectByProjectId(projectId, true);
        if (CollectionUtils.isNotEmpty(activePlans)) {
            for (Plan plan : activePlans) {
                Integer planMinute = alarmNoticeService.getAlarmNoticeTimestampMinute(plan, (PlanAlarmItem) alarmConfig.getAlarmItem());
                if (planMinute != null && planMinute == minute) {
                    List<GroupUser> noticeUsers = getNoticeUsers(plan, alarmConfig);
                    sendMessage(projectId, plan, noticeUsers, alarmConfig);
                }
            }
        }
    }

}
