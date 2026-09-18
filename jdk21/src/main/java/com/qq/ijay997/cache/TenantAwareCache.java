package com.qq.ijay997.cache;

import java.util.concurrent.Callable;

import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.core.oauth.DetailsHelper;
import org.springframework.cache.Cache;
import org.springframework.cache.support.NullValue;
import org.springframework.lang.Nullable;

/**
 * 租户感知缓存装饰器
 * <p>
 * 包装底层 Cache（Redis/Caffeine），对"读/写/删"操作的 key 统一追加租户前缀，实现多租户数据隔离。
 * <p>
 * 设计要点：
 * <ul>
 *     <li>租户隔离在 Cache 装饰层完成，而非 KeyGenerator——因此 {@code @Cacheable(key = "...SpEL...")}
 *     的 SpEL 计算完全保留（SpEL 与 keyGenerator 在 Spring CacheOperation 构建期互斥，不可同时使用）；
 *     SpEL 仅计算业务键段，租户与缓存名维度由本装饰层统一拼接；</li>
 *     <li>注解层看到的缓存名与业务 key 保持原样，对业务代码零感知；</li>
 *     <li>装饰层产出键段 {@code t{tenantId}:}，与底层键前缀（Redis 通道的
 *     {@code kjgl-rp:{cacheName}:}）拼接后，最终 Redis 键结构为：
 *     {@code kjgl-rp:{cacheName}:t{tenantId}:{bizKey(SpEL)}}——缓存名置于租户段之前，
 *     保证 @CacheEvict(allEntries=true) 的前缀 SCAN 清理仅作用于本缓存名，
 *     不跨缓存名误删；</li>
 *     <li>无法获取租户上下文（定时任务/异步线程）时按 {@link CacheProperties.TenantMissingStrategy} 处理，
 *     默认 DENY 抛异常，防止静默写入错误租户的数据。</li>
 * </ul>
 */
public class TenantAwareCache implements Cache {

    private final Cache delegate;

    private final CacheProperties properties;

    public TenantAwareCache(Cache delegate, CacheProperties properties) {
        this.delegate = delegate;
        this.properties = properties;
    }

    /**
     * 解析当前租户 ID，产出装饰层键段：t{tenantId}:{业务key}
     * （cacheName 段由底层 RedisCache 前缀 kjgl-rp:{cacheName}: 承担，此处不重复拼接）
     */
    private Object composeKey(Object key) {
        return "t" + resolveTenantId() + ":" + key;
    }

    /**
     * 从 Choerodon 安全上下文获取当前租户 ID（与项目惯用 API 一致：DetailsHelper.getUserDetails()）
     */
    private Long resolveTenantId() {
        CustomUserDetails userDetails = DetailsHelper.getUserDetails();
        Long tenantId = userDetails != null ? userDetails.getTenantId() : null;
        if (tenantId != null) {
            return tenantId;
        }
        switch (properties.getTenantMissingStrategy()) {
            case DEFAULT_TENANT:
                return properties.getDefaultTenantId();
            case DENY:
            default:
                throw new IllegalStateException(
                        "No tenant context found for cache access. Set cache.tenant-missing-strategy=DEFAULT_TENANT "
                                + "if this cache is accessed from scheduled/async threads.");
        }
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    @Nullable
    public ValueWrapper get(Object key) {
        return delegate.get(composeKey(key));
    }

    @Override
    @Nullable
    public <T> T get(Object key, @Nullable Class<T> type) {
        return delegate.get(composeKey(key), type);
    }

    @Override
    @Nullable
    public <T> T get(Object key, Callable<T> valueLoader) {
        return delegate.get(composeKey(key), valueLoader);
    }

    @Override
    public void put(Object key, @Nullable Object value) {
        delegate.put(composeKey(key), value);
    }

    @Override
    @Nullable
    public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
        return delegate.putIfAbsent(composeKey(key), value);
    }

    @Override
    public void evict(Object key) {
        delegate.evict(composeKey(key));
    }

    @Override
    public boolean evictIfPresent(Object key) {
        // Spring 5.2+ 语义：返回是否确有删除；由 delegate 的同语义方法透传
        return delegate.evictIfPresent(composeKey(key));
    }

    @Override
    public void clear() {
        // 注意：clear 清空该缓存名下所有租户的数据（底层 Cache 无按前缀枚举能力）。
        // 需要按租户精确失效的场景请使用 evict(key)。
        delegate.clear();
    }

    @Override
    public boolean invalidate() {
        return delegate.invalidate();
    }

    /**
     * 供调试/监控使用：透传底层存储值类型（NullValue 为 Spring 空值防穿透占位）
     */
    static boolean isNullValue(Object value) {
        return value == NullValue.INSTANCE;
    }
}
