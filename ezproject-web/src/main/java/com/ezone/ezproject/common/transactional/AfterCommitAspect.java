package com.ezone.ezproject.common.transactional;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Order(Ordered.LOWEST_PRECEDENCE)
@Aspect
@Component
@Slf4j
public class AfterCommitAspect {
    @Around("@annotation(com.ezone.ezproject.common.transactional.AfterCommit) && @annotation(afterCommit)")
    public Object around(ProceedingJoinPoint pjp, AfterCommit afterCommit) throws Throwable {
        if (TransactionSynchronizationManager.isSynchronizationActive() &&
                TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        pjp.proceed();
                    } catch (Throwable e) {
                        log.error("Process after commit exception!", e);
                    }
                }
            });
            return null;
        } else {
            return pjp.proceed();
        }
    }
}
