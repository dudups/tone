package com.ezone.ezproject.modules.card.service;

import com.ezone.ezbase.iam.bean.BaseUser;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.common.EndpointHelper;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService;
import com.ezone.ezproject.modules.event.events.CardDeleteEvent;
import com.ezone.ezproject.modules.event.events.CardRecoveryEvent;
import com.ezone.ezproject.modules.event.events.CardUpdateEvent;
import com.ezone.ezproject.modules.event.events.CardsCloseStatusEvent;
import com.ezone.ezproject.modules.event.events.CardsDeleteEvent;
import com.ezone.ezproject.modules.event.events.CardsRecoveryEvent;
import com.ezone.ezproject.modules.event.events.CardsUpdateEvent;
import com.ezone.ezproject.modules.notice.NoticeService;
import com.ezone.ezproject.modules.notice.message.CardOperationMessageModel;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 关注卡片用户通知处理服务类
 */
@Service
@Slf4j
@AllArgsConstructor
public class CardWatchNoticeService {
    private NoticeService noticeService;

    private EndpointHelper endpointHelper;

    private UserService userService;

    private ProjectSchemaQueryService projectSchemaQueryService;

    private ProjectQueryService projectQueryService;

    private CompanyProjectSchemaQueryService companyProjectSchemaQueryService;

    @Async
    public void noticeCardUpdate(CardUpdateEvent event) {
        noticeCard(event.getCard(), event.getCardEvent().getCardDetail(), event.getUser(), ProjectNoticeConfig.Type.UPDATE);
    }

    @Async
    public void noticeCardsUpdate(CardsUpdateEvent event) {
        noticeCards(
                event.getProject(),
                event.getCardEvents().stream().map(CardEvent::getCardDetail).collect(Collectors.toList()),
                event.getUser(),
                ProjectNoticeConfig.Type.UPDATE);
    }

    @Async
    public void noticeCardsUpdate(CardsCloseStatusEvent event) {
        noticeCards(
                event.getProject(),
                event.getCardEvents().stream().map(CardEvent::getCardDetail).filter(FieldUtil::getPlanIsActive).collect(Collectors.toList()),
                event.getUser(),
                ProjectNoticeConfig.Type.UPDATE);
    }

    @Async
    public void noticeCardDelete(CardDeleteEvent event) {
        noticeCard(event.getCard(), event.getCardDetail(), event.getUser(), ProjectNoticeConfig.Type.DELETE);
    }

    @Async
    public void noticeCardsDelete(CardsDeleteEvent event) {
        noticeCards(
                event.getProject(),
                event.getCardDetails().values(),
                event.getUser(),
                ProjectNoticeConfig.Type.DELETE);
    }

    @Async
    public void noticeCardRecovery(CardRecoveryEvent event) {
        noticeCard(event.getCard(), event.getCardDetail(), event.getUser(), ProjectNoticeConfig.Type.REVERT);
    }

    @Async
    public void noticeCardsRecovery(CardsRecoveryEvent event) {
        noticeCards(
                event.getProject(),
                event.getCardDetails().values(),
                event.getUser(),
                ProjectNoticeConfig.Type.REVERT);
    }

    private void noticeCards(Project project, Collection<Map<String, Object>> cardDetails, String sender, ProjectNoticeConfig.Type operationType) {
        BaseUser user = userService.user(sender);
        cardDetails.forEach(cardDetail -> {
            List<String> receivers = FieldUtil.getWatchUsers(cardDetail);

            if (CollectionUtils.isEmpty(receivers)) {
                return;
            }
            receivers.remove(sender);
            if (CollectionUtils.isEmpty(receivers)) {
                return;
            }
            ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(project.getId());
            CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(project.getCompanyId());
            Map<String, String> cardUserNicknames = noticeService.getCardUserNicknames(schema, cardDetail);
            String nickName = cardUserNicknames.get(user);

            CardOperationMessageModel messageModel = CardOperationMessageModel.builder()
                    .cardDetail(cardDetail)
                    .companyCardSchema(companyCardSchema)
                    .projectCardSchema(schema)
                    .project(project)
                    .endpointHelper(endpointHelper)
                    .sender(sender)
                    .nickName(nickName)
                    .cardUserNickNames(cardUserNicknames)
                    .operationType(operationType)
                    .build();
            noticeService.sendMessageModel(project.getCompanyId(), sender, receivers, messageModel);
        });
    }

    private void noticeCard(Card card, Map<String, Object> cardDetail, String sender, ProjectNoticeConfig.Type type) {
        List<String> receivers = FieldUtil.getWatchUsers(cardDetail);
        if (CollectionUtils.isEmpty(receivers)) {
            return;
        }
        Project project = projectQueryService.select(card.getProjectId());
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(project.getId());
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(project.getCompanyId());
        Map<String, String> cardUserNicknames = noticeService.getCardUserNicknames(schema, cardDetail);
        String nickName = cardUserNicknames.get(sender);

        CardOperationMessageModel messageModel = CardOperationMessageModel.builder()
                .cardDetail(cardDetail)
                .companyCardSchema(companyCardSchema)
                .projectCardSchema(schema)
                .project(project)
                .endpointHelper(endpointHelper)
                .sender(sender)
                .cardUserNickNames(cardUserNicknames)
                .nickName(nickName)
                .operationType(type)
                .build();
        noticeService.sendMessageModel(project.getCompanyId(), sender, receivers, messageModel);
    }

}
