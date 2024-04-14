package com.ezone.ezproject.common.limit.incr;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.boot.autoconfigure.klock.annotation.Klock;
import org.springframework.boot.autoconfigure.klock.model.LockTimeoutStrategy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@AllArgsConstructor
public class DailyIncrLimitReset {
    private List<DailyIncrLimiter> dailyIncrLimiters;

    private static final int LOCK_LEASE_TIME_SECOND = 3600;

    @Klock(leaseTime = LOCK_LEASE_TIME_SECOND, lockTimeoutStrategy = LockTimeoutStrategy.FAIL_FAST)
    @Scheduled(cron = "0 30 0 * * ?")
    public void reset() {
        if (CollectionUtils.isNotEmpty(dailyIncrLimiters)) {
            dailyIncrLimiters.stream().filter(Objects::nonNull).forEach(dailyIncrLimiter -> {
                try {
                    dailyIncrLimiter.reset();
                } catch (Exception e) {
                    log.error(String.format("reset daily limit for [%s] exception!", dailyIncrLimiter.domainResourceKey()), e);
                }
            });
        }
        try {
            TimeUnit.SECONDS.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
