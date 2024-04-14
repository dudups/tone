package com.ezone.ezproject.modules.bill.service;

import com.ezone.ezbase.iam.bean.enums.BillingItem;
import com.ezone.ezbase.iam.bean.enums.BillingMsgUnit;
import com.ezone.ezbase.iam.bean.enums.SystemType;
import com.ezone.ezbase.iam.bean.mq.CompanyConsumptionMsg;
import com.ezone.ezproject.common.storage.IStorage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.klock.annotation.Klock;
import org.springframework.boot.autoconfigure.klock.model.LockTimeoutStrategy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@EnableScheduling
@Slf4j
public class StorageBill {
    @Value("${rocketmq.producer.billStorageBufferTopic}")
    private String billStorageTopic;

    private final RocketMQTemplate rocketMQTemplate;

    private final RedissonClient redisson;

    @Getter(lazy = true)
    private final RSet<Long> buffer = redisson.getSet("com.ezone.ezproject.modules.bill.service.StorageBill");

    private static final int LOCK_LEASE_TIME_SECOND = 60;

    public void updateBill(Long companyId) {
        if (companyId == null) {
            return;
        }
        getBuffer().add(companyId);
    }

    @Klock(leaseTime = LOCK_LEASE_TIME_SECOND, lockTimeoutStrategy = LockTimeoutStrategy.FAIL_FAST)
    @Scheduled(cron = "0 * * * * ?")
    public void billStorage() {
        getBuffer().forEach(companyId ->
                rocketMQTemplate.asyncSend(billStorageTopic, companyId, new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) { }

                    @Override
                    public void onException(Throwable e) {
                        log.error(String.format("Bill storage msg for company:[%s] clean card message exception!", companyId), e);
                    }
                })
        );
    }

    @RocketMQMessageListener(
            topic = "${rocketmq.producer.billStorageBufferTopic}",
            consumerGroup = "${rocketmq.consumer.billStorageBuffer.group}",
            consumeThreadMax = 10)
    @Component
    @AllArgsConstructor
    public class BillStorageConsumer implements RocketMQListener<Long> {
        private IStorage storage;
        private BillService billService;

        @Override
        public void onMessage(Long companyId) {
            try {
                log.info("Start bill storage for company:[{}]", companyId);
                if (StorageBill.this.getBuffer().remove(companyId)) {
                    CompanyConsumptionMsg msg = new CompanyConsumptionMsg();
                    msg.setCompanyId(companyId);
                    msg.setSystemType(SystemType.EZPROJECT);
                    msg.setItem(BillingItem.STORAGE_CAPACITY);
                    msg.setUnit(BillingMsgUnit.BYTE);
                    msg.setTimeStamp(System.currentTimeMillis());
                    msg.setValue(storage.sizeofFolder(String.valueOf(companyId)));
                    billService.bill(msg);
                    log.info("End bill storage for company:[{}]", companyId);
                }
            } catch (Exception e) {
                // 因定期执行，故吞掉异常永远算作消费成功
                log.error("BillStorageConsumer exception!", e);
            }
        }
    }
}
