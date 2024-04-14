package com.ezone.ezproject.modules.common;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Component
public class TransactionHelper {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runWithRequiresNew(Runnable runnable) {
        runnable.run();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T runWithRequiresNew(Supplier<T> supplier) {
        return supplier.get();
    }
}
