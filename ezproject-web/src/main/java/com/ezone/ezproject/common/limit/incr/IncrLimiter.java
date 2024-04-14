package com.ezone.ezproject.common.limit.incr;

/**
 * 限制指定域范围下某类资源增长速度
 * @param <T> 域唯一标识对应的类型
 */
public interface IncrLimiter<T> {
    /**
     * 唯一标识，对应二元组(范围域类型，资源类型)
     * @return
     */
    String domainResourceKey();

    /**
     * 校验是否允许再增长incr的量
     * @param domainId
     * @param incr
     */
    void check(T domainId, Long incr);

    default void check(T domainId, Integer incr) {
        check(domainId, incr == null ? 0L : incr.longValue());
    }

    default void check(T domainId) {
        check(domainId, 1L);
    }

    /**
     * 重置当前周期计数
     */
    void reset();
}
