package com.qq.ijay997.config;

import com.github.benmanes.caffeine.cache.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@EnableCaching
@EnableScheduling
public class CaffeineCacheConfig {

    @Value("${cache.caffeine.spec:maximumSize=1000,expireAfterWrite=10m,recordStats}")
    private String caffeineSpec;

    @Value("${cache.caffeine.cache-names:userCache,userListCache}")
    private String cacheNames;

    private CacheManager cacheManager;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.from(caffeineSpec)
                .removalListener((key, value, cause) ->
                        log.info("[CacheEvict] key={} cause={}", key, cause)));

        List<String> names = Arrays.stream(cacheNames.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toList());
        if (!names.isEmpty()) {
            manager.setCacheNames(names);
        }
        this.cacheManager = manager;
        return manager;
    }

    @Scheduled(fixedRate = 60000)
    public void logPeriodicStats() {
        for (String name : cacheManager.getCacheNames()) {
            org.springframework.cache.Cache springCache = cacheManager.getCache(name);
            if (!(springCache instanceof CaffeineCache)) {
                continue;
            }
            CaffeineCache cache = (CaffeineCache) springCache;
            com.github.benmanes.caffeine.cache.stats.CacheStats s = cache.getNativeCache().stats();
            log.info("[CachePeriodic] {} | size={} | hit={} | miss={} | hitRate={} | eviction={}",
                    name,
                    cache.getNativeCache().estimatedSize(),
                    s.hitCount(), s.missCount(),
                    String.format("%.2f%%", s.hitRate() * 100),
                    s.evictionCount());
        }
    }
}