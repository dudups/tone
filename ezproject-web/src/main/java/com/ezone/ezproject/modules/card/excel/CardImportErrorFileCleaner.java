package com.ezone.ezproject.modules.card.excel;

import com.ezone.ezproject.common.storage.IStorage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.klock.annotation.Klock;
import org.springframework.boot.autoconfigure.klock.model.LockTimeoutStrategy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@EnableScheduling
@AllArgsConstructor
@Slf4j
public class CardImportErrorFileCleaner {

    private IStorage storage;

    private static final int LOCK_LEASE_TIME_SECOND = 3600;

    @Klock(leaseTime = LOCK_LEASE_TIME_SECOND, lockTimeoutStrategy = LockTimeoutStrategy.FAIL_FAST)
    @Scheduled(cron = "0 30 0 * * ?")
    public void cleanCardImportErrorFile() {
        storage.traverseFolder(ExcelCardImport.STORAGE_TMP_ERROR_PATH, traverseData -> {
            if (traverseData.isExpired(1)) {
                storage.delete(traverseData.getKey());
            }
        });
        try {
            TimeUnit.SECONDS.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
