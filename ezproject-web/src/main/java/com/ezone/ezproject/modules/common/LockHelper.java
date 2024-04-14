package com.ezone.ezproject.modules.common;

import com.ezone.ezproject.common.exception.CodedException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.klock.lock.Lock;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
@Slf4j
@AllArgsConstructor
public class LockHelper {
    private TransactionHelper transactionHelper;

    /**
     * 加锁>独立事务执行>释放锁
     * @param lock
     * @param run
     * @param <T>
     * @return
     */
    public <T> T lockRun(Lock lock, Supplier<T> run) {
        if (lock.acquire()) {
            try {
                return transactionHelper.runWithRequiresNew(run);
            } finally {
                lock.release();
            }
        } else {
            throw new CodedException(HttpStatus.CONFLICT, "Lock fail!");
        }
    }
}
