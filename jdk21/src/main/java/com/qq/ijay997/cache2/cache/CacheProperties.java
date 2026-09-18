package com.qq.ijay997.cache2.cache;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 缓存配置属性
 * <p>
 * 前缀 cache.*，与既有缓存设计（CacheResult 手动缓存、hzero RedisHelper）互不干扰，仅服务新增的 Spring Cache 声明式缓存。
 * <p>
 * 配置示例（application.yml）：
 * <pre>
 * cache:
 *   global-prefix: kjgl-rp              # 全局统一键前缀（Redis 通道生效）
 *   default-ttl-seconds: 300            # 全局默认过期时间（秒）
 *   ttl:                                # 按缓存名分桶覆盖（优先级高于 default-ttl-seconds）
 *     lov-cache: 600
 *     dashboard-cache: 120
 *   tenant-cache-enabled: true          # 是否在 Cache 层自动追加租户前缀（灰度开关）
 *   tenant-missing-strategy: DENY       # 无租户上下文时策略：DENY 抛异常 / DEFAULT_TENANT 使用默认租户
 *   default-tenant-id: 0                # DEFAULT_TENANT 策略下的默认租户 ID
 *   caffeine-max-size: 10000            # Caffeine 通道单缓存最大条目数
 * </pre>
 * <p>
 * Redis 最终键结构（三层职责分离，SpEL 仅计算 bizKey 段）：
 * <pre>
 * kjgl-rp : {cacheName} : t{tenantId} : {bizKey(SpEL)}
 *   │            │             │              │
 * 全局前缀   Cache 实例     租户装饰层       注解 SpEL
 * (Redis)  (随TTL语法解析) (TenantAware)    (业务键)
 * </pre>
 * <p>
 * <b>与 Spring Boot 自动配置属性的关系</b>：Boot 的 {@code org.springframework.boot.autoconfigure.cache.CacheProperties}
 * （前缀 {@code spring.cache.*}）驱动 CacheAutoConfiguration 构建单一固定 CacheManager，
 * 不支持通道路由/动态 TTL/租户装饰；本项目自定义 {@code cacheManager()} Bean 后 Boot 的
 * CacheManager 自动退避，{@code spring.cache.*} 大部分配置实际不生效。因此：
 * <pre>
 * 本属性(cache.*)          spring.cache.*(Boot)             对照说明
 * globalPrefix             -（无对应）                        全局键前缀为项目扩展
 * defaultTtlSeconds        redis.time-to-live                语义近似，但 Boot 版仅作用于被退避的 CacheManager，勿混用
 * ttl(Map 按名分桶)         -（Boot 无对应）                    项目扩展，配合 cacheName#ttl 语法
 * caffeineMaxSize          caffeine.spec(maxSize=...)        Boot 用 spec 字符串，本项目用独立字段
 * tenantCacheEnabled 等    -（无对应）                        多租户隔离为项目扩展
 * </pre>
 * 不复用 {@code spring.cache.*} 前缀的原因：该前缀同时驱动 Boot 自动配置，混用会造成
 * "部分配置看似生效实则被退避 Bean 忽略"的误导；采用独立前缀 + 命名语义对齐的方式保持一致性。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

    /** 无租户上下文时的处理策略 */
    public enum TenantMissingStrategy {
        /** 抛出异常，防止定时任务/异步线程静默写入错误租户的缓存 */
        DENY,
        /** 使用默认租户 ID 兜底 */
        DEFAULT_TENANT
    }

    /** 全局统一键前缀（Redis 通道键结构第一段） */
    private String globalPrefix = "kjgl-rp";

    /** 全局默认过期时间（秒） */
    private long defaultTtlSeconds = 300L;

    /** 按缓存名分桶的过期时间（秒），key 为去掉通道前缀后的缓存名 */
    private Map<String, Long> ttl = new HashMap<>();

    /** 是否启用租户前缀隔离 */
    private boolean tenantCacheEnabled = true;

    /** 无租户上下文时的策略 */
    private TenantMissingStrategy tenantMissingStrategy = TenantMissingStrategy.DENY;

    /** DEFAULT_TENANT 策略下的默认租户 ID */
    private Long defaultTenantId = 0L;

    /** Caffeine 通道单缓存最大条目数 */
    private long caffeineMaxSize = 10_000L;
}
