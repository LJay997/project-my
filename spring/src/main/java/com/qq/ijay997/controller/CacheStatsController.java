package com.qq.ijay997.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/actuator/caches")
public class CacheStatsController {

    private final CacheManager cacheManager;

    @GetMapping
    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String cacheName : cacheManager.getCacheNames()) {
            org.springframework.cache.Cache springCache = cacheManager.getCache(cacheName);
            if (!(springCache instanceof CaffeineCache)) {
                continue;
            }
            CaffeineCache caffeineCache = (CaffeineCache) springCache;
            Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            CacheStats s = nativeCache.stats();
            long size = nativeCache.estimatedSize();
            String hitRate = String.format("%.2f%%", s.hitRate() * 100);

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("size", size);
            detail.put("hitCount", s.hitCount());
            detail.put("missCount", s.missCount());
            detail.put("hitRate", hitRate);
            detail.put("evictionCount", s.evictionCount());
            detail.put("loadSuccessCount", s.loadSuccessCount());
            detail.put("loadFailureCount", s.loadFailureCount());
            detail.put("totalLoadTimeMs", s.totalLoadTime() / 1_000_000);
            result.put(cacheName, detail);

            log.info("[CacheStats] {} | size={} | hit={} | miss={} | hitRate={} | eviction={} | loadTime={}ms",
                    cacheName, size, s.hitCount(), s.missCount(), hitRate,
                    s.evictionCount(), s.totalLoadTime() / 1_000_000);
        }
        return result;
    }
}