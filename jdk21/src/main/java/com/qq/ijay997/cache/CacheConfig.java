package com.qq.ijay997.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Spring Cache 总装配（Configurer）
 * <p>
 * 仅新增、零改动现有缓存设计（CacheResult 手动缓存、hzero RedisHelper 等原样保留）。
 * 通过 {@link CachingConfigurer} 将自定义 {@link TenantRoutingCacheManager} 注册为全局
 * CacheManager 与 CacheResolver，此后业务代码直接使用标准注解即可：
 * <pre>
 * // SpEL 照常计算业务 key（租户前缀由 TenantAwareCache 装饰层自动追加，SpEL 与 keyGenerator 互斥不受影响）
 * &#64;Cacheable(cacheNames = "lov-cache#600", key = "#code", unless = "#result == null")
 * public Map&lt;String, LovValueDTO&gt; getLovValueMap(Long tenantId, String code) { ... }
 *
 * // 数据变更时精确失效
 * &#64;CacheEvict(cacheNames = "lov-cache", key = "#code")
 * </pre>
 * <p>
 * 三项全局策略：
 * <ul>
 *     <li>cacheManager：租户路由 + 动态 TTL（详见 {@link TenantRoutingCacheManager}）；</li>
 *     <li>cacheResolver：{@link SimpleCacheResolver} 包装全局 CacheManager，
 *     个别方法可用 {@code @Cacheable(cacheResolver = "...")} 定点覆盖，注解无需逐个指定 cacheManager；</li>
 *     <li>errorHandler：{@link LoggingCacheErrorHandler} 缓存读写失败仅记录 WARN 日志，
 *     不阻断业务主流程（与 CacheResult 的"缓存故障不阻塞业务"降级理念一致）。</li>
 * </ul>
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

    @Override
    public CacheManager cacheManager() {
        return new TenantRoutingCacheManager(connectionFactory, cacheProperties);
    }

    @Override
    public CacheResolver cacheResolver() {
        return new SimpleCacheResolver(cacheManager());
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }
}
