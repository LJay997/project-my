package com.qq.ijay997.cache2.cache;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;

/**
 * 动态 TTL 的 Caffeine 缓存管理器（Spring 官方 {@link CaffeineCacheManager} 子类）
 * <p>
 * 官方 CaffeineCacheManager 的单一 {@code setCaffeine} 规格作用于所有缓存，
 * 无法表达 {@code cacheName#秒数} 的运行期动态 TTL；本子类在 {@link #getCache(String)}
 * 解析 TTL 语法并按需构建，条目淘汰、统计等语义沿用官方 {@code CaffeineCache} 适配
 * （经官方 protected 方法 {@code adaptCaffeineCache} 构建，保持 allowNullValues 行为一致）。
 * <p>
 * TTL 三级解析（与 {@link TtlRedisCacheManager} 规则一致）：
 * 内嵌 {@code 名称#秒数} &gt; yml 分桶 {@code cache.ttl.<名称>} &gt; {@code cache.default-ttl-seconds}。
 * <p>
 * <b>baseName 归一化（本类关键设计）</b>：官方父类 cacheMap 以<b>完整名</b>（含 #ttl 段）
 * 为 key，{@code unit-map#120} 与 {@code unit-map} 会构建成两个互不相通的 Caffeine 实例——
 * 本地缓存没有 Redis 那样按物理键空间共享的特性，跨实例 {@code @CacheEvict} 会落空。
 * 因此本类剥离 #ttl 后按<b>基础名</b>归一化：同一基础名永远复用同一 Caffeine 实例，
 * TTL 取<b>首次到达</b>请求所解析的值（内嵌 #ttl 优先于 yml/默认值）。
 * 若同一基础名需要不同 TTL，请使用 yml 分桶统一声明，而非在注解里写不同 #ttl 变体。
 * <p>
 * 使用示例：
 * <pre>
 * &#64;Cacheable(cacheNames = "caffeine:unit-map#120", key = "#tenantId")  // 本地通道，120s
 * &#64;CacheEvict(cacheNames = "caffeine:unit-map", key = "#tenantId")     // 命中同一实例，生效
 * </pre>
 */
public class TtlCaffeineCacheManager extends CaffeineCacheManager {

    /** 缓存名 TTL 内嵌分隔符：名称#秒数（与 TtlRedisCacheManager 一致） */
    static final String TTL_SEPARATOR = "#";

    private final CacheProperties properties;

    /** 实例池：key 为剥离通道前缀与 #ttl 后的基础名，保证同基础名共享同一 Caffeine 实例 */
    private final ConcurrentMap<String, Cache> cachesByBaseName = new ConcurrentHashMap<>();

    public TtlCaffeineCacheManager(CacheProperties properties) {
        super();
        this.properties = properties;
    }

    @Override
    public Cache getCache(String name) {
        int hashIndex = name.indexOf(TTL_SEPARATOR);
        String baseName = hashIndex >= 0 ? name.substring(0, hashIndex) : name;
        Cache cache = cachesByBaseName.get(baseName);
        if (cache != null) {
            return cache;
        }
        // 先解析 TTL 再进入 computeIfAbsent（lambda 需捕获 effectively final 变量）；
        // 并发下同基础名的 TTL 以首个完成解析的请求为准（first-arrival，见类注释）
        long ttlSeconds = resolveTtlSeconds(baseName, name, hashIndex);
        return cachesByBaseName.computeIfAbsent(baseName, key -> createCache(key, ttlSeconds));
    }

    @Override
    public Collection<String> getCacheNames() {
        Set<String> names = cachesByBaseName.keySet();
        return Collections.unmodifiableSet(names);
    }

    /**
     * TTL 三级解析：name 中内嵌 #ttl（仅当 hashIndex 有效）&gt; yml 分桶（小写基础名）&gt; 全局默认
     */
    private long resolveTtlSeconds(String baseName, String name, int hashIndex) {
        long ttlSeconds = -1L;
        if (hashIndex >= 0) {
            ttlSeconds = Long.parseLong(name.substring(hashIndex + TTL_SEPARATOR.length()).trim());
        }
        if (ttlSeconds <= 0) {
            ttlSeconds = properties.getTtl().getOrDefault(baseName.toLowerCase(Locale.ROOT),
                    properties.getDefaultTtlSeconds());
        }
        return ttlSeconds;
    }

    /**
     * 构建指定 TTL 的 Caffeine 缓存实例，经官方 protected 适配方法包装，
     * 与官方 getCache 路径产出的 CaffeineCache 行为完全一致
     */
    private Cache createCache(String baseName, long ttlSeconds) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(properties.getCaffeineMaxSize());
        return adaptCaffeineCache(baseName, builder.build());
    }
}
