package com.ezone.ezproject.modules.card.service;

import com.ezone.ezbase.iam.bean.BaseUser;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.common.EndpointHelper;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService;
import com.ezone.ezproject.modules.event.events.CardCreateEvent;
import com.ezone.ezproject.modules.event.events.CardsCreateEvent;
import com.ezone.ezproject.modules.notice.NoticeService;
import com.ezone.ezproject.modules.notice.message.CardMemberFieldChangeMessageModel;
import com.ezone.ezproject.modules.notice.message.CardsMemberFieldChangeMessageModel;
import com.ezone.ezproject.modules.project.service.ProjectNoticeConfigService;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
@Service
@Slf4j
@AllArgsConstructor
public class CardCreateUserNoticeService {
    protected static final String OPT_ADD = "add";
    protected static final String OPT_REMOVE = "remove";
    protected static final List<String> optDefines = Arrays.asList(OPT_ADD, OPT_REMOVE);
    private NoticeService noticeService;

    private EndpointHelper endpointHelper;

    private UserService userService;

    private ProjectSchemaQueryService projectSchemaQueryService;

    private ProjectQueryService projectQueryService;

    private ProjectNoticeConfigService projectNoticeConfigService;

    private CompanyProjectSchemaQueryService companyProjectSchemaQueryService;

    /**
     * 这些卡片字段中用户变化时，无需通知变动相关人员
     */
    protected static final List<String> NOT_ATTENTION_CARD_SCHEMA_FIELDS = Arrays.asList("at_users", "create_user", "last_modify_user", "watch_users", "follow_users");

    @Async
    public void noticeCardsCreate(CardsCreateEvent event) {
        Map<Long, Map<String, Object>> cardDetails = event.getCardDetails();
        if (cardDetails == null) {
            return;
        }
        doNoticeCardsCreate(cardDetails, event.getUser());
    }

    @Async
    public void noticeCardCreate(CardCreateEvent event) {
        if (Boolean.TRUE.equals(event.getSendEmail())) {
            doNoticeCardCreate(event.getCard(), event.getCardDetail(), event.getUser());
        }
    }

    private void doNoticeCardsCreate(Map<Long, Map<String, Object>> cardDetails, String operator) {
        List<CardNoticeMsgInfo> cardNoticeMsgInfos = new ArrayList<>();
        Long projectId = null;
        Long companyId = null;
        for (Map.Entry<Long, Map<String, Object>> entry : cardDetails.entrySet()) {
            Map<String, Object> cardDetail = entry.getValue();
            if (cardDetail == null || cardDetail.size() == 0) {
                continue;
            }
            projectId = FieldUtil.getProjectId(cardDetail);
            List<String> userAndMemberFields = projectSchemaQueryService.getUserOrMemberFields(projectId);
            //userMemberFields 用户-用户对应变化的成员字段
            Map<String, List<String>> userMemberFields = new HashMap<>();
            cardDetail.entrySet().stream()
                    .filter(entity -> {
                                String fieldKey = entity.getKey();
                                return !NOT_ATTENTION_CARD_SCHEMA_FIELDS.contains(fieldKey) && userAndMemberFields.contains(fieldKey);
                            }
                    ).forEach(cardFieldVale -> {
                                String fieldKey = cardFieldVale.getKey();
                                List<String> receivers = FieldUtil.toStringList(cardFieldVale.getValue());
                                receivers.remove(operator);
                                for (String receiver : receivers) {
                                    List<String> memberField = userMemberFields.getOrDefault(receiver, new ArrayList<>());
                                    memberField.add(fieldKey);
                                    userMemberFields.put(receiver, memberField);
                                }
                            }
                    );
            companyId = FieldUtil.getCompanyId(cardDetail);
            cardNoticeMsgInfos.add(
                    CardNoticeMsgInfo.builder()
                            .cardDetail(cardDetail)
                            .cardId(entry.getKey())
                            .userOfMemberFields(userMemberFields)
                            .build());
        }
        sendCreateMessage(operator, projectId, companyId, cardNoticeMsgInfos);
    }

