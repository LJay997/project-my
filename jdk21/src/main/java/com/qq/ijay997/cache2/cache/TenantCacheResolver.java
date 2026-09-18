package com.qq.ijay997.cache2.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;

/**
 * 租户装饰解析器
 * <p>
 * 装饰器模式的 {@link CacheResolver}：委托底层解析器完成通道路由与缓存实例获取，
 * 再将返回的每个 {@link Cache} 统一包装为 {@link TenantAwareCache}——租户前缀隔离
 * 由此集中完成，通道 CacheManager（官方标准类组合）保持纯净，不感知租户逻辑。
 * <p>
 * 通过 {@code CachingConfigurer.cacheResolver()} 注册为全局解析器后，
 * 业务注解保持零定制：SpEL key 只算业务键段，租户段由本层自动追加。
 * 个别方法可用 {@code @Cacheable(cacheResolver = "...")} 定点绕过租户装饰。
 * <p>
 * 包装结果按解析器调用链缓存（Spring CacheAspectSupport 对每个注解操作仅解析一次），
 * 运行期开销可忽略；开关关闭（{@code cache.tenant-cache-enabled=false}）时原样透传。
 */
public class TenantCacheResolver implements CacheResolver {

    private final CacheResolver delegate;

    private final CacheProperties properties;

    public TenantCacheResolver(CacheResolver delegate, CacheProperties properties) {
        this.delegate = delegate;
        this.properties = properties;
    }

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        Collection<? extends Cache> caches = delegate.resolveCaches(context);
        if (!properties.isTenantCacheEnabled()) {
            return caches;
        }
        List<Cache> wrapped = new ArrayList<>(caches.size());
        for (Cache cache : caches) {
            wrapped.add(cache instanceof TenantAwareCache ? cache : new TenantAwareCache(cache, properties));
        }
        return wrapped;
    }
}
