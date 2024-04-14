package com.ezone.ezproject.common.stream;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Collectors的toMap等方法，遇到value为null和key重复的情况抛异常，而且值null异常不支持通过传入自定义策略解决入；
 * 本类提供替代方法，避免抛异常；
 */
public class CollectorsV2 {
    public static <T, K, U> java.util.stream.Collector<T, ?, Map<K, U>> toMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper) {
        return java.util.stream.Collector.of(
                HashMap::new,
                (map, t) -> map.put(keyMapper.apply(t), valueMapper.apply(t)),
                (v1, v2) -> v2
        );
    }

}
