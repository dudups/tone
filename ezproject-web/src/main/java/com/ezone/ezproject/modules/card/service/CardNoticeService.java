package com.ezone.ezproject.modules.card.service;

import com.ezone.ezbase.iam.bean.LoginUser;
import com.ezone.ezproject.common.transactional.AfterCommit;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.common.EndpointHelper;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService;
import com.ezone.ezproject.modules.notice.NoticeService;
import com.ezone.ezproject.modules.notice.message.CardAtUserMessageModel;
import com.ezone.ezproject.modules.notice.message.CardCommentAtUserMessageModel;
import com.ezone.ezproject.modules.notice.message.CardRemindMessageModel;
import com.ezone.ezproject.modules.notice.message.CardStatusChangeMessageModel;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 卡片通用通知 (催办、状态变更、卡片及评论中at用户等)
 */
@Service
@Slf4j
@AllArgsConstructor
public class CardNoticeService {
    private NoticeService noticeService;

    private EndpointHelper endpointHelper;

    private UserService userService;

    private ProjectQueryService projectQueryService;

    private CompanyProjectSchemaQueryService companyProjectSchemaQueryService;

    @Async
    @AfterCommit
    public void noticeRemind(Card card, Map<String, Object> cardDetail, LoginUser sender, Collection<String> receivers, CompanyCardSchema companyCardSchema,
                             ProjectCardSchema schema, String content) {

        Project project = projectQueryService.select(card.getProjectId());
        Map<String, String> cardUserNicknames = noticeService.getCardUserNicknames(schema, cardDetail);
        CardRemindMessageModel messageModel = CardRemindMessageModel.builder()
                .cardDetail(cardDetail)
                .companyCardSchema(companyCardSchema)
                .projectCardSchema(schema)
                .project(project)
                .endpointHelper(endpointHelper)
                .sender(sender.getUsername())
                .nickName(cardUserNicknames.get(sender.getUsername()))
                .cardUserNickNames(cardUserNicknames)
                .operationType(ProjectNoticeConfig.Type.REMIND)
                .remindContent(content)
                .build();
        noticeService.sendMessageModel(card.getCompanyId(), sender.getUsername(), receivers, messageModel);
    }

    /**
     * 卡片状态批量变化时的通知
     *
     * @param targetCardProps
     * @param sourceCardProps
     * @param schema
     * @param user
     */
    @Async
    @AfterCommit
    public void noticeStatusFlow(Map<Long, Map<String, Object>> targetCardProps, Map<Long, Map<String, Object>> sourceCardProps, ProjectCardSchema schema, LoginUser user) {
        targetCardProps.forEach((cardId, targetCardDetail) -> {
            Map<String, Object> sourceCardDetail = sourceCardProps.get(cardId);
            String type = FieldUtil.toString(sourceCardDetail.get(CardField.TYPE));
            CardType cardType = schema.findCardType(type);
            if (cardType == null) {
                return;
            }
            String oldStatus = FieldUtil.toString(sourceCardDetail.get(CardField.STATUS));
            String status = FieldUtil.toString(targetCardDetail.get(CardField.STATUS));
            CardType.StatusFlowConf statusFlow = cardType.findStatusFlow(oldStatus, status);
            if (statusFlow != null && statusFlow.isNotice()) {
                Set<String> noticeUsers = CardHelper.getUsersFromCardFields(schema, sourceCardDetail);
                Map<String, Object> finalCardDetail = new HashMap<>();
                finalCardDetail.putAll(sourceCardDetail);
                finalCardDetail.putAll(targetCardDetail);
                noticeStatusFlow(finalCardDetail, user.getUsername(), noticeUsers, schema, ProjectNoticeConfig.Type.UPDATE, oldStatus, status);
            }
        });
    }

