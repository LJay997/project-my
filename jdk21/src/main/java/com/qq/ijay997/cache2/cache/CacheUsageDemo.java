package com.qq.ijay997.cache2.cache;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Spring Cache 声明式缓存使用示例（Demo）
 * <p>
 * 配合 {@link CacheConfig}（方案 B：官方 CacheManager 组合 + CacheResolver 路由）使用，
 * 业务代码只写标准注解，通道路由（{@link RoutingCacheResolver}）、TTL 解析（官方管理器子类）、
 * 租户隔离（{@link TenantCacheResolver}）均由解析器链自动完成。
 * <p>
 * <b>核心约定</b>：
 * <ul>
 *     <li>SpEL 仅计算业务键（bizKey），与 keyGenerator 互斥，不要使用 keyGenerator；</li>
 *     <li>cacheName 支持 {@code 名称#秒数} 内嵌 TTL 与 {@code caffeine:}/{@code redis:} 通道前缀；</li>
 *     <li>实际 Redis 键结构：{@code kjgl-rp:{cacheName}:t{tenantId}:{bizKey(SpEL)}}，
 *     租户 ID 自动取自 {@code DetailsHelper} 上下文，业务代码无需拼接；</li>
 *     <li>建议始终写 {@code unless = "#result == null"} 防止缓存穿透（null 值也会占用存储）。</li>
 * </ul>
 * <p>
 * 本类使用内存 Map 模拟数据源，仅作注解用法示范，生产代码请替换为真实的 Mapper/Service 调用。
 */
@Component
@CacheConfig(cacheNames = "demo-user-cache#300")    // 类级默认缓存名：Redis 通道，TTL 300 秒
public class CacheUsageDemo {

    /** 模拟数据源（生产代码应替换为 Mapper/Service） */
    private final Map<String, String> mockStorage = new LinkedHashMap<>();

    /**
     * 示例 1：查询缓存（最常用形态）
     * <p>
     * cacheName 继承类级 {@code demo-user-cache#300}；SpEL 计算 bizKey；
     * 首次执行方法并写缓存，命中则不执行方法体。
     * 实际 Redis 键：{@code kjgl-rp:demo-user-cache:t{tenantId}:U001}
     */
    @Cacheable(key = "#code", unless = "#result == null")
    public String getUser(String code) {
        // 未命中缓存才会执行：模拟 DB 查询
        return mockStorage.get(code);
    }

    /**
     * 示例 2：Caffeine 本地通道 + 更短 TTL（适合高频读、容忍短暂不一致的热点数据）
     * <p>
     * 实际键（JVM 内）：{@code t{tenantId}:H001}（本地缓存按 Cache 实例隔离，无全局前缀）
     */
    @Cacheable(cacheNames = "caffeine:demo-hot-cache#60", key = "#code")
    public String getHotUser(String code) {
        return mockStorage.get(code);
    }

    /**
     * 示例 3：多参数 SpEL 键 + 条件缓存
     * <p>
     * 键由多个参数拼接；{@code condition} 不满足时完全跳过缓存（读写都不走 Cache）。
     * 实际 Redis 键：{@code kjgl-rp:demo-lov-cache:t{tenantId}:zh_CN:UNIT}
     */
    @Cacheable(cacheNames = "demo-lov-cache#600", key = "#lang + ':' + #lovCode", condition = "#lovCode != null")
    public Map<String, String> getLovValues(String lang, String lovCode) {
        // 模拟值集查询
        return new LinkedHashMap<>();
    }

    /**
     * 示例 4：更新缓存（@CachePut）
     * <p>
     * 方法始终执行，并将返回值同步写入缓存——适用于"写后读强一致"场景。
     * 注意：@CachePut 的 key 必须与对应 @Cacheable 的 key 规则一致，否则会分裂出两个键。
     */
    @CachePut(key = "#code")
    public String updateUser(String code, String name) {
        mockStorage.put(code, name);
        return name;
    }

    /**
     * 示例 5：精确失效（@CacheEvict 单键）
     * <p>
     * 删除当前租户下该 code 对应的缓存条目，其他租户与其他条目不受影响。
     */
    @CacheEvict(key = "#code")
    public void deleteUser(String code) {
        mockStorage.remove(code);
    }

    /**
     * 示例 6：条件失效（beforeInvocation 控制时机）
     * <p>
     * {@code beforeInvocation = true}：方法执行前失效（方法抛异常也已完成失效，适合"先失效再重建"）；
     * 默认 false：方法成功返回后失效（方法抛异常则保留旧缓存）。
     */
    @CacheEvict(key = "#code", beforeInvocation = true)
    public String resetUser(String code) {
        mockStorage.put(code, "RESET");
        return "RESET";
    }

    /**
     * 示例 7：全量失效（allEntries = true）——慎用
     * <p>
     * 清空 {@code demo-user-cache} 缓存名下<b>所有租户</b>的全部条目（Redis 侧按
     * {@code kjgl-rp:demo-user-cache:*} 前缀 SCAN 删除，已隔离于其他缓存名，
     * 但无法只清单个租户）；且多实例部署时仅本机 Caffeine 通道被清，Redis 通道全局生效。
     */
    @CacheEvict(allEntries = true)
    public void reloadAllUsers() {
        mockStorage.clear();
    }
}
