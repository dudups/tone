package com.ezone.ezproject.modules.card.service;

import com.ezone.ezbase.iam.bean.BaseUser;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.card.event.model.EventMsg;
import com.ezone.ezproject.modules.card.event.model.UpdateEventMsg;
import com.ezone.ezproject.modules.common.EndpointHelper;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService;
import com.ezone.ezproject.modules.event.events.CardUpdateEvent;
import com.ezone.ezproject.modules.event.events.CardsUpdateEvent;
import com.ezone.ezproject.modules.notice.NoticeService;
import com.ezone.ezproject.modules.notice.message.CardMemberFieldChangeMessageModel;
import com.ezone.ezproject.modules.project.service.ProjectNoticeConfigService;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
@Service
@Slf4j
@AllArgsConstructor
public class CardMemberChangedNoticeService {
    private NoticeService noticeService;

    private EndpointHelper endpointHelper;

    private ProjectSchemaQueryService projectSchemaQueryService;

    private ProjectNoticeConfigService projectNoticeConfigService;

    private ProjectQueryService projectQueryService;

    private CompanyProjectSchemaQueryService companyProjectSchemaQueryService;

    private UserService userService;

    /**
     * 这些卡片字段中用户变化时，无需通知变动相关人员
     */
    public static final List<String> NOT_ATTENTION_CARD_SCHEMA_FIELDS = Arrays.asList("at_users", "create_user", "last_modify_user", "watch_users", "follow_users");

    @Async
    public void noticeCardUpdate(CardUpdateEvent event) {
        EventMsg eventMsg = event.getCardEvent().getEventMsg();
        if (eventMsg instanceof UpdateEventMsg) {
            UpdateEventMsg updateEventMsg = (UpdateEventMsg) eventMsg;
            doNoticeCardUpdate(event.getCard(), event.getCardEvent().getCardDetail(), updateEventMsg, event.getUser());
        }
    }

    @Async
    public void noticeCardsUpdate(CardsUpdateEvent event) {
        noticeCardsUpdate(event.getProject(), event.getCardEvents(), event.getUser());
    }

    private void noticeCardsUpdate(Project project, List<CardEvent> cardEvents, String operator) {
//
        for (CardEvent cardEvent : cardEvents) {
            EventMsg cardEventMsg = cardEvent.getEventMsg();
            if (cardEventMsg instanceof UpdateEventMsg) {
                Map<String, Object> cardDetail = cardEvent.getCardDetail();
                doNoticeCardUpdate(project, cardDetail, (UpdateEventMsg) cardEventMsg, operator);
            }
        }
    }

    private void doNoticeCardUpdate(Card card, Map<String, Object> cardDetail, UpdateEventMsg updateEventMsg, String operator) {
        if (updateEventMsg == null) {
            return;
        }
        Long projectId = card.getProjectId();
        Long companyId = card.getCompanyId();
        Project project = projectQueryService.select(projectId);
        sendUpdateMessage(updateEventMsg, companyId, project, cardDetail, operator);
    }

    private void doNoticeCardUpdate(Project project, Map<String, Object> cardDetail, UpdateEventMsg updateEventMsg, String operator) {
        if (updateEventMsg == null) {
            return;
        }
        Long companyId = project.getCompanyId();
        sendUpdateMessage(updateEventMsg, companyId, project, cardDetail, operator);
    }

