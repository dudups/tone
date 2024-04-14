package com.ezone.ezproject.common.function;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class CacheableFunction<T, R> implements Function<T, R> {
    private Function<T, R> function;

    private Map<T, R> cache = new HashMap<>();

    private CacheableFunction(Function<T, R> function) {
        this.function = function;
    }

    @Override
    public R apply(T t) {
        if (cache.containsKey(t)) {
            return cache.get(t);
        }
        R r = function.apply(t);
        cache.put(t, function.apply(t));
        return r;
    }

    public static <T, R> CacheableFunction<T, R> instance(Function<T, R> function) {
        return new CacheableFunction<>(function);
    }

    public CacheableFunction<T, R> cache(T t, R r) {
        cache.put(t, r);
        return this;
    }

    public CacheableFunction<T, R> cache(Map<T, R> map) {
        cache.putAll(map);
        return this;
    }
}
