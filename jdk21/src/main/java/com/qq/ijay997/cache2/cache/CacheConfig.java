package com.qq.ijay997.cache2.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Spring Cache 总装配（Configurer）——方案 B：官方 CacheManager 组合 + CacheResolver 路由
 * <p>
 * 仅新增、零改动现有缓存设计（CacheResult 手动缓存、hzero RedisHelper 等原样保留）。
 * 缓存链路按职责分层，每层均为 Spring 官方扩展点或其小子类，替换任一层无需重写其余部分：
 * <pre>
 * @Cacheable 注解（SpEL 业务键，零定制）
 *    │
 * TenantCacheResolver          —— 租户装饰：Cache 统一包装 TenantAwareCache（追加 t{tenantId}: 段）
 *    │
 * RoutingCacheResolver         —— 通道分流：caffeine: 前缀 → 本地通道，其余 → Redis 通道
 *    │            │
 * TtlCaffeineCacheManager   TtlRedisCacheManager
 * （官方子类，解析 name#ttl）  （官方子类，解析 name#ttl，键前缀 kjgl-rp:{cacheName}:）
 * </pre>
 * 此后业务代码直接使用标准注解即可：
 * <pre>
 * // Redis 通道 600s：SpEL 照常计算业务 key（租户前缀由 TenantCacheResolver 层自动追加）
 * &#64;Cacheable(cacheNames = "lov-cache#600", key = "#code", unless = "#result == null")
 * public Map&lt;String, LovValueDTO&gt; getLovValueMap(Long tenantId, String code) { ... }
 *
 * // Caffeine 本地通道 120s
 * &#64;Cacheable(cacheNames = "caffeine:unit-map#120", key = "#tenantId")
 *
 * // 数据变更时精确失效（同规则路由；Redis 通道跨 TTL 变体共享键空间，evict 天然一致）
 * &#64;CacheEvict(cacheNames = "lov-cache", key = "#code")
 * </pre>
 * <p>
 * 与方案 A（单类 {@code TenantRoutingCacheManager} 包办路由+TTL+租户）相比：
 * 通道底层由自定义实现换成官方 CaffeineCacheManager / RedisCacheManager 子类，
 * 新增通道只需 {@link RoutingCacheResolver#registerChannel(String, CacheManager)}，
 * 拆换底层（如引入多级缓存、Redisson）不再重写路由逻辑。
 * <p>
 * 不覆写 {@link CachingConfigurer#cacheManager()}：已提供全局 {@link #cacheResolver()}，
 * 缓存拦截器按 CacheResolver 解析缓存、不再回退查询 CacheManager Bean，
 * 两个通道 Bean 无歧义冲突；{@code @Primary} 仅用于容器内其他组件按类型注入的兜底。
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig implements CachingConfigurer {

    private final RedisConnectionFactory connectionFactory;

    private final CacheProperties cacheProperties;

    public CacheConfig(RedisConnectionFactory connectionFactory, CacheProperties cacheProperties) {
        this.connectionFactory = connectionFactory;
        this.cacheProperties = cacheProperties;
    }

    /** Caffeine 本地通道（cacheName 以 caffeine: 前缀路由到本管理器） */
    @Bean
    public CacheManager caffeineCacheManager() {
        return new TtlCaffeineCacheManager(cacheProperties);
    }

    /** Redis 分布式通道（默认通道，无前缀缓存名走本管理器） */
    @Bean
    @Primary
    public CacheManager redisCacheManager() {
        return new TtlRedisCacheManager(connectionFactory, cacheProperties);
    }

    @Override
    public CacheResolver cacheResolver() {
        // 直接调用 @Bean 方法，经 CGLIB 代理返回容器单例
        CacheResolver routing = new RoutingCacheResolver(caffeineCacheManager(), redisCacheManager());
        return new TenantCacheResolver(routing, cacheProperties);
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }
}
