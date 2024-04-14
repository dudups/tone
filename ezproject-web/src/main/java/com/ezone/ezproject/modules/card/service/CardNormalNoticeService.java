package com.ezone.ezproject.modules.card.service;

import com.ezone.ezbase.iam.bean.BaseUser;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.card.event.model.EventMsg;
import com.ezone.ezproject.modules.card.event.model.UpdateEventMsg;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService;
import com.ezone.ezproject.modules.event.events.CardDeleteEvent;
import com.ezone.ezproject.modules.event.events.CardRecoveryEvent;
import com.ezone.ezproject.modules.event.events.CardUpdateEvent;
import com.ezone.ezproject.modules.event.events.CardsDeleteEvent;
import com.ezone.ezproject.modules.event.events.CardsRecoveryEvent;
import com.ezone.ezproject.modules.event.events.CardsUpdateEvent;
import com.ezone.ezproject.modules.notice.MergeSendNoticeHelper;
import com.ezone.ezproject.modules.notice.NoticeService;
import com.ezone.ezproject.modules.notice.bean.NoticeType;
import com.ezone.ezproject.modules.project.service.ProjectNoticeConfigService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 卡片通用通知 (新建、更新、删除、还原时通知成员字段中的成员)
 */
@Service
@Slf4j
@AllArgsConstructor
public class CardNormalNoticeService {

    private NoticeService noticeService;

    private UserService userService;

    private ProjectSchemaQueryService projectSchemaQueryService;

    private ProjectNoticeConfigService projectNoticeConfigService;

    private CompanyProjectSchemaQueryService companyProjectSchemaQueryService;

    @Async
    public void noticeCardUpdate(CardUpdateEvent event) {
        boolean isOpen = needNotice(event.getCard().getProjectId(), ProjectNoticeConfig.Type.UPDATE);
        //状态通知单独处理，过滤掉状态通知。
        EventMsg eventMsg = event.getCardEvent().getEventMsg();
        if (eventMsg instanceof UpdateEventMsg) {
            UpdateEventMsg updateEventMsg = (UpdateEventMsg)eventMsg;
            List<UpdateEventMsg.FieldDetailMsg> fieldDetailMsgs = updateEventMsg.getFieldDetailMsgs();
            for (UpdateEventMsg.FieldDetailMsg fieldDetailMsg : fieldDetailMsgs) {
                if (CardField.STATUS.equals(fieldDetailMsg.getFieldKey())) {
                    return;
                }
            }
        }

        if (isOpen) {
            noticeCardUpdate(event.getCard(), event.getCardEvent().getCardDetail(), event.getUser(), ProjectNoticeConfig.Type.UPDATE, true, event.getCardEvent().getEventMsg());
        }
    }

    @Async
    public void noticeCardsUpdate(CardsUpdateEvent event) {
        if (needNotice(event.getProject().getId(), ProjectNoticeConfig.Type.UPDATE)) {
            BaseUser user = userService.user(event.getUser());
            noticeCardsUpdate(
                    event.getProject(),
                    event.getCardEvents(),
                    user,
                    ProjectNoticeConfig.Type.UPDATE, true);
        }
    }

    @Async
    public void noticeCardDelete(CardDeleteEvent event) {
        if (needNotice(event.getCard().getProjectId(), ProjectNoticeConfig.Type.DELETE)) {
            noticeCard(event.getCard(), event.getCardDetail(), event.getUser(), ProjectNoticeConfig.Type.DELETE);
        }
    }

    @Async
    public void noticeCardsDelete(CardsDeleteEvent event) {
        if (needNotice(event.getProject().getId(), ProjectNoticeConfig.Type.DELETE)) {
            noticeCards(
                    event.getProject(),
                    event.getCardDetails(),
                    event.getUser(),
                    ProjectNoticeConfig.Type.DELETE);
        }
    }

    @Async
    public void noticeCardRecovery(CardRecoveryEvent event) {
        if (needNotice(event.getCard().getProjectId(), ProjectNoticeConfig.Type.REVERT)) {
            noticeCard(event.getCard(), event.getCardDetail(), event.getUser(), ProjectNoticeConfig.Type.REVERT);
        }
    }

    @Async
    public void noticeCardsRecovery(CardsRecoveryEvent event) {
        if (needNotice(event.getProject().getId(), ProjectNoticeConfig.Type.REVERT)) {
            noticeCards(
                    event.getProject(),
                    event.getCardDetails(),
                    event.getUser(),
                    ProjectNoticeConfig.Type.REVERT);
        }
    }

    private void noticeCards(Project project, Map<Long, Map<String, Object>> cardDetails, String sender, ProjectNoticeConfig.Type operationType) {
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(project.getId());
        BaseUser user = userService.user(sender);
        MergeSendNoticeHelper noticeHelper = MergeSendNoticeHelper.builder()
                .noticeType(NoticeType.DEFAULT)
                .sender(user)
                .noticeService(noticeService)
                .companyId(project.getCompanyId())
                .cardUrlsLimitSize(2)
                .operationType(operationType)
                .userOrMemberFields(projectSchemaQueryService.getUserOrMemberFields(project.getId()))
                .project(project)
                .build();
        cardDetails.forEach((cardId, cardDetail) -> {
            Set<String> noticeUsers = CardHelper.getUsersForReceiveNormalMsg(schema, cardDetail);
            if (CollectionUtils.isEmpty(noticeUsers)) {
                return;
            }
            noticeHelper.add(noticeUsers, cardId, cardDetail);
        });
        noticeHelper.send();
    }


