package com.ezone.ezproject.modules.notice;

import com.ezone.ezbase.iam.bean.BaseUser;
import com.ezone.ezbase.iam.bean.GroupUser;
import com.ezone.ezbase.iam.bean.enums.GroupUserType;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.service.CardHelper;
import com.ezone.ezproject.modules.common.EndpointHelper;
import com.ezone.ezproject.modules.notice.bean.NoticeMsg;
import com.ezone.ezproject.modules.notice.bean.NoticeType;
import com.ezone.ezproject.modules.notice.message.CardOperationMessageModel;
import com.ezone.ezproject.modules.notice.message.CardsOperationMessageModel;
import com.ezone.ezproject.modules.notice.message.MessageModel;
import com.ezone.ezproject.modules.notice.message.NoticeContentHelp;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import jodd.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NoticeService {
    @Value("${rocketmq.producer.noticeTopic}")
    private String noticeTopic;

    private final NoticeContentHelp noticeContentHelp;

    private final EndpointHelper endpointHelper;

    private final RocketMQTemplate rocketMQTemplate;

    private final ProjectQueryService projectQueryService;

    private final UserService userService;

    public void notice(NoticeMsg msg) {
        if (null == msg) {
            return;
        }
        rocketMQTemplate.asyncSend(noticeTopic, msg, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("Send notice msg success, msgId:[{}], status:[{}], msg:[{}]", sendResult.getMsgId(), sendResult.getSendStatus(), msg);
            }

            @Override
            public void onException(Throwable e) {
                log.error(String.format("Send notice msg:[%s] exception!", msg), e);
            }
        });
    }

    public void asyncNotice(Long companyId, String sender, Collection<String> receivers, Supplier<String> contentSupplier, NoticeType... noticeTypes) {
        String content = contentSupplier.get();
        asyncNotice(companyId, sender, receivers, content, noticeTypes);
    }

    public void asyncNotice(Long companyId, String sender, Collection<String> users, String content, NoticeType... noticeTypes) {
        if (CollectionUtils.isEmpty(users)) {
            return;
        }
        if (noticeTypes == null || noticeTypes.length == 0) {
            noticeTypes = NoticeType.DEFAULTS;
        }
        Set<GroupUser> msgReceivers = users.stream().map(user -> new GroupUser(user, GroupUserType.USER)).collect(Collectors.toSet());
        for (NoticeType noticeType : noticeTypes) {
            notice(NoticeMsg.builder()
                    .companyId(companyId)
                    .sender(sender)
                    .receivers(msgReceivers)
                    .content(content)
                    .notificationType(noticeType)
                    .filterSender(true)
                    .build());
        }
    }

    public void asyncNoticeUserGroup(Long companyId, String sender, Collection<GroupUser> users, String content, NoticeType... noticeTypes) {
        if (CollectionUtils.isEmpty(users)) {
            return;
        }
        if (noticeTypes == null || noticeTypes.length == 0) {
            noticeTypes = NoticeType.DEFAULTS;
        }
        Set<GroupUser> msgReceivers = new HashSet<>(users);
        for (NoticeType noticeType : noticeTypes) {
            notice(NoticeMsg.builder()
                    .companyId(companyId)
                    .sender(sender)
                    .receivers(msgReceivers)
                    .content(content)
                    .notificationType(noticeType)
                    .filterSender(true)
                    .build());
        }
    }


    public void doNotice(Card card, Map<String, Object> cardDetail, CompanyCardSchema companyCardSchema, ProjectCardSchema schema, String sender, ProjectNoticeConfig.Type operationType, Collection<String> receivers) {
        receivers.remove(sender);
        if (CollectionUtils.isEmpty(receivers)) {
            return;
        }
        Project project = projectQueryService.select(card.getProjectId());
        Map<String, String> cardUserNicknames = getCardUserNicknames(schema, cardDetail);
        String nickName = cardUserNicknames.get(sender);
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
        sendMessageModel(card.getCompanyId(), sender, receivers, messageModel);
    }


    /**
     * 发送模板内容
     *
     * @param companyId    企业ID
     * @param sender
     * @param receivers
     * @param messageModel
     */
    public void sendMessageModelToGroupUsers(Long companyId, String sender, Set<GroupUser> receivers, MessageModel messageModel) {
        if (CollectionUtils.isEmpty(receivers)) {
            return;
        }
        String content = noticeContentHelp.requestBodyForMsgPlatform(NoticeType.SYSTEM, messageModel);
        notice(NoticeMsg.builder()
                .companyId(companyId)
                .sender(sender)
                .receivers(receivers)
                .content(content)
                .notificationType(NoticeType.SYSTEM)
                .filterSender(true)
                .build());

        String mailContent = noticeContentHelp.requestBodyForMsgPlatform(NoticeType.EMAIL, messageModel);
        notice(NoticeMsg.builder()
                .companyId(companyId)
                .sender(sender)
                .receivers(receivers)
                .content(mailContent)
                .notificationType(NoticeType.EMAIL)
                .filterSender(true)
                .build());

        String feiShuContent = noticeContentHelp.requestBodyForMsgPlatform(NoticeType.FEI_SHU,
                messageModel);
        notice(NoticeMsg.builder()
                .companyId(companyId)
                .sender(sender)
                .receivers(receivers)
                .content(feiShuContent)
                .notificationType(NoticeType.FEI_SHU)
                .filterSender(true)
                .build());
    }

    /**
     * 发送模板内容
     *
     * @param companyId    企业ID
     * @param sender
     * @param receivers
     * @param messageModel
     */
    public void sendMessageModel(Long companyId, String sender, Collection<String> receivers, MessageModel messageModel) {
        String content = noticeContentHelp.requestBodyForMsgPlatform(NoticeType.SYSTEM, messageModel);
        log.debug("内部通知", content);
        asyncNotice(companyId, sender, receivers, content);
        String mailContent = noticeContentHelp.requestBodyForMsgPlatform(NoticeType.EMAIL, messageModel);
        asyncNotice(companyId, sender, receivers, mailContent, NoticeType.EMAIL);
        String feiShuContent = noticeContentHelp.requestBodyForMsgPlatform(NoticeType.FEI_SHU,
                messageModel);
        asyncNotice(companyId, sender, receivers, feiShuContent, NoticeType.FEI_SHU);
    }


    /**
     * 发送模板内容
     *
     * @param companyId    企业ID
     * @param sender
     * @param receivers
     * @param messageModel
     */
    public void sendMessageModelForGroupUser(Long companyId, String sender, Collection<GroupUser> receivers, MessageModel messageModel) {
        String content = noticeContentHelp.requestBodyForMsgPlatform(NoticeType.SYSTEM, messageModel);
        log.debug("内部通知", content);
        asyncNoticeUserGroup(companyId, sender, receivers, content);
        String mailContent = noticeContentHelp.requestBodyForMsgPlatform(NoticeType.EMAIL, messageModel);
        asyncNoticeUserGroup(companyId, sender, receivers, mailContent, NoticeType.EMAIL);
        String feiShuContent = noticeContentHelp.requestBodyForMsgPlatform(NoticeType.FEI_SHU,
                messageModel);
        asyncNoticeUserGroup(companyId, sender, receivers, feiShuContent, NoticeType.FEI_SHU);
    }

    public void doNotices(Project project, Map<Long, Map<String, Object>> cardDetails, BaseUser sender, ProjectNoticeConfig.Type operationType, Set<String> receivers) {
        receivers.remove(sender.getUsername());
        if (CollectionUtils.isEmpty(receivers)) {
            return;
        }
        CardsOperationMessageModel messageModel = CardsOperationMessageModel.builder()
                .cardDetails(cardDetails)
                .endpointHelper(endpointHelper)
                .project(project)
                .sender(sender.getUsername())
                .nickName(userService.userNickOrName(project.getCompanyId(), sender))
                .operationType(operationType)
                .build();

        sendMessageModel(project.getCompanyId(), sender.getUsername(), receivers, messageModel);
    }

    /**
     * 获取通知时，卡片中有关用户的昵称 <br/>
     * 如未配置昵你，显示用户名
     *
     * @param projectCardSchema
     * @param cardDetail
     * @return map key为用户名，value为用户昵称
     */
    public Map<String, String> getCardUserNicknames(ProjectCardSchema projectCardSchema, Map<String, Object> cardDetail) {
        Set<String> usernames = CardHelper.getUsersFromCardFields(projectCardSchema, cardDetail);
        List<BaseUser> baseUsers = userService.queryUsersByUsernames(new ArrayList<>(usernames));
        Map<String, String> nameNikeNameMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(baseUsers)) {
            baseUsers.forEach(baseUser -> {
                String username = baseUser.getUsername();
                String nickname = baseUser.getNickname();
                nameNikeNameMap.put(username, StringUtil.isBlank(nickname) ? username : nickname);
            });
        }
        return nameNikeNameMap;
    }

}
