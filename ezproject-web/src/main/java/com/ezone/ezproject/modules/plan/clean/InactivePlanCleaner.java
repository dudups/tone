package com.ezone.ezproject.modules.plan.clean;

import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.modules.plan.service.PlanCmdService;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
@EnableScheduling
@Slf4j
public class InactivePlanCleaner {
    @Value("${rocketmq.producer.cleanInactivePlanTopic}")
    private String cleanInactivePlanTopic;

    private final ProjectQueryService projectQueryService;

    private final RocketMQTemplate rocketMQTemplate;

    private static final int LOCK_LEASE_TIME_SECOND = 3600;

    @Klock(leaseTime = LOCK_LEASE_TIME_SECOND, lockTimeoutStrategy = LockTimeoutStrategy.FAIL_FAST)
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanInactivePlan() {
        int pageSize = 1000;
        int pageNumber = 1;
        do {
            List<Project> projects = projectQueryService.selectAll(pageNumber, pageSize);
            if (CollectionUtils.isEmpty(projects)) {
                break;
            }
            projects.stream().filter(p -> p.getPlanKeepDays() > 0).forEach(project ->
                    rocketMQTemplate.asyncSend(cleanInactivePlanTopic, project.getId(), new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) { }

                        @Override
                        public void onException(Throwable e) {
                            log.error(String.format("Produce project:[%s] clean plan message exception!", project.getId()), e);
                        }
                    })
            );
            pageNumber++;
        } while (true);
        try {
            TimeUnit.SECONDS.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @RocketMQMessageListener(
            topic = "${rocketmq.producer.cleanInactivePlanTopic}",
            consumerGroup = "${rocketmq.consumer.cleanInactivePlan.group}",
            consumeThreadMax = 10)
    @Component
    @AllArgsConstructor
    public class CleanInactivePlanConsumer implements RocketMQListener<Long> {
        private PlanCmdService planCmdService;

        @Override
        public void onMessage(Long projectId) {
            try {
                log.info("Start clean plans for project:[{}]", projectId);
                planCmdService.cleanInactivePlan(projectId);
                log.info("End clean plans for project:[{}]", projectId);
            } catch (Exception e) {
                // 因定期执行，故吞掉异常永远算作消费成功
                log.error("CleanInactivePlanConsumer exception!", e);
            }
        }
    }
}
