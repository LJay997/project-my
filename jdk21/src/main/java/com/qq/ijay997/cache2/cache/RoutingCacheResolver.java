package com.qq.ijay997.cache2.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;

/**
 * 通道路由解析器
 * <p>
 * 按缓存名前缀将 {@code @Cacheable/@CachePut/@CacheEvict} 声明的缓存名路由到对应的
 * 标准 {@link CacheManager}（Caffeine 本地通道 / Redis 分布式通道），替代原先
 * "单类自定义 CacheManager 包办路由" 的方案——各通道底层均为 Spring 官方
 * CacheManager 组合，后续替换或新增通道底层时无需重写解析逻辑。
 * <p>
 * 路由规则（前缀匹配，先注册先匹配）：
 * <ul>
 *     <li>{@code caffeine:xxx} → CaffeineCacheManager（本地缓存）；</li>
 *     <li>{@code redis:xxx} 或无前缀 → RedisCacheManager（分布式缓存，默认通道）。</li>
 * </ul>
 * <p>
 * 缓存名中的 TTL 段（{@code 名称#秒数}）在本解析器<b>原样保留</b>传递给目标
 * CacheManager——TTL 语法解析是各通道 CacheManager 的职责
 * （详见 {@link TtlRedisCacheManager} / {@link TtlCaffeineCacheManager}），
 * 本类只负责通道分流，单一职责。
 * <p>
 * 新增通道零改代码：通过 {@link #registerChannel(String, CacheManager)} 注册新前缀即可
 * （路由表为 {@link LinkedHashMap}，注册仅限初始化阶段）。
 */
public class RoutingCacheResolver implements CacheResolver {

    /** Caffeine 本地通道前缀 */
    public static final String CAFFEINE_CHANNEL = "caffeine:";

    /** Redis 分布式通道前缀 */
    public static final String REDIS_CHANNEL = "redis:";

    /** 路由表：key 为缓存名前缀，value 为目标 CacheManager（LinkedHashMap 保持注册顺序） */
    private final Map<String, CacheManager> routeTable = new LinkedHashMap<>();

    /** 未匹配任何前缀时的默认 CacheManager（Redis 分布式通道） */
    private final CacheManager defaultCacheManager;

    public RoutingCacheResolver(CacheManager caffeineCacheManager, CacheManager redisCacheManager) {
        routeTable.put(CAFFEINE_CHANNEL, caffeineCacheManager);
        routeTable.put(REDIS_CHANNEL, redisCacheManager);
        this.defaultCacheManager = redisCacheManager;
    }

    /**
     * 注册新通道（仅限初始化阶段调用，非线程安全）
     *
     * @param namePrefix    缓存名前缀（如 {@code hazelcast:}）
     * @param cacheManager  该通道的 CacheManager
     */
    public void registerChannel(String namePrefix, CacheManager cacheManager) {
        routeTable.put(namePrefix, cacheManager);
    }

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        Collection<String> cacheNames = context.getOperation().getCacheNames();
        List<Cache> result = new ArrayList<>(cacheNames.size());
        for (String cacheName : cacheNames) {
            result.add(resolveCache(cacheName, context));
        }
        return result;
    }

    private Cache resolveCache(String cacheName, CacheOperationInvocationContext<?> context) {
        CacheManager target = defaultCacheManager;
        String working = cacheName;
        for (Map.Entry<String, CacheManager> entry : routeTable.entrySet()) {
            if (working.startsWith(entry.getKey())) {
                target = entry.getValue();
                working = working.substring(entry.getKey().length());
                break;
            }
        }
        // 保留 #ttl 段整名传递，TTL 解析由目标 CacheManager 完成
        Cache cache = target.getCache(working);
        if (cache == null) {
            throw new IllegalStateException(
                    "Cannot resolve cache '" + cacheName + "' for operation " + context.getOperation());
        }
        return cache;
    }

    /** 供调试/监控使用：当前路由表快照 */
    Map<String, CacheManager> getRouteTable() {
        return Collections.unmodifiableMap(routeTable);
    }
}
