package com.ezone.ezproject.common.limit.incr;

import com.ezone.ezproject.common.exception.CodedException;
import lombok.Getter;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

public abstract class AbstractIncrLimiter<T> implements IncrLimiter<T> {
    @Autowired
    private RedissonClient redisson;

    @Getter(lazy = true)
    private final RMap<T, Long> incrMap = redisson.getMap(domainResourceKey(), CODEC);

    /**
     * RMap.addAndGet基于redis hash的INCRBYFLOAT指令，
     * 在redis层不能把值存成Json序列化结果(eg: ["java.lang.Long",1])，故需对MapValue自定义序列化以确保存为数字字符串(eg: 1)
     */
    public static final Codec CODEC = new JsonJacksonCodec() {
        @Override
        public Decoder<Object> getMapValueDecoder() {
            return LongCodec.INSTANCE.getMapValueDecoder();
        }

        @Override
        public Encoder getMapValueEncoder() {
            return LongCodec.INSTANCE.getMapValueEncoder();
        }
    };

    public abstract Long incrLimit();

    public abstract String limitMsg();

    @Override
    public void check(T domainId, Long incr) {
        if (incr == null || incr <= 0) {
            return;
        }
        Long incrLimit = incrLimit();
        if (incrLimit == null || incrLimit <= 0) {
            return;
        }
        Long oldIncr = getIncrMap().get(domainId);
        Long newIncr = oldIncr == null ? incr : (oldIncr + incr);
        if (newIncr > incrLimit()) {
            throw new CodedException(HttpStatus.TOO_MANY_REQUESTS, limitMsg());
        }
        getIncrMap().addAndGetAsync(domainId, incr);
    }

    @Override
    public void reset() {
        getIncrMap().clear();
    }
}