    private void noticeCardsUpdate(Project project, List<CardEvent> cardEvents, BaseUser sender, ProjectNoticeConfig.Type operationType, boolean deleteModifyUser) {
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(project.getId());
        MergeSendNoticeHelper defaultNoticeHelper = MergeSendNoticeHelper.builder()
                .noticeType(NoticeType.DEFAULT)
                .sender(sender)
                .noticeService(noticeService)
                .companyId(project.getCompanyId())
                .cardUrlsLimitSize(2)
                .operationType(operationType)
                .userOrMemberFields(projectSchemaQueryService.getUserOrMemberFields(project.getId()))
                .project(project)
                .build();

        cardEvents.forEach(cardEvent -> {
            Map<String, Object> cardDetail = cardEvent.getCardDetail();
            Set<String> receivers = CardHelper.getUsersForReceiveNormalMsg(schema, cardDetail);
            if (CollectionUtils.isEmpty(receivers)) {
                return;
            }
            if (deleteModifyUser) {
                List<String> userOfFieldUpdate = getUserOfFieldUpdate(projectSchemaQueryService.getUserOrMemberFields(project.getId()), cardEvent.getEventMsg());
                userOfFieldUpdate.forEach(receivers::remove);
            }
            defaultNoticeHelper.add(receivers, cardEvent.getCardId(), cardDetail);
        });
        defaultNoticeHelper.send();
    }

    private void noticeCard(Card card, Map<String, Object> cardDetail, String sender, ProjectNoticeConfig.Type operationType) {
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(card.getProjectId());
        Set<String> receivers = CardHelper.getUsersForReceiveNormalMsg(schema, cardDetail);
        if (CollectionUtils.isEmpty(receivers)) {
            return;
        }
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(card.getCompanyId());
        noticeService.doNotice(card, cardDetail, companyCardSchema, schema, sender, operationType, receivers);
    }

    private void noticeCardUpdate(Card card, Map<String, Object> cardDetail, String sender, ProjectNoticeConfig.Type operationType, boolean deleteModifyUser, EventMsg eventMsg) {
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(card.getProjectId());
        Set<String> receivers = CardHelper.getUsersForReceiveNormalMsg(schema, cardDetail);
        if (deleteModifyUser) {
            List<String> userOfFieldUpdate = getUserOfFieldUpdate(projectSchemaQueryService.getUserOrMemberFields(card.getProjectId()), eventMsg);
            userOfFieldUpdate.forEach(receivers::remove);
        }
        if (CollectionUtils.isEmpty(receivers)) {
            return;
        }
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(card.getCompanyId());
        noticeService.doNotice(card, cardDetail, companyCardSchema, schema, sender, operationType, receivers);
    }

    /**
     * 获取卡片变更时，成员字段中变更的用户
     *
     * @param userOrMemberFields 成员字段
     * @return Map<String, List < String>>  key-[remove与add]  value-用户列表。
     */
    private static List<String> getUserOfFieldUpdate(List<String> userOrMemberFields, EventMsg eventMsg) {
        if (!(eventMsg instanceof UpdateEventMsg)) {
            return Collections.emptyList();
        }
        List<UpdateEventMsg.FieldDetailMsg> fieldDetailMsgs = ((UpdateEventMsg) eventMsg).getFieldDetailMsgs();
        List<String> removes = new ArrayList<>();
        List<String> adds = new ArrayList<>();
        if (fieldDetailMsgs != null) {
            for (UpdateEventMsg.FieldDetailMsg fieldDetailMsg : fieldDetailMsgs) {
                String modifiedFieldKey = fieldDetailMsg.getFieldKey();
                boolean isChangeUserNoticeField = userOrMemberFields.contains(modifiedFieldKey);
                if (!isChangeUserNoticeField) {
                    continue;
                }
                String fromUsers = fieldDetailMsg.getFromMsg();
                String toUsers = fieldDetailMsg.getToMsg();

                CardMemberChangedNoticeService.calcDiff(fromUsers, toUsers, adds, removes);
            }
        }
        List<String> result = new ArrayList<>();
        result.addAll(adds);
        result.addAll(removes);
        return result;
    }

    private boolean needNotice(Long projectId, ProjectNoticeConfig.Type configType) {
        ProjectNoticeConfig projectNoticeConfig;
        try {
            projectNoticeConfig = projectNoticeConfigService.getProjectNoticeConfig(projectId);
        } catch (IOException e) {
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        ProjectNoticeConfig.Config cardNoticeConfig = projectNoticeConfig.getCardNoticeConfig();
        return cardNoticeConfig.isOpen() && cardNoticeConfig.getNotifyTypes().contains(configType);
    }
}