    private void doNoticeCardCreate(Card card, Map<String, Object> cardDetail, String operator) {
        if (card == null || cardDetail == null || cardDetail.size() == 0) {
            return;
        }
        Long projectId = card.getProjectId();
        List<String> userAndMemberFields = projectSchemaQueryService.getUserOrMemberFields(projectId);
        Map<String, Map<String, List<String>>> userOptFields = new HashMap<>();
        cardDetail.entrySet().stream()
                .filter(cardEntity -> {
                            String fieldKey = cardEntity.getKey();
                            return !NOT_ATTENTION_CARD_SCHEMA_FIELDS.contains(fieldKey) && userAndMemberFields.contains(fieldKey);
                        }
                ).forEach(cardEntity -> {
                            String fieldKey = cardEntity.getKey();
                            List<String> receivers = FieldUtil.toStringList(cardEntity.getValue());
                            receivers.remove(operator);
                            receivers.forEach(rec -> {
                                Map<String, List<String>> fieldOpts = userOptFields.getOrDefault(rec, new HashMap<>(8));
                                List<String> fields = fieldOpts.getOrDefault(OPT_ADD, new ArrayList<>());
                                fields.add(fieldKey);
                                fieldOpts.put(OPT_ADD, fields);
                                userOptFields.put(rec, fieldOpts);
                            });
                        }
                );
        sendCreateMessage(card, cardDetail, operator, userOptFields, projectId, card.getCompanyId());
    }

