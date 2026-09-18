# 计划：缓存架构方案对比（自定义 CacheManager vs CacheResolver 路由）与重构实施

## Summary

回答用户三问：①为什么用 TenantRoutingCacheManager ②用 CacheResolver 能否解析目标 CacheManager（下次接 CaffeineCacheManager 是否要重写逻辑）③租户 ID 等 ThreadLocal 上下文有哪些。结论：用户的质疑成立——当前单类方案违反开闭原则；采纳 **方案 B（官方 CacheManager 组合 + CacheResolver 路由 + 装饰器租户层）** 重构。批准后执行文件级重构清单。

## Current State Analysis（已验证事实）

### 现状（方案 A）：TenantRoutingCacheManager 单类承担四职责
通道路由（caffeine:/redis: 前缀）+ TTL 解析（name#600）+ Cache 构建（自己 new RedisCache/CaffeineCache）+ 租户包装（TenantAwareCache）。**用户痛点成立**：
1. 新增缓存实现必须改 TenantRoutingCacheManager 内部分支（违反开闭原则）——"下次要使用 CaffeineCacheManager 还要重写逻辑"属实
2. 丢弃官方特性：已验证 [CaffeineCacheManager（spring-context-support 5.3.33）](file:///Users/jay/Documents/ideaProject/cncc-project/maven-repository/org/springframework/spring-context-support/5.3.33/spring-context-support-5.3.33.jar) 拥有 `setCaffeine/setCacheSpecification/setCacheLoader/dynamic` 等能力，RedisCacheManager 拥有预注册 TTL/事务同步——单类方案全部绕过
3. 为动态 TTL 已出现 `DynamicTtlRedisCache` 暴露 protected 构造器的 hack
4. Actuator `/actuator/caches` 只见一个黑盒 CacheManager

### 关键技术边界（javap 已验证）
- **动态 TTL 是两个方案共同的下限**：官方 CaffeineCacheManager 仅全局单一 spec（无 per-cache TTL）；RedisCacheManager 仅预注册固定配置。`name#ttl` 运行期语法在任何方案都需小子类（无法 100% 官方化）——区别是方案 B 将自定义压缩到"仅 TTL 一个职责"
- **CacheResolver 完全可以承担路由**：`NamedCacheResolver(CacheManager, String... cacheNames)` 与 `CompositeCacheResolver` 在 spring-context 5.3.33 均可用；本项目需自定义 `RoutingCacheResolver`（前缀规则路由，比 Named 白名单更灵活）
- **租户上下文 ThreadLocal 清单**：
  1. `SecurityContextHolder` → `DetailsHelper.getUserDetails()` → `CustomUserDetails.getTenantId()/getLang()/getUserId()`（**当前实现采用**，编译期验证可用）
  2. `RedisDatabaseThreadLocal`（hzero-starter-redis 动态 db，可作路由维度参考）
  3. kjgl-data-permission-starter 权限上下文 ThreadLocal（可作过滤维度参考）
  4. 异步/定时任务无上下文 → `tenant-missing-strategy`（DENY/DEFAULT_TENANT）兜底（已实现）

## 方案优劣对比（写入最终回复）

| 维度 | 方案 A：单类（现状） | 方案 B：官方组合 + Resolver 路由（采纳） |
|---|---|---|
| 新增缓存实现 | 改单类内部 buildXXX 分支 | 新增标准 CacheManager Bean + 路由表加一行 |
| 官方特性复用 | 无（自建 Cache） | 全保留（spec/cacheLoader/预注册 TTL/事务同步） |
| 动态 TTL | 需 hack | 同样需小子类，但仅此一处自定义 |
| 职责分离 | 4 职责纠缠 | 路由/TTL/租户三类各自独立可测 |
| 调用链 | 1 跳 | 2 跳（Resolver 委托） |
| 可观测性 | 单黑盒 | 多标准 Bean，Actuator 可见 |

## Proposed Changes（方案 B 重构清单，批准后执行）

现有 4 类 → 调整为 6 类（职责单一化），Demo/Properties/TenantAwareCache 零改动：

1. **`RoutingCacheResolver.java`（新）**：implements CacheResolver；解析 cacheName 前缀（`caffeine:`→CaffeineCacheManager、默认/`redis:`→RedisCacheManager）+ 剥离 `#ttl` 段 → 委托目标 CacheManager.getCache(基础名)；路由规则表为可配置 Map（新增通道零改代码）
2. **`TenantCacheResolver.java`（新）**：装饰 delegate CacheResolver，将返回的 Cache 包装为 TenantAwareCache（租户逻辑与缓存实现彻底解耦）
3. **`TtlRedisCacheManager.java`（新）**：extends RedisCacheManager；覆写 getCache 支持 `name#ttl` 动态构建（迁入现 DynamicTtlRedisCache 逻辑）；yml `cache.ttl` Map 经 `withInitialCacheConfigurations` 预注册（官方 API）
4. **`TtlCaffeineCacheManager.java`（新）**：extends CaffeineCacheManager；对称支持 `name#ttl`（默认走官方 spec）
5. **`CacheConfig.java`（改）**：注册两个标准 CacheManager Bean（`caffeineCacheManager`/`redisCacheManager`）+ `cacheResolver()` 返回 `TenantCacheResolver(new RoutingCacheResolver(...))`；移除单类 cacheManager()
6. **`TenantRoutingCacheManager.java`（删除）**：职责被 1-4 拆分
7. **不动**：`TenantAwareCache`、`CacheProperties`、`CacheUsageDemo`、pom
8. **验证：按用户要求暂时跳过**——不执行 mvn compile，以 IDE 诊断为静态参考；键结构与 TTL 语义保持与现状一致（`kjgl-rp:{cacheName}:t{tenantId}:{bizKey}`）

## Assumptions & Decisions

- 采纳方案 B（用户质疑方向正确）；键结构、TTL 语法、租户行为对外不变（Demo 示例全部继续有效）
- Caffeine per-cache TTL 仍需小子类实现（官方类边界决定），在最终回复中如实说明
- 用户明确要求：暂不执行编译验证步骤

## Verification

- 编译通过；grep 确认 TenantRoutingCacheManager 已删、无残留引用
- CacheUsageDemo 7 个示例的注解无需任何修改（对外契约不变）
