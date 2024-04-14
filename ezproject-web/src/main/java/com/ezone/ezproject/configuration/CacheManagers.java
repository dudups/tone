package com.ezone.ezproject.configuration;

import com.ezone.galaxy.framework.common.spring.SpringBeanFactory;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheManagers {

    public static final String REDISSON_CACHE_MANAGER = "redissonCacheManager";

    public static final String LOCAL_CACHE_MANAGER = "localCacheManager";

    public static final String TRANSIENT_CACHE_MANAGER = "transientCacheManager";

    private static final String REDISSON_SPRING_CACHE_CONFIG_LOCATION = "classpath:/redisson-spring-cache-manager.yaml";

    @Primary
    @Bean(REDISSON_CACHE_MANAGER)
    public RedissonSpringCacheManager redissonCacheManager(@Autowired RedissonClient redissonClient) {
        return new RedissonSpringCacheManager(redissonClient, REDISSON_SPRING_CACHE_CONFIG_LOCATION);
    }

    @Bean(LOCAL_CACHE_MANAGER)
    public CacheManager localCacheManager() {
        return new CompositeCacheManager(
                sizeBasedCaffeineCacheBuilder(10000, "ProjectService.getProjectCompany")
        );
    }

    @Bean(TRANSIENT_CACHE_MANAGER)
    public CacheManager transientCacheManager() {
        return new CompositeCacheManager(
                writeTimeBasedTransientCaffeineCacheBuilder(1000, 1, "ProjectMemberQueryService.maxRoleInCompanyProjectMembers"),
                writeTimeBasedTransientCaffeineCacheBuilder(1000, 60, "EndpointHelper.companyUrl"),
                writeTimeBasedTransientCaffeineCacheBuilder(1000, 60, "CompanyService.enableNickname"),
                writeTimeBasedTransientCaffeineCacheBuilder(100, 60, "SystemService.bpmIsOpen")
        );
    }

    private CaffeineCacheManager sizeBasedCaffeineCacheBuilder(long maximumSize, String... cacheNames) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(cacheNames);
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .initialCapacity(maximumSize > 100 ? 100 : (int) maximumSize)
                .maximumSize(maximumSize);
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }

    private CaffeineCacheManager writeTimeBasedCaffeineCacheBuilder(
            long maximumSize, long durationMinute, String... cacheNames) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(cacheNames);
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .initialCapacity(maximumSize > 100 ? 100 : (int) maximumSize)
                .maximumSize(maximumSize)
                .expireAfterWrite(durationMinute, TimeUnit.MINUTES);
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }

    private CaffeineCacheManager writeTimeBasedTransientCaffeineCacheBuilder(
            long maximumSize, long durationSeconds, String... cacheNames) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(cacheNames);
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .initialCapacity(maximumSize > 100 ? 100 : (int) maximumSize)
                .maximumSize(maximumSize)
                .expireAfterWrite(durationSeconds, TimeUnit.SECONDS);
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }

    public void clearCache(String cacheManagerName, String cacheName, String cacheKey) {
        CacheManager cacheManager = SpringBeanFactory.getBean(CacheManager.class, cacheManagerName);
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(cacheKey);
        }
    }
}
