package com.ezone.ezproject.external.ci;

import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.external.ci.bean.AutoStatusFlowEventType;
import com.ezone.ezproject.external.ci.bean.CodeCardEventMessage;
import com.ezone.ezproject.modules.card.service.CardCmdService;
import com.ezone.ezproject.modules.card.service.CardQueryService;
import com.ezone.ezproject.modules.card.status.flow.CodeEventFilter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RocketMQMessageListener(
        topic = "${rocketmq.consumer.codeEvent.topic}",
        selectorExpression = "LINK",
        consumerGroup = "${rocketmq.consumer.codeEvent.group}",
        consumeThreadMax = 10)
@Service
@AllArgsConstructor
@Slf4j
public class CodeCardMessageListener implements RocketMQListener<CodeCardEventMessage> {
    private static final Pattern CARD_PATTERN = Pattern.compile("(?<projectKey>[\\w\\d-]{1,32})-(?<seqNum>[\\d]+)");

    private CardQueryService cardQueryService;
    private CardCmdService cardCmdService;

    @Override
    public void onMessage(CodeCardEventMessage message) {
        log.info("received code event message: {}", message);
        if (null == message || CollectionUtils.isEmpty(message.getCardKeys())) {
            return;
        }
//        Matcher matcher = CARD_PATTERN.matcher(message.getCommitMessage());
//        while (matcher.find()) {
//            String projectKey = matcher.group("name");
//            Long seqNum = NumberUtils.createLong(matcher.group("seqNum"));
//        }
        Long companyId = message.getCompanyId();
        List<Card> cards = cardQueryService.select(companyId, message.getCardKeys());
        if (CollectionUtils.isEmpty(cards)) {
            return;
        }
        AutoStatusFlowEventType eventType;
        switch (message.getEvent()) {
            case PUSH:
                eventType = AutoStatusFlowEventType.CODE_PUSH;
                break;
            case REVIEW_ADD:
            case REVIEW_UPDATE:
            case REVIEW_PUSH:
                eventType = AutoStatusFlowEventType.CODE_REVIEW_ADD;
                break;
            case REVIEW_APPROVAL:
                eventType = AutoStatusFlowEventType.CODE_REVIEW_APPROVAL;
                break;
            case REVIEW_REJECTION:
                eventType = AutoStatusFlowEventType.CODE_REVIEW_REJECTION;
                break;
            case REVIEW_MERGE:
                eventType = AutoStatusFlowEventType.CODE_REVIEW_MERGE;
                break;
            case TAG_ADD:
                eventType = AutoStatusFlowEventType.CODE_TAG_ADD;
                break;
            default:
                log.error("Ignore invalid event:[{}]", message.getEvent());
                return;
        }
        cards.stream()
                .collect(Collectors.groupingBy(Card::getProjectId))
                .entrySet()
                .forEach(entry -> {
                    try {
                        cardCmdService.tryAutoStatusFlow(entry.getKey(), entry.getValue(), message.getUsername(), eventType,
                                CodeEventFilter.builder().eventType(eventType).branch(message.getBranchName()).build());
                    } catch (Exception e) {
                        log.error("tryAutoStatusFlow exception!", e);
                    }
                });
    }
}
