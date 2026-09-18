package com.qq.ijay997.cache2.cache;

import java.time.Duration;
import java.util.Locale;

import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.lang.Nullable;

/**
 * 动态 TTL 的 Redis 缓存管理器（Spring 官方 {@link RedisCacheManager} 子类）
 * <p>
 * 官方 RedisCacheManager 仅支持"预注册固定 TTL"或"全库单一默认 TTL"，无法表达
 * {@code cacheName#秒数} 的运行期动态 TTL 语法；本子类仅在缓存实例创建钩子
 * {@link #createRedisCache(String, RedisCacheConfiguration)} 内解析 TTL，
 * 键计算、序列化、事务语义、{@code @CacheEvict(allEntries=true)} 的前缀 SCAN 清理等
 * 全部沿用官方实现。
 * <p>
 * TTL 三级解析（与 Caffeine 通道规则一致）：
 * <ol>
 *     <li>缓存名内嵌 {@code 名称#秒数}（如 {@code lov-cache#600}）；</li>
 *     <li>yml 分桶 {@code cache.ttl.<名称>}（key 为小写基础名）；</li>
 *     <li>全局默认 {@code cache.default-ttl-seconds}。</li>
 * </ol>
 * <p>
 * <b>键结构（字节码验证结论）</b>：{@link RedisCacheConfiguration#prefixCacheNameWith(String)}
 * 底层为 {@code CacheKeyPrefix.prefixed(p)} = {@code p + cacheName + ":"}，会自动追加
 * 缓存名与分隔符——因此这里只需传入全局前缀，最终 Redis 键前缀为：
 * <pre>
 * kjgl-rp : {baseName} : {t{tenantId}} : {bizKey(SpEL)}
 *   │          │           │              │
 * 全局前缀   缓存实例名   TenantAwareCache 装饰层   注解 SpEL
 * (prefixCacheNameWith)   (租户段)                 (业务键)
 * </pre>
 * 前缀中包含缓存名段是 {@code @CacheEvict(allEntries=true)} 安全性的前提：
 * {@code RedisCache.clear()} 按前缀 SCAN 删除，若前缀不含缓存名会跨缓存误删。
 * <p>
 * 注意：父类 cacheMap 以<b>完整名</b>（含 #ttl 段）为 key——{@code lov-cache#600} 与
 * {@code lov-cache} 是两个 Cache 实例，但键前缀相同、共享同一物理键空间，
 * 因此跨 TTL 变体的 {@code @CacheEvict} 天然一致（Caffeine 通道无此特性，
 * 见 {@link TtlCaffeineCacheManager} 的 baseName 归一化说明）。
 */
public class TtlRedisCacheManager extends RedisCacheManager {

    /** 缓存名 TTL 内嵌分隔符：名称#秒数 */
    static final String TTL_SEPARATOR = "#";

    private final RedisCacheWriter cacheWriter;

    private final CacheProperties properties;

    public TtlRedisCacheManager(RedisConnectionFactory connectionFactory, CacheProperties properties) {
        this(RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory), properties);
    }

    private TtlRedisCacheManager(RedisCacheWriter cacheWriter, CacheProperties properties) {
        // 动态创建开关随两参构造器开启：未预注册的缓存名运行期按需构建
        super(cacheWriter, RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(properties.getDefaultTtlSeconds()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer())));
        // 父类 cacheWriter 为私有字段且无 getter，自持引用供动态构建使用
        this.cacheWriter = cacheWriter;
        this.properties = properties;
    }

    /**
     * 缓存实例创建钩子：解析完整名（含可选 #ttl）→ 计算键前缀与 TTL → 构建官方 RedisCache 子类。
     * 入参 name 为 {@link #getCache(String)} 的完整调用名（如 {@code lov-cache#600}）。
     */
    @Override
    protected RedisCache createRedisCache(String name, @Nullable RedisCacheConfiguration cacheConfig) {
        String baseName = name;
        long ttlSeconds = -1L;
        int hashIndex = name.indexOf(TTL_SEPARATOR);
        if (hashIndex >= 0) {
            baseName = name.substring(0, hashIndex);
            ttlSeconds = Long.parseLong(name.substring(hashIndex + TTL_SEPARATOR.length()).trim());
        }
        if (ttlSeconds <= 0) {
            ttlSeconds = properties.getTtl().getOrDefault(baseName.toLowerCase(Locale.ROOT),
                    properties.getDefaultTtlSeconds());
        }

        RedisCacheConfiguration config = (cacheConfig != null ? cacheConfig : RedisCacheConfiguration.defaultCacheConfig())
                .entryTtl(Duration.ofSeconds(ttlSeconds))
                // CacheKeyPrefix.prefixed 会自动追加 cacheName + ":"，此处只传全局前缀；
                // 传入的 cacheName 由下方 TtlRedisCache 构造名（baseName）决定
                .prefixCacheNameWith(properties.getGlobalPrefix() + ":")
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return new TtlRedisCache(baseName, cacheWriter, config);
    }

    /**
     * 以 public 可达构造器暴露 {@link RedisCache} 的 protected 构造器，
     * 使本管理器能动态构建缓存实例（官方 RedisCache 构造器不对外公开）
     */
    private static class TtlRedisCache extends RedisCache {

        TtlRedisCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfig) {
            super(name, cacheWriter, cacheConfig);
        }
    }
}
