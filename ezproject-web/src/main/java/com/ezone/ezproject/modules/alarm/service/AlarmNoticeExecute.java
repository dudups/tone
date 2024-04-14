package com.ezone.ezproject.modules.alarm.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.klock.annotation.Klock;
import org.springframework.boot.autoconfigure.klock.model.LockTimeoutStrategy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
@Slf4j
public class AlarmNoticeExecute {
    private final AlarmMessageNoticeService alarmMessageNoticeService;
    private final CardAlarmNoticePlanService cardAlarmNoticePlanService;
    private final RocketMQTemplate rocketMQTemplate;
    private final AlarmConfigQueryService alarmQueryService;
    private static final int LOCK_LEASE_TIME_SECOND_SEND = 59;
    private static final int LOCK_LEASE_TIME_SECOND_CLEAN = 3600;

    @Value("${rocketmq.producer.sendCardAlarmNoticeTopic}")
    private String sendCardAlarmNoticeTopic;


    /**
     * 执行卡片预警通知计划
     */
    @Klock(leaseTime = LOCK_LEASE_TIME_SECOND_SEND, lockTimeoutStrategy = LockTimeoutStrategy.FAIL_FAST)
    @Scheduled(cron = "0 * * * * ?")
    public void sendCardAlarmNotice() {
        log.debug("start sendCardAlarmNotice");
        long start = System.currentTimeMillis();
        List<Long> projectIds = alarmQueryService.haveAlarmConfigProjectIds();
        projectIds.stream().forEach(id -> rocketMQTemplate.asyncSend(sendCardAlarmNoticeTopic, id, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
            }

            @Override
            public void onException(Throwable e) {
                log.error(String.format("Produce card draft:[%s] clean message exception!", id), e);
            }
        }));
        int secondOfMinute = DateTime.now().getSecondOfMinute();
        try {
            TimeUnit.SECONDS.sleep(50 - secondOfMinute);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.debug("end sendCardAlarmNotice");
    }

    @RocketMQMessageListener(
            topic = "${rocketmq.producer.sendCardAlarmNoticeTopic}",
            consumerGroup = "${rocketmq.consumer.sendCardAlarmNotice.group}",
            consumeThreadMax = 10)
    @Component
    @AllArgsConstructor
    public class SendCardAlarmNoticeConsumer implements RocketMQListener<Long> {
        private AlarmMessageNoticeService alarmMessageNoticeService;

        @Override
        public void onMessage(Long id) {
            try {
                log.debug("Start send card alarm message:[{}]", id);
                alarmMessageNoticeService.sendCardAlarmNotice(id);
                log.debug("End send card alarm message:[{}]", id);
            } catch (Exception e) {
                // 因定期执行，故吞掉异常永远算作消费成功
                log.error("SendCardAlarmNoticeConsumer exception!", e);
            }
        }
    }

    /**
     * 执行项目及计划的预警通知
     */
    @Klock(leaseTime = LOCK_LEASE_TIME_SECOND_SEND, lockTimeoutStrategy = LockTimeoutStrategy.FAIL_FAST)
    @Scheduled(cron = "0 * * * * ?")
    @Async
    public void sendProjectAndPlanAlarmNotice() {
        log.debug("start sendProjectAndPlanAlarmNotice");
        long start = System.currentTimeMillis();
        int minute = (int) (start / 1000 / 60);
        alarmMessageNoticeService.sendProjectAndPlanAlarmNotice(minute);

        int secondOfMinute = DateTime.now().getSecondOfMinute();
        try {
            TimeUnit.SECONDS.sleep(50- secondOfMinute);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.debug("end sendProjectAndPlanAlarmNotice");
    }

    /**
     * 清理卡片预警通知计划
     */
    @Klock(leaseTime = LOCK_LEASE_TIME_SECOND_CLEAN, lockTimeoutStrategy = LockTimeoutStrategy.FAIL_FAST)
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanInactiveCardAlarmNoticePlan() {
        Date endTime = DateUtils.addDays(new Date(), -7);
        cardAlarmNoticePlanService.cleanNoticePlansBeforeDate(endTime);
        try {
            TimeUnit.SECONDS.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}