    private void sendUpdateMessage(UpdateEventMsg updateEventMsg, Long companyId, Project project, Map<String, Object> cardDetail, String operator) {
        ProjectNoticeConfig projectNoticeConfig = null;
        Long projectId = project.getId();
        try {
            projectNoticeConfig = projectNoticeConfigService.getProjectNoticeConfig(projectId);
        } catch (IOException e) {
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        ProjectNoticeConfig.Config cardNoticeConfig = projectNoticeConfig.getCardNoticeConfig();
        if (!cardNoticeConfig.isOpen() || !cardNoticeConfig.getNotifyTypes().contains(ProjectNoticeConfig.Type.UPDATE)) {
            return;
        }
        List<UpdateEventMsg.FieldDetailMsg> fieldDetailMsgs = updateEventMsg.getFieldDetailMsgs();
        if (fieldDetailMsgs == null) {
            return;
        }
        List<String> userAndMemberFields = projectSchemaQueryService.getUserOrMemberFields(projectId);
        ProjectCardSchema projectCardSchema = projectSchemaQueryService.getProjectCardSchema(projectId);

        for (UpdateEventMsg.FieldDetailMsg fieldDetailMsg : fieldDetailMsgs) {
            boolean needSendMsgField = !NOT_ATTENTION_CARD_SCHEMA_FIELDS.contains(fieldDetailMsg.getFieldKey()) && userAndMemberFields.contains(fieldDetailMsg.getFieldKey());
            List<String> fieldNames = projectCardSchema.fieldNames(Collections.singletonList(fieldDetailMsg.getFieldKey()));
            if (!needSendMsgField || fieldNames.isEmpty()) {
                if (fieldNames.isEmpty()) {
                    log.warn(fieldDetailMsg.getFieldKey() + "字段未获取到名称！");
                }
                continue;
            }
            //人员字段,通知相关人员
            String fromUsers = fieldDetailMsg.getFromMsg();
            String toUsers = fieldDetailMsg.getToMsg();
            List<String> removes = new ArrayList<>();
            List<String> adds = new ArrayList<>();
            CardMemberChangedNoticeService.calcDiff(fromUsers, toUsers, adds, removes);
            removes.remove(operator);
            adds.remove(operator);
            CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(companyId);
            Map<String, String> cardUserNicknames = noticeService.getCardUserNicknames(projectCardSchema, cardDetail);
            if (CollectionUtils.isNotEmpty(removes)) {
                CardMemberFieldChangeMessageModel messageModel = CardMemberFieldChangeMessageModel.builder()
                        .cardDetail(cardDetail)
                        .endpointHelper(endpointHelper)
                        .project(project)
                        .companyCardSchema(companyCardSchema)
                        .projectCardSchema(projectCardSchema)
                        .sender(operator)
                        .nickName(cardUserNicknames.get(operator))
                        .cardUserNickNames(cardUserNicknames)
                        .changedMemberFieldKeys(Collections.singletonList(fieldDetailMsg.getFieldKey()))
                        .memberOperatedType(CardMemberFieldChangeMessageModel.MemberOperatedType.delete)
                        .cardOperatedType(ProjectNoticeConfig.Type.UPDATE)
                        .build();
                noticeService.sendMessageModel(companyId, operator, removes, messageModel);
            }
            if (CollectionUtils.isNotEmpty(adds)) {
                CardMemberFieldChangeMessageModel messageModel = CardMemberFieldChangeMessageModel.builder()
                        .cardDetail(cardDetail)
                        .endpointHelper(endpointHelper)
                        .project(project)
                        .companyCardSchema(companyCardSchema)
                        .projectCardSchema(projectCardSchema)
                        .sender(operator)
                        .nickName(cardUserNicknames.get(operator))
                        .cardUserNickNames(cardUserNicknames)
                        .changedMemberFieldKeys(Collections.singletonList(fieldDetailMsg.getFieldKey()))
                        .memberOperatedType(CardMemberFieldChangeMessageModel.MemberOperatedType.add)
                        .cardOperatedType(ProjectNoticeConfig.Type.UPDATE)
                        .build();
                noticeService.sendMessageModel(companyId, operator, adds, messageModel);
            }
        }
    }

    /**
     * 计算新增与删除用户名，使用adds与removes参数返回。
     *
     * @param fromUsersStr
     * @param toUsersStr
     * @param adds
     * @param removes
     */
    public static void calcDiff(String fromUsersStr, String toUsersStr, @Nonnull List<String> adds, @Nonnull List<String> removes) {
        List<String> fromUsers = StringUtils.isNotEmpty(fromUsersStr) ? Arrays.asList(fromUsersStr.trim().split("\\s+")) : Collections.emptyList();
        List<String> toUsers = StringUtils.isNotEmpty(toUsersStr) ? Arrays.asList(toUsersStr.trim().split("\\s+")) : Collections.emptyList();
        adds.addAll(toUsers.stream().filter(o -> !fromUsers.contains(o)).collect(Collectors.toList()));
        removes.addAll(fromUsers.stream().filter(o -> !toUsers.contains(o)).collect(Collectors.toList()));
    }

}
