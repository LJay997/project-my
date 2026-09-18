package com.qq.ijay997.cache;

import java.time.Duration;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * 租户路由 + 动态 TTL 缓存管理器
 * <p>
 * 在 CacheManager 层完成三件事，使 {@code @Cacheable} 注解保持零定制：
 * <ol>
 *     <li><b>通道路由</b>：按缓存名前缀路由到 Caffeine（本地）或 Redis（分布式）——
 *     {@code caffeine:xxx} 走本地缓存，{@code redis:xxx} 或无前缀走 Redis；</li>
 *     <li><b>动态 TTL 解析</b>：支持缓存名内嵌过期时间语法 {@code 名称#秒数}
 *     （如 {@code dict#600} 表示 600 秒），未内嵌时按 yml 分桶 {@code cache.ttl.<名称>} 查找，
 *     仍未命中则使用 {@code cache.default-ttl-seconds}；</li>
 *     <li><b>租户隔离</b>：底层 Cache 统一包装 {@link TenantAwareCache}，自动追加租户前缀。</li>
 * </ol>
 * 使用示例：
 * <pre>
 * &#64;Cacheable(cacheNames = "lov-cache#600", key = "#code")               // Redis 通道，600s
 * &#64;Cacheable(cacheNames = "caffeine:unit-map#120", key = "#tenantId")  // Caffeine 通道，120s
 * &#64;CacheEvict(cacheNames = "lov-cache", key = "#code")                 // 失效时同规则路由
 * </pre>
 * <p>
 * Redis 最终键结构（三层职责分离，SpEL 仅计算 bizKey 段）：
 * {@code kjgl-rp : t{tenantId} : {cacheName} : {bizKey(SpEL)}}
 * （全局前缀可配置 cache.global-prefix，默认 kjgl-rp）
 */
public class TenantRoutingCacheManager implements CacheManager {

    /** Caffeine 本地通道前缀 */
    public static final String CAFFEINE_CHANNEL = "caffeine:";

    /** Redis 分布式通道前缀 */
    public static final String REDIS_CHANNEL = "redis:";

    /** 缓存名 TTL 内嵌分隔符：名称#秒数 */
    public static final String TTL_SEPARATOR = "#";

    private final RedisConnectionFactory connectionFactory;

    private final CacheProperties properties;

    /** 已构建缓存实例池：key 为注解中声明的完整缓存名（含通道与 TTL 段） */
    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();

    public TenantRoutingCacheManager(RedisConnectionFactory connectionFactory, CacheProperties properties) {
        this.connectionFactory = connectionFactory;
        this.properties = properties;
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, this::buildCache);
    }

    @Override
    public java.util.Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(caches.keySet());
    }

    /**
     * 解析缓存名语法并构建底层 Cache：
     * 通道（caffeine:/redis:/默认 redis）+ 基础名 + 可选 #TTL（秒），
     * TTL 三级解析：内嵌 # &gt; yml cache.ttl 分桶 &gt; cache.default-ttl-seconds
     */
    private Cache buildCache(String name) {
        String working = name;
        String channel = REDIS_CHANNEL;

        if (working.startsWith(CAFFEINE_CHANNEL)) {
            channel = CAFFEINE_CHANNEL;
            working = working.substring(CAFFEINE_CHANNEL.length());
        } else if (working.startsWith(REDIS_CHANNEL)) {
            working = working.substring(REDIS_CHANNEL.length());
        }

        String baseName = working;
        long ttlSeconds = -1L;
        int hashIndex = working.indexOf(TTL_SEPARATOR);
        if (hashIndex >= 0) {
            baseName = working.substring(0, hashIndex);
            ttlSeconds = Long.parseLong(working.substring(hashIndex + TTL_SEPARATOR.length()).trim());
        }
        if (ttlSeconds <= 0) {
            ttlSeconds = properties.getTtl().getOrDefault(baseName.toLowerCase(Locale.ROOT),
                    properties.getDefaultTtlSeconds());
        }

        Cache delegate;
        boolean tenantEnabled = properties.isTenantCacheEnabled();
        if (CAFFEINE_CHANNEL.equals(channel)) {
            delegate = buildCaffeineCache(baseName, ttlSeconds);
        } else {
            delegate = buildRedisCache(baseName, ttlSeconds);
        }

        return tenantEnabled ? new TenantAwareCache(delegate, properties) : delegate;
    }

    /**
     * Redis 通道：按解析出的 TTL 动态构建 RedisCache（Jackson 序列化，允许 null 值配合短 TTL 防穿透）
     * <p>
     * 键前缀必须包含缓存名段（{@code kjgl-rp:{cacheName}:}）——原因：RedisCache 的
     * {@code clear()}（对应 @CacheEvict(allEntries=true)）按前缀 SCAN 删除，前缀若不含
     * 缓存名会跨缓存名误删其他缓存的数据。缓存名后紧跟租户段，最终键结构：
     * {@code kjgl-rp:{cacheName}:t{tenantId}:{bizKey(SpEL)}}
     */
    private Cache buildRedisCache(String baseName, long ttlSeconds) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(ttlSeconds))
                .prefixCacheNameWith(properties.getGlobalPrefix() + ":" + baseName + ":")
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        return new DynamicTtlRedisCache(baseName,
                RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory), config);
    }

    /**
     * 以 public 构造器暴露 {@link RedisCache} 的 protected 构造器，
     * 用于按解析出的 TTL 动态构建缓存实例（Spring 官方 RedisCacheManager 仅支持预注册固定 TTL，
     * 运行期 name#ttl 语法需动态构建）
     */
    private static class DynamicTtlRedisCache extends RedisCache {

        DynamicTtlRedisCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfig) {
            super(name, cacheWriter, cacheConfig);
        }
    }

    /**
     * Caffeine 通道：写后过期 + 容量上限，防止本地缓存无限膨胀
     */
    private Cache buildCaffeineCache(String baseName, long ttlSeconds) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(properties.getCaffeineMaxSize());
        return new CaffeineCache(baseName, builder.build());
    }
}
