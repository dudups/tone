package com.ezone.ezproject.modules.notice;

import com.ezone.ezbase.iam.bean.BaseUser;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.modules.notice.bean.NoticeType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Builder
@Data
public class MergeSendNoticeHelper {
    private final NoticeService noticeService;
    private final Long companyId;
    private final NoticeType noticeType;
    private final BaseUser sender;
    private final Project project;
    private final ProjectNoticeConfig.Type operationType;
    private final Integer cardUrlsLimitSize;
    private final List<String> userOrMemberFields;

    /**
     * key-user
     */
    private final List<CardNoticeInfo> cache = new ArrayList<>();


    public void add(Collection<String> receivers, Long cardId, Map<String, Object> cardDetail) {
        cache.add(CardNoticeInfo.builder().receiver(receivers).cardId(cardId).cardDetail(cardDetail).build());
    }

    /**
     * 发送缓存中的消息。
     */
    public void send() {
        Map<String, Map<Long, Map<String, Object>>> userCardsMap = new HashMap<>();
        for (CardNoticeInfo cardInfo : cache) {
            if (cardInfo != null && CollectionUtils.isNotEmpty(cardInfo.getReceiver())) {
                for (String user : cardInfo.getReceiver()) {
                    Map<Long, Map<String, Object>> cards = userCardsMap.getOrDefault(user, new HashMap<>());
                    cards.put(cardInfo.getCardId(), cardInfo.getCardDetail());
                    userCardsMap.put(user, cards);
                }
            }
        }
        //合并消息发送
        userCardsMap.forEach(this::doSend);
    }

    private void doSend(String receiver, Map<Long, Map<String, Object>> cardDetails) {
        if (MapUtils.isEmpty(cardDetails) || receiver == null || receiver.equals(sender)) {
            return;
        }
        Set receives = new HashSet();
        receives.add(receiver);
        noticeService.doNotices(project, cardDetails, sender, operationType, receives);
    }

    private void asyncNotice(Long companyId, String sender, String receiver, String content, NoticeType... noticeTypes) {
        noticeService.asyncNotice(companyId, sender, Collections.singletonList(receiver), content, noticeTypes);
    }

    @Data
    @Builder
    private static class CardNoticeInfo {
        private Collection<String> receiver;
        private Long cardId;
        private Map<String, Object> cardDetail;
    }

}