    private void sendCreateMessage(String operator, Long projectId, Long companyId, @Nonnull List<CardNoticeMsgInfo> infos) {
        if (projectId == null) {
            return;
        }
        ProjectNoticeConfig projectNoticeConfig = null;
        try {
            projectNoticeConfig = projectNoticeConfigService.getProjectNoticeConfig(projectId);
        } catch (IOException e) {
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        ProjectNoticeConfig.Config cardNoticeConfig = projectNoticeConfig.getCardNoticeConfig();
        if (!cardNoticeConfig.isOpen() || !cardNoticeConfig.getNotifyTypes().contains(ProjectNoticeConfig.Type.CREATE)) {
            return;
        }

        Project project = projectQueryService.select(projectId);
        ProjectCardSchema projectCardSchema = projectSchemaQueryService.getProjectCardSchema(projectId);
        Map<String, UserMsgMergeInfo> userMergeMsgMap = new HashMap<>(16);
        for (CardNoticeMsgInfo info : infos) {
            Map<String, List<String>> userOfMemberFields = info.userOfMemberFields;
            //将同一用户的合并
            userOfMemberFields.forEach((user, optFields) -> {
                List<String> names = projectCardSchema.fieldNames(optFields);
                UserMsgMergeInfo mergeInfo = userMergeMsgMap.getOrDefault(user, new UserMsgMergeInfo());
                userMergeMsgMap.put(user, mergeInfo);
                mergeInfo.setCreator(operator);
                mergeInfo.getCardDetails().put(info.cardId, info.cardDetail);
                mergeInfo.getCardFields().addAll(names);
            });
        }
        String nickName = userService.userNickOrName(companyId, userService.user(operator));
        userMergeMsgMap.forEach((user, innerComment) -> {
            if (MapUtils.isNotEmpty(innerComment.cardDetails)) {
                CardsMemberFieldChangeMessageModel messageModel = CardsMemberFieldChangeMessageModel.builder()
                        .cardDetails(innerComment.cardDetails)
                        .endpointHelper(endpointHelper)
                        .project(project)
                        .sender(operator)
                        .nickName(nickName)
                        .fieldNames(innerComment.cardFields)
                        .memberOperatedType(CardsMemberFieldChangeMessageModel.MemberOperatedType.add)
                        .cardOperatedType(ProjectNoticeConfig.Type.CREATE)
                        .build();
                noticeService.sendMessageModel(companyId, operator, Collections.singletonList(user), messageModel);
            }
        });
    }

    private void sendCreateMessage(Card card, Map<String, Object> cardDetail, String operator, Map<String, Map<String, List<String>>> userOptFields, Long projectId, Long companyId) {
        ProjectNoticeConfig projectNoticeConfig = null;
        try {
            projectNoticeConfig = projectNoticeConfigService.getProjectNoticeConfig(projectId);
        } catch (IOException e) {
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        ProjectNoticeConfig.Config cardNoticeConfig = projectNoticeConfig.getCardNoticeConfig();
        if (!cardNoticeConfig.isOpen() || !cardNoticeConfig.getNotifyTypes().contains(ProjectNoticeConfig.Type.CREATE)) {
            return;
        }
        if (userOptFields == null) {
            return;
        }
        ProjectCardSchema projectCardSchema = projectSchemaQueryService.getProjectCardSchema(projectId);
        Project project = projectQueryService.select(card.getProjectId());
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(card.getCompanyId());
        Map<String, String> cardUserNicknames = noticeService.getCardUserNicknames(projectCardSchema, cardDetail);
        userOptFields.entrySet().stream()
                .filter(entity -> entity.getValue() != null)
                .forEach(entity -> {
                            String userName = entity.getKey();
                            Map<String, List<String>> optFieldMap = entity.getValue();
                            optFieldMap.entrySet().stream()
                                    .filter(optFields -> (optFields.getValue() != null && !optFields.getValue().isEmpty()))
                                    .forEach(optFields -> {
                                        List<String> changedMemberFieldKeys = optFields.getValue();

                                        if (OPT_ADD.equals(optFields.getKey())) {
                                                    CardMemberFieldChangeMessageModel messageModel = CardMemberFieldChangeMessageModel.builder()
                                                            .cardDetail(cardDetail)
                                                            .endpointHelper(endpointHelper)
                                                            .project(project)
                                                            .companyCardSchema(companyCardSchema)
                                                            .projectCardSchema(projectCardSchema)
                                                            .sender(operator)
                                                            .nickName(cardUserNicknames.get(operator))
                                                            .cardUserNickNames(cardUserNicknames)
                                                            .changedMemberFieldKeys(changedMemberFieldKeys)
                                                            .memberOperatedType(CardMemberFieldChangeMessageModel.MemberOperatedType.add)
                                                            .cardOperatedType(ProjectNoticeConfig.Type.CREATE)
                                                            .build();
                                                    noticeService.sendMessageModel(companyId, operator, Collections.singletonList(userName), messageModel);
                                                } else if (OPT_REMOVE.equals(optFields.getKey())) {
                                                    CardMemberFieldChangeMessageModel messageModel = CardMemberFieldChangeMessageModel.builder()
                                                            .cardDetail(cardDetail)
                                                            .endpointHelper(endpointHelper)
                                                            .project(project)
                                                            .companyCardSchema(companyCardSchema)
                                                            .projectCardSchema(projectCardSchema)
                                                            .sender(operator)
                                                            .nickName(cardUserNicknames.get(operator))
                                                            .cardUserNickNames(cardUserNicknames)
                                                            .changedMemberFieldKeys(changedMemberFieldKeys)
                                                            .memberOperatedType(CardMemberFieldChangeMessageModel.MemberOperatedType.delete)
                                                            .cardOperatedType(ProjectNoticeConfig.Type.CREATE)
                                                            .build();
                                                    noticeService.sendMessageModel(companyId, operator, Collections.singletonList(userName), messageModel);
                                                }
                                            }
                                    );
                        }
                );
    }

    @Data
    @Builder
    public static class CardNoticeMsgInfo {
        Long cardId;
        Map<String, Object> cardDetail;
        /**
         * 当前卡片中按用户分类的，保存用户在哪些成员字段中出现过。
         * key-用户名，value-成员字段名
         */
        Map<String, List<String>> userOfMemberFields;
    }

    @Data
    public static class UserMsgMergeInfo {
        String creator;
        Map<Long, Map<String, Object>> cardDetails = new HashMap<>();
        Set<String> cardFields = new HashSet<>();
    }
}
