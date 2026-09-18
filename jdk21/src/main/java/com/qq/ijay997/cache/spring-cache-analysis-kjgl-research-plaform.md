# 计划：kjgl-research-plaform 缓存分析报告（按用户四条反馈修订）

## 交付物

1 份分析报告文档：`插件总结/kjgl-research-plaform缓存架构分析与设计.md`（沿用系列惯例，仅此一个文件，不改任何代码）

## 报告内容（四章节，全部落实用户反馈）

**第一章 现状评估**（探索事实）
- Spring Cache 零使用（pom 无 starter-cache、代码零注解）；`CacheResult` 自研工具剖析（SHA-256 哈希键/Page 兼容/异常降级/固定 10min TTL/闲置状态）；data-redis + hzero-starter-redis 双体系并存；LovCodeService 显式传 tenantId 的租户隔离现状

**第二章 缓存架构设计**（核心修正点）
- **SpEL 问题解答**（专节）：`@Cacheable` 的 key(SpEL) 与 keyGenerator 互斥（CacheOperation 构建期校验直接抛异常）→ 因此租户隔离**不放 KeyGenerator**，改为 **TenantPrefixCacheManager 装饰层**统一拼 `t{tenantId}:` 前缀，SpEL key 计算完全保留
- **CacheResolver 路由**：CaffeineCacheManager 与 RedisCacheManager 双底层，通过 `CompositeCacheResolver`/`NamedCacheResolver` 按 cacheName 前缀路由；`CachingConfigurer.cacheResolver()` 设为全局默认，注解零指定
- **TTL 解析**：cacheName 内嵌 `dict#600` 语法（RedisCacheConfiguration.entryTtl / Caffeine expireAfterWrite 动态构建）+ application.yml Map 分桶兜底 + 默认 TTL
- 集成形态：Memcached 评估结论（不推荐）与适配路径；两级缓存组合为可选进阶

**第三章 多租户缓存 key 策略**
- 必须引入租户标识的论据（显式传参隔离 + 数据权限差异化数据集）
- key 规范表：`t{tenantId}:{cacheName}[:{lang}]:{bizKey}`、禁止段（userId/可变对象）、Cluster hash tag 取舍
- 空上下文策略（异步/定时任务 DENY 或 DEFAULT 可配置）

**第四章 实施步骤与注意事项**（零改动约束版）
- **仅新增**：spring-boot-starter-cache + caffeine 依赖、`CacheConfig implements CachingConfigurer` 及配套 4-5 个配置类——**现有代码（含 CacheResult）零修改**
- 注意事项：ExcludeAutoconfigureRunListener 影响、db 1 冲突、RedisTemplate 注入歧义、失效广播、与 hzero @ProcessCacheValue 切面划界

## 验证

- Read 复查四章节齐全；SpEL 互斥问题有明确解答；全篇无"修改现有代码"表述
- 现状断言与探索事实一致
