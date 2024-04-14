package com.ezone.ezproject.modules.card.clean;

import com.ezone.ezproject.es.entity.CardDraft;
import com.ezone.ezproject.modules.attachment.service.CardAttachmentCmdService;
import com.ezone.ezproject.modules.card.bean.query.Lt;
import com.ezone.ezproject.modules.card.service.CardDraftCmdService;
import com.ezone.ezproject.modules.card.service.CardDraftQueryService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.klock.annotation.Klock;
import org.springframework.boot.autoconfigure.klock.model.LockTimeoutStrategy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
@EnableScheduling
@Slf4j
public class CardDraftCleaner {
    @Value("${rocketmq.producer.cleanCardDraftTopic}")
    private String cleanCardDraftTopic;

    private final CardDraftQueryService cardDraftQueryService;
    private final CardAttachmentCmdService cardAttachmentCmdService;

    private final RocketMQTemplate rocketMQTemplate;

    private static final int LOCK_LEASE_TIME_SECOND = 3600;

    @Klock(leaseTime = LOCK_LEASE_TIME_SECOND, lockTimeoutStrategy = LockTimeoutStrategy.FAIL_FAST)
    @Scheduled(cron = "0 30 1 * * ?")
    public void clearCardDraft() {
        try {
            cardDraftQueryService.searchIds(Lt.builder()
                    .field(CardDraft.CREATE_TIME)
                    .value(String.valueOf(DateUtils.addDays(new Date(), -1).getTime())).build())
                    .stream()
                    .forEach(id -> rocketMQTemplate.asyncSend(cleanCardDraftTopic, id, new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) { }

                        @Override
                        public void onException(Throwable e) {
                            log.error(String.format("Produce card draft:[%s] clean message exception!", id), e);
                        }
                    }));
        } catch (IOException e) {
            log.error("clearCardDraft exception!", e);
        }
        try {
            TimeUnit.SECONDS.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @RocketMQMessageListener(
            topic = "${rocketmq.producer.cleanCardDraftTopic}",
            consumerGroup = "${rocketmq.consumer.cleanCardDraft.group}",
            consumeThreadMax = 10)
    @Component
    @AllArgsConstructor
    public class CleanCardDraftConsumer implements RocketMQListener<Long> {
        private CardDraftCmdService cardDraftCmdService;

        @Override
        public void onMessage(Long id) {
            try {
                log.info("Start clean draft:[{}]", id);
                CardDraft draft = cardDraftQueryService.select(id);
                if (draft != null) {
                    cardDraftCmdService.delete(id);
                    cardAttachmentCmdService.delete(id, draft);
                }
                log.info("End clean draft:[{}]", id);
            } catch (Exception e) {
                // 因定期执行，故吞掉异常永远算作消费成功
                log.error("CleanCardDraftConsumer exception!", e);
            }
        }
    }
}
