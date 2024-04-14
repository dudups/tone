package com.ezone.ezproject.common.function;

import lombok.Getter;

import java.util.function.Supplier;

public class CacheableSupplier<T> implements Supplier<T> {
    private Supplier<T> supplier;

    @Getter(lazy = false)
    private final T cache = supplier.get();

    private CacheableSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        return getCache();
    }

    public static <T> CacheableSupplier<T> instance(Supplier<T> supplier) {
        return new CacheableSupplier<>(supplier);
    }
}