    /**
     * 单卡片库状态变化通知
     *
     * @param user
     * @param cardDetail
     * @param schema
     * @param oldStatus
     * @param status
     */
    @Async
    @AfterCommit
    public void noticeStatusFlow(String user, Map<String, Object> cardDetail, ProjectCardSchema schema, String oldStatus, String status) {
        if (StringUtils.equals(oldStatus, status)) {
            return;
        }
        String type = FieldUtil.getType(cardDetail);
        CardType cardType = schema.findCardType(type);
        if (cardType == null) {
            return;
        }
        CardType.StatusFlowConf statusFlow = cardType.findStatusFlow(oldStatus, status);
        if (statusFlow != null && statusFlow.isNotice()) {
            Set<String> noticeUsers = CardHelper.getUsersFromCardFields(schema, cardDetail);
            noticeStatusFlow(cardDetail, user, noticeUsers, schema, ProjectNoticeConfig.Type.UPDATE, schema.findCardStatusName(oldStatus), schema.findCardStatusName(status));
        }
    }


    private void noticeStatusFlow(Map<String, Object> cardDetail, String sender, Collection<String> receivers,
                                  ProjectCardSchema schema, ProjectNoticeConfig.Type operationType, String fromStatusName, String toStatusName) {
        receivers.remove(sender);
        if (CollectionUtils.isEmpty(receivers)) {
            return;
        }
        Project project = projectQueryService.select(FieldUtil.getProjectId(cardDetail));
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(project.getCompanyId());
        Map<String, String> cardUserNicknames = noticeService.getCardUserNicknames(schema, cardDetail);
        CardStatusChangeMessageModel messageModel = CardStatusChangeMessageModel.builder()
                .cardDetail(cardDetail)
                .companyCardSchema(companyCardSchema)
                .projectCardSchema(schema)
                .project(project)
                .endpointHelper(endpointHelper)
                .sender(sender)
                .nickName(cardUserNicknames.get(sender))
                .cardUserNickNames(cardUserNicknames)
                .cardDetail(cardDetail)
                .operationType(operationType)
                .fromStatusName(fromStatusName)
                .toStatusName(toStatusName)
                .build();
        noticeService.sendMessageModel(project.getCompanyId(), sender, receivers, messageModel);
    }


    @Async
    @AfterCommit
    public void noticeAtUsersInCard(Card card, Map<String, Object> cardDetail, String sender, Collection<String> receivers,
                                    CompanyCardSchema companyCardSchema, ProjectCardSchema schema, ProjectNoticeConfig.Type operationType) {
        if (CollectionUtils.isEmpty(receivers)) {
            return;
        }
        Project project = projectQueryService.select(card.getProjectId());
        Map<String, String> cardUserNicknames = noticeService.getCardUserNicknames(schema, cardDetail);
        CardAtUserMessageModel messageModel = CardAtUserMessageModel.builder()
                .cardDetail(cardDetail)
                .companyCardSchema(companyCardSchema)
                .projectCardSchema(schema)
                .project(project)
                .endpointHelper(endpointHelper)
                .sender(sender)
                .cardUserNickNames(cardUserNicknames)
                .nickName(cardUserNicknames.get(sender))
                .operationType(operationType)
                .build();
        noticeService.sendMessageModel(card.getCompanyId(), sender, receivers, messageModel);
    }

    @Async
    @AfterCommit
    public void noticeAtUsersInCardComment(Card card, Map<String, Object> cardDetail, LoginUser sender, Collection<String> receivers,
                                           CompanyCardSchema companyCardSchema, ProjectCardSchema schema, ProjectNoticeConfig.Type operationType) {
        Project project = projectQueryService.select(card.getProjectId());
        CardCommentAtUserMessageModel messageModel = CardCommentAtUserMessageModel.builder()
                .cardDetail(cardDetail)
                .companyCardSchema(companyCardSchema)
                .projectCardSchema(schema)
                .project(project)
                .endpointHelper(endpointHelper)
                .sender(sender.getUsername())
                .nickName(userService.userNickOrName(card.getCompanyId(), sender))
                .operationType(operationType)
                .build();
        noticeService.sendMessageModel(card.getCompanyId(), sender.getUsername(), receivers, messageModel);
    }

}
