package com.ezone.ezproject.configuration;

import io.netty.channel.nio.NioEventLoopGroup;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.klock.config.KlockConfig;
import org.springframework.boot.autoconfigure.klock.core.KlockAspectHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.util.ClassUtils;

/**
 * Klock配置redis仅支持了单节点和集群模式，此处增加哨兵模式支持
 */
@Configuration
@ConditionalOnProperty(prefix = RedisSentinelAutoConfiguration.PREFIX, name = "master-name")
@AutoConfigureAfter(RedisAutoConfiguration.class)
@EnableConfigurationProperties(KlockExtConfig.class)
@Import({KlockAspectHandler.class})
public class RedisSentinelAutoConfiguration {
    public static final String PREFIX = "spring.klock.sentinel-server";

    @Autowired
    private KlockConfig klockConfig;

    @Autowired
    private KlockExtConfig klockExtConfig;

    @Primary
    @Bean(destroyMethod = "shutdown")
    RedissonClient redisson() throws Exception {
        Config config = new Config();
        config.useSentinelServers()
                .addSentinelAddress(klockExtConfig.getSentinelServer().getSentinelAddresses().stream().toArray(String[]::new))
                .setMasterName(klockExtConfig.getSentinelServer().getMasterName())
                .setDatabase(klockConfig.getDatabase())
                .setPassword(klockConfig.getPassword());
        Codec codec=(Codec) ClassUtils.forName(klockConfig.getCodec(),ClassUtils.getDefaultClassLoader()).newInstance();
        config.setCodec(codec);
        config.setEventLoopGroup(new NioEventLoopGroup());
        return Redisson.create(config);
    }
}
