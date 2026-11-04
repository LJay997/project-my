package com.qq.ijay997.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/actuator/caches")
public class CacheStatsController {

    private final CaffeineCacheManager cacheManager;

    public CacheStatsController(CaffeineCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @GetMapping
    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String cacheName : cacheManager.getCacheNames()) {
            CaffeineCache springCache = (CaffeineCache) cacheManager.getCache(cacheName);
            if (springCache == null) {
                continue;
            }
            Cache<Object, Object> nativeCache = springCache.getNativeCache();
            CacheStats s = nativeCache.stats();
            long size = nativeCache.estimatedSize();

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("size", size);
            detail.put("hitCount", s.hitCount());
            detail.put("missCount", s.missCount());
            detail.put("hitRate", String.format("%.2f%%", s.hitRate() * 100));
            detail.put("evictionCount", s.evictionCount());
            detail.put("loadSuccessCount", s.loadSuccessCount());
            detail.put("loadFailureCount", s.loadFailureCount());
            detail.put("totalLoadTimeMs", s.totalLoadTime() / 1_000_000);
            result.put(cacheName, detail);
        }
        return result;
    }
}