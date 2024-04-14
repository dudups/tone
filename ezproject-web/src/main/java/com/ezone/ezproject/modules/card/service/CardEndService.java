package com.ezone.ezproject.modules.card.service;

import com.ezone.ezbase.iam.bean.LoginUser;
import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.modules.card.bean.CardEnd;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
@Slf4j
public class CardEndService {
    @Value("${rocketmq.producer.cardEndTopic}")
    private String cardEndTopic;

    private RocketMQTemplate rocketMQTemplate;

    public CardEndService(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Async
    public void cardEnd(List<String> users, Card card) {
        if (CollectionUtils.isEmpty(users)) {
            return;
        }
        users.stream().filter(StringUtils::isNotEmpty).distinct().forEach(user -> send(new CardEnd()
                .setId(IdUtil.generateId())
                .setCardId(card.getId())
                .setCompanyId(card.getCompanyId())
                .setSeqNum(card.getSeqNum())
                .setProjectId(card.getProjectId())
                .setProjectKey(card.getProjectKey())
                .setTime(System.currentTimeMillis())
                .setUser(user)));
    }

    public void send(CardEnd msg) {
        if (null == msg) {
            return;
        }
        rocketMQTemplate.asyncSend(cardEndTopic, msg, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) { }

            @Override
            public void onException(Throwable e) {
                log.error(String.format("Send cardEnd msg:[%s] exception!", msg), e);
            }
        });
    }

    public void send(List<CardEnd> msgs) {
        if (CollectionUtils.isEmpty(msgs)) {
            return;
        }
        msgs.forEach(this::send);
    }

    @Async
    public void asyncSend(List<CardEnd> msgs) {
        if (CollectionUtils.isEmpty(msgs)) {
            return;
        }
        msgs.forEach(this::send);
    }

    @Async
    public void asyncSends(Supplier<List<CardEnd>> msgsSupplier) {
        send(msgsSupplier.get());
    }

    @Async
    public void asyncSend(Supplier<CardEnd> msgSupplier) {
        send(msgSupplier.get());
    }

    @Deprecated
    public void cardEnd(Map<Long, Map<String, Object>> targetCardProps, Map<Long, Map<String, Object>> sourceCardProps, Map<Long, Card> cardsMap, ProjectCardSchema schema) {
        targetCardProps.forEach((cardId, targetCardDetail) -> {
            Map<String, Object> sourceCardDetail = sourceCardProps.get(cardId);
            String type = FieldUtil.toString(sourceCardDetail.get(CardField.TYPE));
            String oldStatus = FieldUtil.toString(sourceCardDetail.get(CardField.STATUS));
            String status = FieldUtil.toString(targetCardDetail.get(CardField.STATUS));
            if (CardHelper.isChangeToEnd(schema, type, oldStatus, status)) {
                Card card = cardsMap.get(cardId);
                cardEnd(FieldUtil.toStringList(sourceCardDetail.get(CardField.OWNER_USERS)), card);
            }
        });
    }

    @Async
    public void cardEnd(LoginUser user, Card card, Map<String, Object> cardDetail, ProjectCardSchema schema, String oldStatus, String status) {
        String type = FieldUtil.getType(cardDetail);
        if (CardHelper.isChangeToEnd(schema, type, oldStatus, status)) {
            cardEnd(FieldUtil.toStringList(cardDetail.get(CardField.OWNER_USERS)), card);
        }
    }
}
